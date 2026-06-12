package com.mct.app.helper.admob.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.mct.app.helper.admob.AdsManager;
import com.mct.app.helper.admob.listener.AdCallback;
import com.mct.app.helper.admob.listener.LoadCallback;
import com.mct.app.helper.admob.listener.LoadError;
import com.mct.app.helper.admob.listener.ShowError;
import com.mct.app.helper.admob.listener.ShowMode;
import com.mct.app.helper.admob.listener.SkipReason;

/**
 * Base for full-screen ads (app-open, interstitial, native-full-screen, rewarded).
 * The reward event is delivered by passing a {@code RewardedAdCallback} through to
 * {@link #onShowAds}; there is no separate {@code BaseRewardedAds} class.
 */
public abstract class BaseFullScreenAds<Ads> extends BaseAds<Ads> {

    private boolean autoLoadAfterDismiss = true;
    private Context autoLoadContext;
    private OnAdsShowChangeListener onAdsShowChangeListener;

    public BaseFullScreenAds(String adsUnitId, long adsInterval) {
        super(adsUnitId, adsInterval);
    }

    /**
     * Render the ad. Must only render (never mutate ad state). Wire {@code contentCallback}
     * to the GMS ad and call its show method. {@code callback} is the caller's
     * {@link AdCallback} (possibly a {@code RewardedAdCallback}) — rewarded subclasses
     * use it to deliver the earned reward.
     */
    protected abstract void onShowAds(
            @NonNull Activity activity,
            @NonNull Ads ads,
            @NonNull FullScreenContentCallback contentCallback,
            @Nullable AdCallback callback
    );

    /**
     * Show this full-screen ad. The manager has already cleared the global gate
     * (premium / overlap / back-to-back gap). This handles per-placement state:
     * ready → show; not ready → load-then-show (when {@link ShowMode#LOAD_IF_NEEDED})
     * or skip with a precise reason.
     */
    public final void show(@NonNull String reportAlias,
                           @NonNull Activity activity,
                           @NonNull ShowMode mode,
                           @Nullable AdCallback callback) {
        assertMainThread();

        if (isShowing()) {
            skip(callback, SkipReason.OVERLAP);
            return;
        }
        if (isLoading()) {
            if (mode == ShowMode.LOAD_IF_NEEDED) {
                loadThenShow(reportAlias, activity, callback, SkipReason.NOT_LOADED);
            } else {
                skip(callback, SkipReason.LOADING);
            }
            return;
        }
        if (isCanShowAds() && validateActivityToShow(activity)) {
            setCustomAlias(reportAlias);
            autoLoadContext = activity.getApplicationContext();
            final AdCallback cb = callback;
            // capture the exact ad object this impression uses, so its terminal
            // events can never be mis-attributed to a later reload/re-show
            final Ads shownAd = getAds();
            transitionToShowing(
                    () -> onShowAds(activity, shownAd, new FullScreenContentCallbackImpl(this, shownAd, cb), cb),
                    cb);
            return;
        }
        // not in a showable state
        if (!validateActivityToShow(activity)) {
            skip(callback, SkipReason.ACTIVITY_INVALID);
            return;
        }
        if (mode == ShowMode.LOAD_IF_NEEDED && isCanLoadAds()) {
            loadThenShow(reportAlias, activity, callback, SkipReason.NOT_LOADED);
            return;
        }
        skip(callback, isReady() ? SkipReason.COOLDOWN : SkipReason.NOT_LOADED);
    }

    private void loadThenShow(@NonNull String reportAlias,
                              @NonNull Activity activity,
                              @Nullable AdCallback callback,
                              @NonNull SkipReason failReason) {
        load(activity.getApplicationContext(), new LoadCallback() {
            @Override
            public void onAdLoaded(BaseAds<?> ad) {
                showAfterLoad(reportAlias, activity, callback);
            }

            @Override
            public void onAdFailedToLoad(BaseAds<?> ad, LoadError error) {
                skip(callback, failReason);
            }
        });
    }

