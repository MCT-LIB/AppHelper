package com.mct.app.helper.admob.ads;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.LoadAdError;
import com.mct.app.helper.admob.AdState;
import com.mct.app.helper.admob.AdsManager;
import com.mct.app.helper.admob.AdsShowPolicy;
import com.mct.app.helper.admob.AdsValue;
import com.mct.app.helper.admob.listener.AdCallback;
import com.mct.app.helper.admob.listener.LoadCallback;
import com.mct.app.helper.admob.listener.LoadError;
import com.mct.app.helper.admob.listener.ShowError;
import com.mct.app.helper.admob.utils.AdUnitTestIds;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base class for every ad type. Implemented as an explicit {@link AdState} state
 * machine so trouble states cannot get stuck:
 *
 * <pre>
 *   IDLE ──load──▶ LOADING ──success──▶ READY ──show──▶ SHOWING
 * </pre>
 *
 * <ul>
 *     <li>All eligibility (load / show / interval / expiry) is delegated to the
 *     pure, unit-tested {@link AdsShowPolicy} — there is no {@code Handler}.</li>
 *     <li>A {@code loadGeneration} token invalidates stale GMS load callbacks
 *     (replaces the v1 {@code AtomicBoolean} dispose dance).</li>
 *     <li>{@link #transitionToShowing} is the single producer of
 *     {@link AdState#SHOWING}; it always rolls back on a synchronous throw, so a
 *     failed show can never leave the ad stuck "showing".</li>
 *     <li>{@link #onRelease(Object)} is the single, guaranteed resource-release
 *     hook (fixes the v1 banner {@code AdView} leak).</li>
 * </ul>
 *
 * Threading: instances are confined to the main thread (every GMS load/show
 * callback, observer and RecyclerView binding already runs there).
 * {@link #assertMainThread()} enforces this in debug builds.
 *
 * @noinspection unused
 */
public abstract class BaseAds<Ads> {

    protected static final String TAG = "BaseAds";

    /** Ads loaded longer ago than this are considered stale and reloadable. */
    private static final long DEFAULT_OVERDUE_MS = 4 * 60 * 60 * 1000L;
    /** Window for {@link #isDismissNearly()}. */
    private static final long DISMISS_NEARLY_MS = 3000L;
    /**
     * Watchdog: an ad claiming {@link AdState#SHOWING} for longer than this is
     * considered stuck (GMS dropped its FullScreenContentCallback) and may be
     * force-released by the manager so it stops blocking every other
     * full-screen ad with {@code SkipReason.OVERLAP}.
     */
    private static final long DEFAULT_MAX_SHOW_DURATION_MS = 10 * 60 * 1000L;

    private final String adsUnitId;
    private final long adsInterval;
    private String alias;
    private String customAlias;

    // --- state machine ---
    private AdState state = AdState.IDLE;
    private Ads ad;
    private long loadedAt;
    private long lastDismissedAt;
    private long showStartedAt;
    private int loadGeneration;
    private final List<LoadCallback> pendingLoadCallbacks = new ArrayList<>();

    public BaseAds(String adsUnitId, long adsInterval) {
        this.adsUnitId = adsUnitId;
        this.adsInterval = adsInterval;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Subclass hooks
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Issue the actual GMS load. On completion call {@link LoadSession#loaded}
     * or {@link LoadSession#failed}. The session guards against stale callbacks.
     */
    protected abstract void onLoad(@NonNull Context context, @NonNull LoadSession session);

    /**
     * Release native resources held by the ad. Called exactly once whenever the
     * ad leaves the cache (load fail / clear / dismiss / fail-to-show / expire).
     * The argument may be {@code null} (e.g. cleared mid-load); subclasses that
     * hold extra resources (pools) must still clean up.
     */
    protected abstract void onRelease(@Nullable Ads ad);

    /** Overridable max age before a loaded ad is considered stale. */
    protected long getOverdueMs() {
        return DEFAULT_OVERDUE_MS;
    }

    /**
     * Overridable watchdog threshold: how long this ad may stay
     * {@link AdState#SHOWING} before {@link #isShowingStale()} reports it stuck.
     * Long enough that no legitimate impression (rewarded video + endcard, or an
     * ad paused in background for a while) trips it; {@code <= 0} disables.
     */
    protected long getMaxShowDurationMs() {
        return DEFAULT_MAX_SHOW_DURATION_MS;
    }

    /** Whether this placement enforces a per-ad interval between impressions. */
    protected boolean allowAdsInterval() {
        return false;
    }

    /**
     * Whether a fresh load should still run while the ad is already
     * {@link AdState#READY}, in order to TOP UP a partially-filled cache (e.g. a
     * native-ad pool that holds fewer ads than its configured size). When this
     * returns true, {@link #load(Context, LoadCallback)} issues another
     * {@link #onLoad} <b>without releasing</b> the current cache, so the new ads
     * are appended to it.
     * <p>
     * Default {@code false}: a single ad is simply ready or not.
     */
    protected boolean shouldTopUpWhenReady() {
        return false;
    }

    protected void onAdClicked() {
    }

    ///////////////////////////////////////////////////////////////////////////
    // Load
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Load an ad if eligible. If already ready, the callback's
     * {@link LoadCallback#onAdLoaded} fires immediately; if a load is already in
     * flight, the callback is queued onto it.
     */
    public final void load(@NonNull Context context, @Nullable LoadCallback callback) {
        assertMainThread();
        // A cache that is READY but only partially filled (e.g. a native-ad pool
        // holding fewer ads than its configured size) wants another load to TOP it
        // up. Without this, load() would short-circuit on isReady() forever and the
        // pool would stay capped at its first batch until expiry.
        boolean topUp = isReady() && !isPremium() && shouldTopUpWhenReady();
        if (isReady() && !topUp) {
            if (callback != null) {
                callback.onAdLoaded(this);
            }
            return;
        }
        if (state == AdState.LOADING) {
            if (callback != null) {
                pendingLoadCallbacks.add(callback);
            }
            return;
        }
        if (!topUp && !AdsShowPolicy.canLoad(state, loadedAt, now(), isPremium(), getOverdueMs())) {
            // not loadable (premium, or showing) -> report failure
            if (callback != null) {
                callback.onAdFailedToLoad(this, new LoadError(
                        AdRequest.ERROR_CODE_INTERNAL_ERROR, "Ad not loadable in state " + state, "com.mct.app.helper"));
            }
            return;
        }
        if (!topUp && state == AdState.READY) {
            // expired -> release before reloading. A top-up deliberately KEEPS the
            // existing cache and appends to it, so it must not release here.
            releaseAd();
        }
        Log.d(TAG, "load: " + getClass().getSimpleName());
        state = AdState.LOADING;
        loadGeneration++;
        pendingLoadCallbacks.clear();
        if (callback != null) {
            pendingLoadCallbacks.add(callback);
        }
        onLoad(context, new LoadSession(loadGeneration));
    }

    /**
     * Token handed to {@link #onLoad}. Bound to a {@code loadGeneration}; once the
     * ad is cleared or reloaded the old session becomes stale and its completions
     * are ignored (loaded ads are released to avoid leaks).
     */
    protected final class LoadSession {

        private final int generation;

        private LoadSession(int generation) {
            this.generation = generation;
        }

        public boolean isStale() {
            return generation != loadGeneration || state != AdState.LOADING;
        }

        public void loaded(@NonNull Ads loadedAd) {
            assertMainThread();
            if (isStale()) {
                onRelease(loadedAd);
                return;
            }
            Log.d(TAG, "onAdLoaded: " + BaseAds.this.getClass().getSimpleName());
            ad = loadedAd;
            loadedAt = now();
            state = AdState.READY;
            for (LoadCallback c : drainLoadCallbacks()) {
                c.onAdLoaded(BaseAds.this);
            }
            AdsManager.getInstance().onAdsLoaded(BaseAds.this);
        }

        public void failed(@NonNull LoadAdError error) {
            assertMainThread();
            if (isStale()) {
                return;
            }
            Log.d(TAG, "onAdFailedToLoad: " + BaseAds.this.getClass().getSimpleName() + " " + error);
            ad = null;
            state = AdState.IDLE;
            LoadError e = LoadError.of(error);
            for (LoadCallback c : drainLoadCallbacks()) {
                c.onAdFailedToLoad(BaseAds.this, e);
            }
            AdsManager.getInstance().onAdsFailedToLoad(BaseAds.this, e);
        }
    }

    private List<LoadCallback> drainLoadCallbacks() {
        List<LoadCallback> copy = new ArrayList<>(pendingLoadCallbacks);
        pendingLoadCallbacks.clear();
        return copy;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Show transitions (used by full-screen subclasses)
    ///////////////////////////////////////////////////////////////////////////

    /**
     * The single producer of {@link AdState#SHOWING}. Runs {@code showAction}
     * (the subclass render) and, if it throws synchronously, rolls the state back
     * to {@link AdState#IDLE}, releases the ad and reports failure via {@code cb}.
     *
     * @return true if the show action started without throwing
     */
    protected final boolean transitionToShowing(@NonNull Runnable showAction, @Nullable AdCallback cb) {
        assertMainThread();
        if (state != AdState.READY) {
            return false;
        }
        state = AdState.SHOWING;
        showStartedAt = now();
        try {
            showAction.run();
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "show failed: " + getClass().getSimpleName(), t);
            releaseAd();
            state = AdState.IDLE;
            loadedAt = 0;
            showStartedAt = 0;
            if (cb != null) {
                cb.onFailedToShow(ShowError.of(t));
                cb.onComplete(false);
            }
            return false;
        }
    }

    /**
     * Full-screen ad was closed by the user: consumed, marks dismiss time.
     * <p>
     * Guarded by state AND identity: the event is stale when the ad is no longer
     * {@link AdState#SHOWING} (it was {@link #clear()}ed mid-show, or force-released
     * by the stuck-SHOWING watchdog), or when {@code shownAd} is not the ad this
     * instance currently holds (cleared mid-show, reloaded AND re-shown — the event
     * belongs to the previous impression). A stale event must NOT release the
     * cached/showing replacement. The dismiss time is still recorded because an
     * impression really did just end on screen, so the global back-to-back gap
     * stays armed.
     *
     * @param shownAd the GMS ad object the terminal event belongs to
     *                ({@code null} skips the identity check)
     */
    protected final void transitionDismissed(@Nullable Object shownAd) {
        assertMainThread();
        lastDismissedAt = now();
        if (isStaleTerminalEvent(shownAd, "dismiss")) {
            return;
        }
        releaseAd();
        state = AdState.IDLE;
        loadedAt = 0;
        showStartedAt = 0;
    }

    /**
     * Full-screen ad failed to render: consumed, NOT counted as a dismiss.
     * State- and identity-guarded like {@link #transitionDismissed(Object)}.
     */
    protected final void transitionFailedToShow(@Nullable Object shownAd) {
        assertMainThread();
        if (isStaleTerminalEvent(shownAd, "failed-to-show")) {
            return;
        }
        releaseAd();
        state = AdState.IDLE;
        loadedAt = 0;
        showStartedAt = 0;
    }

    private boolean isStaleTerminalEvent(@Nullable Object shownAd, String event) {
        if (state != AdState.SHOWING) {
            Log.w(TAG, "stale " + event + " event ignored (state=" + state + "): " + getClass().getSimpleName());
            return true;
        }
        if (shownAd != null && shownAd != ad) {
            Log.w(TAG, "stale " + event + " event ignored (different impression): " + getClass().getSimpleName());
            return true;
        }
        return false;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clear / release
    ///////////////////////////////////////////////////////////////////////////

    /** Fully reset to {@link AdState#IDLE}, releasing all resources. */
    public final void clear() {
        assertMainThread();
        loadGeneration++;            // invalidate any in-flight load
        // Drain BEFORE notifying so the callbacks observe the final IDLE state
        // (and a re-entrant load() from within a callback starts cleanly).
        List<LoadCallback> dropped = drainLoadCallbacks();
        releaseAd();
        state = AdState.IDLE;
        loadedAt = 0;
        lastDismissedAt = 0;
        showStartedAt = 0;
        if (!dropped.isEmpty()) {
            // A load was in flight (e.g. a show(LOAD_IF_NEEDED) waiting on it).
            // Report cancellation instead of silently dropping it, otherwise the
            // caller's AdCallback#onComplete would never fire.
            LoadError e = new LoadError(AdRequest.ERROR_CODE_INTERNAL_ERROR,
                    "Load cancelled by clear()", "com.mct.app.helper");
            for (LoadCallback c : dropped) {
                c.onAdFailedToLoad(this, e);
            }
        }
    }

    private void releaseAd() {
        Ads released = ad;
        ad = null;
        onRelease(released);
    }

    ///////////////////////////////////////////////////////////////////////////
    // State queries
    ///////////////////////////////////////////////////////////////////////////

    public AdState getState() {
        return state;
    }

    public boolean isLoading() {
        return state == AdState.LOADING;
    }

    public boolean isShowing() {
        return state == AdState.SHOWING;
    }

    public boolean isReady() {
        return state == AdState.READY && !isExpired();
    }

    public boolean isExpired() {
        return AdsShowPolicy.isExpired(loadedAt, now(), getOverdueMs());
    }

    public boolean isCanLoadAds() {
        return AdsShowPolicy.canLoad(state, loadedAt, now(), isPremium(), getOverdueMs());
    }

    public boolean isCanShowAds() {
        return AdsShowPolicy.canShow(state, loadedAt, lastDismissedAt, now(), isPremium(),
                getIntervalForPolicy(), getOverdueMs());
    }

    public boolean isAllowAdsInterval() {
        return allowAdsInterval();
    }

    public boolean isDismissNearly() {
        return AdsShowPolicy.isDismissNearly(lastDismissedAt, now(), DISMISS_NEARLY_MS);
    }

    /**
     * Watchdog predicate: true when this ad has claimed {@link AdState#SHOWING}
     * for longer than {@link #getMaxShowDurationMs()}. That only happens when GMS
     * dropped the terminal FullScreenContentCallback events (ad activity killed,
     * SDK bug) — the ad is stuck and would otherwise block every full-screen ad
     * with {@code SkipReason.OVERLAP} forever.
     */
    public boolean isShowingStale() {
        return state == AdState.SHOWING
                && AdsShowPolicy.isShowingStale(showStartedAt, now(), getMaxShowDurationMs());
    }

    private long getIntervalForPolicy() {
        return allowAdsInterval() ? adsInterval : 0;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Accessors
    ///////////////////////////////////////////////////////////////////////////

    public AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    public String getAdsUnitId() {
        return adsUnitId;
    }

    public long getAdsInterval() {
        return adsInterval;
    }

    /** Real ads unit id, swapped for a Google test id in debug mode. */
    public String getLoadAdsUnitId() {
        return AdsManager.getInstance().isDebug() ? AdUnitTestIds.getAdsUnitId(this) : getAdsUnitId();
    }

    public String getAlias() {
        return alias;
    }

    public String getCustomAlias() {
        return customAlias;
    }

    public Ads getAds() {
        return ad;
    }

    public long getAdsLoadedTime() {
        return loadedAt;
    }

    public long getLastDismissedAt() {
        return lastDismissedAt;
    }

    /** Time the current/last show transition started (0 if never shown). */
    public long getShowStartedAt() {
        return showStartedAt;
    }

    public void setAlias(String a) {
        alias = a;
    }

    public void setCustomAlias(String a) {
        customAlias = a;
    }

    protected final void onPaidEvent(AdValue adValue) {
        AdsValue v = AdsValue.of(TextUtils.isEmpty(customAlias) ? alias : customAlias, adValue);
        Optional.ofNullable(AdsManager.getInstance().getOnPaidEventListener()).ifPresent(l -> l.onPaidEvent(v));
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helpers
    ///////////////////////////////////////////////////////////////////////////

    private static boolean isPremium() {
        return AdsManager.getInstance().isPremium();
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    protected static boolean validateActivityToShow(Activity activity) {
        return activity != null && !activity.isDestroyed();
    }

    static void assertMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            if (AdsManager.getInstance().isDebug()) {
                throw new IllegalStateException("AdsManager must be used on the main thread");
            }
            Log.w(TAG, "Ad state mutated off the main thread");
        }
    }
}
