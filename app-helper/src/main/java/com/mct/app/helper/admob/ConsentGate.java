package com.mct.app.helper.admob;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * Holds ad requests back until UMP allows them.
 * <p>
 * The gate is only active once {@link AdsManager#init}/{@link AdsManager#initAsync} has started
 * gathering consent. Before that (and when the app turned it off with
 * {@link AdsManager#setConsentRequiredBeforeLoad(boolean)}) every load is allowed, which keeps the
 * old behaviour for code paths that never call init (tests, custom consent platforms).
 * <p>
 * States:
 * <ul>
 * <li><b>Undecided</b>: UMP does not allow requests and has not produced a usable answer yet
 * (still gathering, or the consent-info update failed, e.g. offline first launch). Loads are
 * parked. After a failed update the gate asks for a retry when connectivity returns or when a new
 * load is parked (throttled), so an offline start does not block ads for the whole process.</li>
 * <li><b>Decided</b>: UMP answered without error (or allows requests). Parked loads replay if
 * {@code canRequestAds()} is true, otherwise they fail with
 * {@link com.mct.app.helper.admob.listener.LoadError#CODE_CONSENT_NOT_OBTAINED}.</li>
 * </ul>
 * Note: UMP's {@code canRequestAds()} is also true when the user declined personalised ads
 * (non-personalised ads are still allowed), so a decided "no" is rare in practice.
 * <p>
 * Main-thread only; {@link AdsManager} posts its public calls onto the main thread.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class ConsentGate {

    static final long RETRY_THROTTLE_MS = 30_000L;
    static final long RETRY_STUCK_MS = 120_000L;

    /** Source of truth for "may we request ads", normally UMP's ConsentInformation. */
    public interface ConsentSource {
        boolean canRequestAds();
    }

    /** Re-runs the UMP consent-info update; returns false if it could not start (no activity). */
    public interface RetryHandler {
        boolean retryConsent();
    }

    /** A load held by the gate. */
    public interface ParkedLoad {
        /** Consent now allows requests: send the load. */
        void replay();

        /** Consent was decided without permission, or the gate was reset: fail the load. */
        void deny();
    }

    /** Notified when {@code canRequestAds()} changes as seen by the gate. */
    public interface ChangeListener {
        void onCanRequestAdsChanged(boolean canRequestAds);
    }

    private static final ConsentGate INSTANCE = new ConsentGate(SystemClock::uptimeMillis);

    public static ConsentGate get() {
        return INSTANCE;
    }

    private final LongSupplier clock;
    private volatile boolean enabled = true;
    private volatile boolean gatheringStarted = false;
    private volatile boolean flowFinished = false;
    private boolean decided = false;
    private boolean retryInProgress = false;
    private long lastRetryAt = Long.MIN_VALUE / 2;
    @Nullable
    private ConsentSource source;
    @Nullable
    private RetryHandler retryHandler;
    @Nullable
    private ChangeListener changeListener;
    @Nullable
    private Boolean lastCanRequest;
    private final List<ParkedLoad> parked = new ArrayList<>();
    private final List<Runnable> finishedWaiters = new ArrayList<>();

    @VisibleForTesting
    ConsentGate(@NonNull LongSupplier clock) {
        this.clock = clock;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            // nothing may stay parked behind a gate that no longer exists
            flush(true);
        }
    }

    boolean isEnabled() {
        return enabled;
    }

    void setChangeListener(@Nullable ChangeListener listener) {
        this.changeListener = listener;
    }

    /** Consent gathering has started; {@code source} answers canRequestAds from now on. */
    void begin(@NonNull ConsentSource source, @Nullable RetryHandler retryHandler) {
        this.source = source;
        this.retryHandler = retryHandler;
        this.gatheringStarted = true;
        this.flowFinished = false;
        this.decided = false;
        this.retryInProgress = false;
        this.lastCanRequest = source.canRequestAds();
    }

    /**
     * The UMP flow finished (form dismissed, not required, or failed).
     *
     * @param error true if the consent-info update or the form failed
     */
    void complete(boolean error) {
        if (!gatheringStarted) {
            return;
        }
        flowFinished = true;
        retryInProgress = false;
        boolean can = canRequestAds();
        if (can || !error) {
            decided = true;
            flush(can);
        }
        // else: consent info is unavailable (offline first launch). Stay undecided: keep loads
        // parked and retry on connectivity / next load instead of failing every load all session.
        notifyChange(can);
        List<Runnable> waiters = new ArrayList<>(finishedWaiters);
        finishedWaiters.clear();
        for (Runnable r : waiters) {
            r.run();
        }
    }

    /** Re-evaluates after something may have changed consent (privacy-options form). */
    void onConsentMaybeChanged() {
        if (!gatheringStarted) {
            return;
        }
        boolean can = canRequestAds();
        if (can && !decided) {
            decided = true;
            flush(true);
        }
        notifyChange(can);
    }

    /** Connectivity came back: retry a failed consent-info update right away. */
    void onConnectivityAvailable() {
        maybeRetry(true);
    }

    /** True once the UMP flow has finished at least once (with or without error). */
    boolean isFlowFinished() {
        return flowFinished;
    }

    boolean isDecided() {
        return decided;
    }

    boolean isGatheringStarted() {
        return gatheringStarted;
    }

    /** Runs {@code r} once the UMP flow has finished (immediately if it already has). */
    void runWhenFlowFinished(@NonNull Runnable r) {
        if (flowFinished) {
            r.run();
        } else {
            finishedWaiters.add(r);
        }
    }

    private boolean canRequestAds() {
        ConsentSource s = source;
        return s != null && s.canRequestAds();
    }

    /** True if an ad request may be sent right now. */
    public boolean allowsLoad() {
        if (!enabled || !gatheringStarted) {
            return true;
        }
        return canRequestAds();
    }

    /** True if a refused load should be parked (consent not decided yet) rather than failed. */
    public boolean shouldPark() {
        return enabled && gatheringStarted && !decided && !canRequestAds();
    }

    /** Parks a load until consent is decided. May trigger a (throttled) consent retry. */
    public void park(@NonNull ParkedLoad load) {
        parked.add(load);
        maybeRetry(false);
    }

    private void maybeRetry(boolean force) {
        if (!enabled || !gatheringStarted || !flowFinished || decided || retryHandler == null) {
            return;
        }
        long now = clock.getAsLong();
        // a retry whose UMP callback never came (activity destroyed under the form) expires
        if (retryInProgress && now - lastRetryAt < RETRY_STUCK_MS) {
            return;
        }
        if (!force && now - lastRetryAt < RETRY_THROTTLE_MS) {
            return;
        }
        lastRetryAt = now;
        retryInProgress = retryHandler.retryConsent();
    }

    @VisibleForTesting
    int parkedCount() {
        return parked.size();
    }

    private void notifyChange(boolean can) {
        if (lastCanRequest != null && lastCanRequest == can) {
            return;
        }
        lastCanRequest = can;
        ChangeListener l = changeListener;
        if (l != null) {
            l.onCanRequestAdsChanged(can);
        }
    }

    private void flush(boolean allowed) {
        List<ParkedLoad> copy = new ArrayList<>(parked);
        parked.clear();
        for (ParkedLoad p : copy) {
            if (allowed) {
                p.replay();
            } else {
                p.deny();
            }
        }
    }

    /**
     * Used by {@link AdsManager#clear()}: fails parked loads (their callbacks get
     * CODE_CONSENT_NOT_OBTAINED) and forgets the consent flow. The app's on/off choice
     * ({@link #setEnabled}) and change listener are kept.
     */
    void reset() {
        flush(false);
        finishedWaiters.clear();
        source = null;
        retryHandler = null;
        gatheringStarted = false;
        flowFinished = false;
        decided = false;
        retryInProgress = false;
        lastRetryAt = Long.MIN_VALUE / 2;
        lastCanRequest = null;
    }
}
