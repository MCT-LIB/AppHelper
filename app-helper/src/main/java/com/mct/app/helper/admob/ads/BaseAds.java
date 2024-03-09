package com.mct.app.helper.admob.ads;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdLoadCallback;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.mct.app.helper.BuildConfig;
import com.mct.app.helper.admob.AdsUtils;
import com.mct.app.helper.admob.Callback;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseAds<Ads> {

    protected static final String TAG = "BaseAds";

    private final String adsUnitId;
    private final long adsInterval;
    private Ads ads;

    private long loadTimeAd = 0;
    private boolean isLoading = false;
    private boolean isShowing = false;
    private boolean isCanShow = true;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable delayShowFlagRunnable = () -> isCanShow = true;

    public BaseAds(String adsUnitId, long adsInterval) {
        this.adsUnitId = adsUnitId;
        this.adsInterval = adsInterval;
    }

    protected abstract void onLoadAds(@NonNull Context context, @NonNull AdLoadCallback<Ads> callback);

    public final void load(@NonNull Context context, Callback success, Callback failure) {
        if (isCanLoadAds()) {
            setLoading(true);
            onLoadAds(context, new AdLoadCallbackImpl<>(this, success, failure));
        } else {
            invokeCallback(failure);
        }
    }

    public final void postDelayShowFlag() {
        isCanShow = false;
        handler.removeCallbacks(delayShowFlagRunnable);
        handler.postDelayed(delayShowFlagRunnable, getLoadAdsInterval());
    }

    public final boolean isCanLoadAds() {
        if (isLoading) {
            return false;
        }
        if (isShowing) {
            return false;
        }
        if (ads == null) {
            return true;
        } else {
            return isAdsOverdue();
        }
    }

    public final boolean isCanShowAds() {
        if (isLoading) {
            return false;
        }
        if (isShowing) {
            return false;
        }
        if (!isCanShow) {
            return false;
        }
        if (ads == null) {
            return false;
        } else {
            return !isAdsOverdue();
        }
    }

    private boolean isAdsOverdue() {
        long dateDifference = System.currentTimeMillis() - loadTimeAd;
        long numMilliSecondsPerHour = 3600000;
        return dateDifference > (numMilliSecondsPerHour * 4);
    }

    public AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    /**
     * @return real ads unit id
     */
    public String getAdsUnitId() {
        return adsUnitId;
    }

    /**
     * @return real ads interval
     */
    public long getAdsInterval() {
        return adsInterval;
    }

    /**
     * @return ads unit id to load based on debug
     */
    protected String getLoadAdsUnitId() {
        return BuildConfig.DEBUG ? AdsUtils.getAdsUnitIdTest(this) : adsUnitId;
    }

    /**
     * @return ads interval to load based on debug
     */
    protected long getLoadAdsInterval() {
        return BuildConfig.DEBUG ? AdsUtils.getIntervalTest(this) : adsInterval;
    }

    public long getLoadTimeAd() {
        return loadTimeAd;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public boolean isShowing() {
        return isShowing;
    }

    public boolean isCanShow() {
        return isCanShow;
    }

    public Ads getAds() {
        return ads;
    }

    protected void setAds(Ads ads) {
        this.ads = ads;
    }

    protected void setLoadTimeAd(long loadTimeAd) {
        this.loadTimeAd = loadTimeAd;
    }

    protected void setLoading(boolean loading) {
        isLoading = loading;
    }

    protected void setShowing(boolean showing) {
        isShowing = showing;
    }

    protected static void invokeCallback(Callback callback) {
        if (callback != null) {
            callback.callback();
        }
    }

    protected static class AdLoadCallbackImpl<Ads> extends AdLoadCallback<Ads> {

        private final AtomicBoolean dispose;
        private BaseAds<Ads> ads;
        private Callback success;
        private Callback failure;

        public AdLoadCallbackImpl(BaseAds<Ads> ads, Callback success, Callback failure) {
            this.dispose = new AtomicBoolean(false);
            this.ads = ads;
            this.success = success;
            this.failure = failure;
        }

        @Override
        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
            if (isDispose()) {
                return;
            }
            Log.d(TAG, "onAdFailedToLoad: " + loadAdError);
            ads.setAds(null);
            ads.setLoading(false);
            invokeCallback(failure);
            dispose();
        }

        @Override
        public void onAdLoaded(@NonNull Ads ads) {
            if (isDispose()) {
                return;
            }
            Log.d(TAG, "onAdLoaded: " + this.ads.getClass().getSimpleName());
            this.ads.setAds(ads);
            this.ads.setLoading(false);
            this.ads.setLoadTimeAd(System.currentTimeMillis());
            invokeCallback(success);
            dispose();
        }

        private boolean isDispose() {
            return dispose.get();
        }

        private void dispose() {
            if (dispose.getAndSet(true)) {
                return;
            }
            ads = null;
            success = null;
            failure = null;
        }
    }

}
