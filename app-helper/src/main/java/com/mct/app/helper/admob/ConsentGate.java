package com.mct.app.helper.admob;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds ad requests back until the UMP consent flow has finished.
 * <p>
 * The gate is only active once {@link AdsManager#init}/{@link AdsManager#initAsync} has started
 * gathering consent. Before that (and when the app turned it off with
 * {@link AdsManager#setConsentRequiredBeforeLoad(boolean)}) every load is allowed, which keeps the
 * old behaviour for code paths that never call init (tests, custom consent platforms).
 * <p>
 * While gathering is in progress and UMP does not yet allow requests, loads are parked and replayed
 * once consent resolves. After it resolves, a load is allowed only if UMP's
 * {@code canRequestAds()} is true; otherwise it fails straight away.
 * <p>
 * Main-thread only, like the rest of the ad stack.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class ConsentGate {

    /** Source of truth for "may we request ads", normally UMP's ConsentInformation. */
    public interface ConsentSource {
        boolean canRequestAds();
    }

    /** A parked load: {@code onAllowed} replays it, {@code onDenied} fails its callbacks. */
    private static final class Parked {
        final Runnable onAllowed;
        final Runnable onDenied;

        Parked(Runnable onAllowed, Runnable onDenied) {
            this.onAllowed = onAllowed;
            this.onDenied = onDenied;
        }
    }

    private static final ConsentGate INSTANCE = new ConsentGate();

    public static ConsentGate get() {
        return INSTANCE;
    }

    private boolean enabled = true;
    private boolean gatheringStarted = false;
    private boolean resolved = false;
    private ConsentSource source;
    private final List<Parked> parked = new ArrayList<>();
    private final List<Runnable> resolvedWaiters = new ArrayList<>();

    @VisibleForTesting
    ConsentGate() {
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

    /** Consent gathering has started; {@code source} answers canRequestAds from now on. */
    void begin(@NonNull ConsentSource source) {
        this.source = source;
        this.gatheringStarted = true;
        this.resolved = false;
    }

    /** UMP finished (form dismissed, not required, or failed). Replays or fails parked loads. */
    void complete() {
        if (!gatheringStarted) {
            return;
        }
        resolved = true;
        flush(canRequestAds());
        List<Runnable> waiters = new ArrayList<>(resolvedWaiters);
        resolvedWaiters.clear();
        for (Runnable r : waiters) {
            r.run();
        }
    }

    boolean isResolved() {
        return resolved;
    }

    boolean isGatheringStarted() {
        return gatheringStarted;
    }

    /** Runs {@code r} once consent has resolved (immediately if it already has). */
    void runWhenResolved(@NonNull Runnable r) {
        if (resolved) {
            r.run();
        } else {
            resolvedWaiters.add(r);
        }
    }

    private boolean canRequestAds() {
        return source != null && source.canRequestAds();
    }

    /** True if an ad request may be sent right now. */
    public boolean allowsLoad() {
        if (!enabled || !gatheringStarted) {
            return true;
        }
        return canRequestAds();
    }

    /** True if a refused load should be parked (consent still being gathered) rather than failed. */
    public boolean shouldPark() {
        return enabled && gatheringStarted && !resolved && !canRequestAds();
    }

    /** Parks a load until consent resolves. */
    public void park(@NonNull Runnable onAllowed, @NonNull Runnable onDenied) {
        parked.add(new Parked(onAllowed, onDenied));
    }

    @VisibleForTesting
    int parkedCount() {
        return parked.size();
    }

    private void flush(boolean allowed) {
        List<Parked> copy = new ArrayList<>(parked);
        parked.clear();
        for (Parked p : copy) {
            (allowed ? p.onAllowed : p.onDenied).run();
        }
    }

    /** Drops all state (used by {@link AdsManager#clear()}). Parked loads are dropped silently. */
    void reset() {
        parked.clear();
        resolvedWaiters.clear();
        source = null;
        gatheringStarted = false;
        resolved = false;
        enabled = true;
    }
}