    /**
     * Show once the deferred load completes. Re-enters the manager so the GLOBAL
     * gate (overlap / back-to-back gap) is re-evaluated at the actual show time —
     * the request-time gate is stale by the time a load finishes seconds later.
     */
    private void showAfterLoad(@NonNull String reportAlias, @NonNull Activity activity, @Nullable AdCallback callback) {
        String alias = getAlias();
        if (alias != null) {
            AdsManager.getInstance().showFullScreen(alias, reportAlias, activity, ShowMode.IF_READY, callback);
        } else {
            show(reportAlias, activity, ShowMode.IF_READY, callback);
        }
    }

    private static void skip(@Nullable AdCallback cb, @NonNull SkipReason reason) {
        if (cb != null) {
            cb.onSkipped(reason);
            cb.onComplete(false);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Dismiss / show-change plumbing
    ///////////////////////////////////////////////////////////////////////////

    private void handleDismiss(@Nullable Object shownAd) {
        onAdDismissedFullScreen();      // notify listener (e.g. restore banner visibility)
        transitionDismissed(shownAd);   // base: release + IDLE + record dismiss time (identity-guarded)
        if (autoLoadAfterDismiss && autoLoadContext != null) {
            load(autoLoadContext, null);
        }
    }

    public void setOnAdsShowChangeListener(OnAdsShowChangeListener listener) {
        this.onAdsShowChangeListener = listener;
    }

    public boolean isAutoLoadAfterDismiss() {
        return autoLoadAfterDismiss;
    }

    public void setAutoLoadAfterDismiss(boolean autoLoadAfterDismiss) {
        this.autoLoadAfterDismiss = autoLoadAfterDismiss;
    }

    private void onAdShowedFullScreen() {
        if (onAdsShowChangeListener != null) {
            onAdsShowChangeListener.onShow(this);
        }
    }

    private void onAdDismissedFullScreen() {
        if (onAdsShowChangeListener != null) {
            onAdsShowChangeListener.onDismiss(this);
        }
    }

    public interface OnAdsShowChangeListener {
        void onShow(BaseFullScreenAds<?> fullScreenAds);

        void onDismiss(BaseFullScreenAds<?> fullScreenAds);
    }

    /**
     * Adapts GMS {@link FullScreenContentCallback} events onto the state machine
     * and the caller's {@link AdCallback}. Guards against duplicate terminal events.
     */
    protected static class FullScreenContentCallbackImpl extends FullScreenContentCallback {

        private boolean shown;
        private boolean done;
        private BaseFullScreenAds<?> ads;
        private Object shownAd;
        private AdCallback callback;

        public FullScreenContentCallbackImpl(BaseFullScreenAds<?> ads, AdCallback callback) {
            this(ads, null, callback);
        }

        /** @param shownAd the GMS ad object of THIS impression (identity guard for terminal events) */
        public FullScreenContentCallbackImpl(BaseFullScreenAds<?> ads, Object shownAd, AdCallback callback) {
            this.ads = ads;
            this.shownAd = shownAd;
            this.callback = callback;
        }

        @Override
        public void onAdClicked() {
            if (ads != null) {
                ads.onAdClicked();
            }
        }

        @Override
        public void onAdShowedFullScreenContent() {
            if (shown) {
                return; // guard against a duplicate impression callback
            }
            shown = true;
            Log.d(TAG, "onAdShowedFullScreenContent");
            if (ads != null) {
                ads.onAdShowedFullScreen();
            }
            if (callback != null) {
                callback.onShown();
            }
        }

        @Override
        public void onAdDismissedFullScreenContent() {
            if (done) {
                return;
            }
            done = true;
            Log.d(TAG, "onAdDismissedFullScreenContent");
            BaseFullScreenAds<?> a = ads;
            Object shown = shownAd;
            AdCallback cb = callback;
            ads = null;
            shownAd = null;
            callback = null;
            if (a != null) {
                a.handleDismiss(shown);
            }
            if (cb != null) {
                cb.onDismissed();
                cb.onComplete(true);
            }
        }

        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
            if (done) {
                return;
            }
            done = true;
            Log.d(TAG, "onAdFailedToShowFullScreenContent: " + adError);
            BaseFullScreenAds<?> a = ads;
            Object shown = shownAd;
            AdCallback cb = callback;
            ads = null;
            shownAd = null;
            callback = null;
            if (a != null) {
                a.transitionFailedToShow(shown);
            }
            if (cb != null) {
                cb.onFailedToShow(ShowError.of(adError));
                cb.onComplete(false);
            }
        }
    }
}
