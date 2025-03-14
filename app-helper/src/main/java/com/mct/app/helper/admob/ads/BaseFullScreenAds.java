package com.mct.app.helper.admob.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.mct.app.helper.admob.Callback;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseFullScreenAds<Ads> extends BaseAds<Ads> {

    private long lastDismissTime = 0;
    private boolean autoLoadAfterDismiss = true;
    private OnAdsShowChangeListener onAdsShowChangeListener;

    public BaseFullScreenAds(String adsUnitId, long adsInterval) {
        super(adsUnitId, adsInterval);
    }

    protected abstract void onShowAds(
            @NonNull Activity activity,
            @NonNull Ads ads,
            @NonNull FullScreenContentCallback callback
    );

    public final void show(@NonNull String alias, @NonNull Activity activity, boolean waitLoadAndShow, Callback callback) {
        if (isLoading()) {
            if (waitLoadAndShow) {
                setAdLoadCallbacks(
                        () -> show(alias, activity, waitLoadAndShow, callback),
                        () -> invokeCallback(callback)
                );
            } else {
                invokeCallback(callback);
            }
            return;
        }
        if (isCanLoadAds()) {
            if (waitLoadAndShow) {
                load(activity.getApplicationContext(),
                        () -> show(alias, activity, waitLoadAndShow, callback),
                        () -> invokeCallback(callback)
                );
            } else {
                load(activity.getApplicationContext(), null, null);
                invokeCallback(callback);
            }
            return;
        }
        if (isCanShowAds() && validateActivityToShow(activity)) {
            setShowing(true);
            setCustomAlias(alias);
            onShowAds(activity, getAds(), new FullScreenContentCallbackImpl(this, activity.getApplicationContext(), callback));
        } else {
            invokeCallback(callback);
        }
    }

    public void setOnAdsShowChangeListener(OnAdsShowChangeListener listener) {
        this.onAdsShowChangeListener = listener;
    }

    public boolean isDismissNearly() {
        return System.currentTimeMillis() - lastDismissTime < 3000;
    }

    public long getLastDismissTime() {
        return lastDismissTime;
    }

    private void setLastDismissTime(long lastDismissTime) {
        this.lastDismissTime = lastDismissTime;
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

    protected static class FullScreenContentCallbackImpl extends FullScreenContentCallback {

        private final AtomicBoolean dispose;
        private BaseFullScreenAds<?> ads;
        private Context context;
        private Callback callback;

        public FullScreenContentCallbackImpl(BaseFullScreenAds<?> ads, Context context, Callback callback) {
            this.dispose = new AtomicBoolean(false);
            this.ads = ads;
            this.context = context;
            this.callback = callback;
        }

        @Override
        public void onAdClicked() {
            if (isDispose()) {
                return;
            }
            Log.d(TAG, "onAdClicked");
            ads.onAdClicked();
        }

        @Override
        public void onAdDismissedFullScreenContent() {
            if (isDispose()) {
                return;
            }
            Log.d(TAG, "onAdDismissedFullScreenContent: " + ads.getClass().getSimpleName());
            ads.onAdDismissedFullScreen();
            ads.postDelayShowFlag();
            ads.setLastDismissTime(System.currentTimeMillis());
            ads.setAds(null);
            ads.setShowing(false);
            if (ads.isAutoLoadAfterDismiss()) {
                ads.load(context, null, null);
            }
            invokeCallback(callback);
            dispose();
        }

        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
            if (isDispose()) {
                return;
            }
            Log.d(TAG, "onAdFailedToShowFullScreenContent: " + ads.getClass().getSimpleName() + " " + adError);
            ads.setLastDismissTime(0);
            ads.setAds(null);
            ads.setShowing(false);
            invokeCallback(callback);
            dispose();
        }

        @Override
        public void onAdShowedFullScreenContent() {
            if (isDispose()) {
                return;
            }
            Log.d(TAG, "onAdShowedFullScreenContent: " + ads.getClass().getSimpleName());
            ads.onAdShowedFullScreen();
        }

        private boolean isDispose() {
            return dispose.get();
        }

        private void dispose() {
            if (dispose.getAndSet(true)) {
                return;
            }
            ads = null;
            context = null;
            callback = null;
        }
    }

}
