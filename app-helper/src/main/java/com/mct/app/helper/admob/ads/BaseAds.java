package com.mct.app.helper.admob.ads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Supplier;

import com.google.android.gms.ads.AdLoadCallback;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.mct.app.helper.admob.AdsManager;
import com.mct.app.helper.admob.Callback;
import com.mct.app.helper.admob.utils.TestAdsUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @noinspection SameParameterValue, unused
 */
public abstract class BaseAds<Ads> {

    protected static final String TAG = "BaseAds";

    private final String adsUnitId;
    private final long adsInterval;
    private String alias;
    private String customAlias;
    private Ads ads;
    private long adsLoadedTime = 0;
    private boolean isLoading = false;
    private boolean isShowing = false;
    private boolean isCanShow = true;
    private AdLoadCallbackImpl<Ads> adLoadCallback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable delayShowFlagRunnable = () -> setCanShow(true);

    public BaseAds(String adsUnitId, long adsInterval) {
        this.adsUnitId = adsUnitId;
        this.adsInterval = adsInterval;
    }

    protected abstract void onLoadAds(@NonNull Context context, @NonNull AdLoadCallback<Ads> callback);

    public final void load(@NonNull Context context, Callback success, Callback failure) {
        if (isLoading()) {
            setAdLoadCallbacks(success, failure);
            return;
        }
        if (isCanLoadAds()) {
            setLoading(true);
            onLoadAds(context, adLoadCallback = new AdLoadCallbackImpl<>(this, success, failure));
        } else {
            invokeCallback(failure);
        }
    }

    public final void forceClear() {
        clearAdLoadCallback();
        setAds(null);
        setLoading(false);
        setShowing(false);
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
        long dateDifference = System.currentTimeMillis() - adsLoadedTime;
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
     * @return on paid event listener
     */
    protected OnPaidEventListener getOnPaidEventListener() {
        Supplier<String> a = () -> TextUtils.isEmpty(customAlias) ? alias : customAlias.trim();
        return AdsManager.getInstance().getOnPaidEventListener(a);
    }

    /**
     * @return ads unit id to load based on debug
     */
    protected String getLoadAdsUnitId() {
        return AdsManager.getInstance().isDebug() ? TestAdsUtils.getAdsUnitIdTest(this) : adsUnitId;
    }

    /**
     * @return ads interval to load based on debug
     */
    protected long getLoadAdsInterval() {
        return AdsManager.getInstance().isDebug() ? TestAdsUtils.getIntervalTest(this) : adsInterval;
    }

    public String getAlias() {
        return alias;
    }

    public String getCustomAlias() {
        return customAlias;
    }

    public Ads getAds() {
        return ads;
    }

    public long getAdsLoadedTime() {
        return adsLoadedTime;
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

    public void setAlias(String a) {
        alias = a;
    }

    public void setCustomAlias(String a) {
        customAlias = a;
    }

    protected void setAds(Ads a) {
        ads = a;
    }

    protected void setAdsLoadedTime(long time) {
        adsLoadedTime = time;
    }

    protected void setLoading(boolean loading) {
        isLoading = loading;
    }

    protected void setShowing(boolean showing) {
        isShowing = showing;
    }

    protected void setCanShow(boolean canShow) {
        isCanShow = canShow;
    }

    protected void setAdLoadCallbacks(Callback success, Callback failure) {
        if (adLoadCallback != null) {
            adLoadCallback.success = success;
            adLoadCallback.failure = failure;
        }
    }

    protected void clearAdLoadCallback() {
        if (adLoadCallback != null) {
            adLoadCallback.dispose();
            adLoadCallback = null;
        }
    }

    protected void post(Runnable runnable) {
        handler.post(runnable);
    }

    protected void postDelayed(Runnable runnable, long delayMillis) {
        handler.postDelayed(runnable, delayMillis);
    }

    protected void removeCallbacks(Runnable runnable) {
        handler.removeCallbacks(runnable);
    }

    protected static boolean validateActivityToShow(Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
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
            synchronized (dispose) {
                if (isDisposed()) {
                    return;
                }
                Log.d(TAG, "onAdFailedToLoad: " + loadAdError);
                ads.setAds(null);
                ads.setLoading(false);
                invokeCallback(failure);
                dispose();
            }
        }

        @Override
        public void onAdLoaded(@NonNull Ads adsModel) {
            synchronized (dispose) {
                if (isDisposed()) {
                    return;
                }
                Log.d(TAG, "onAdLoaded: " + ads.getClass().getSimpleName());
                ads.setAds(adsModel);
                ads.setLoading(false);
                ads.setAdsLoadedTime(System.currentTimeMillis());
                invokeCallback(success);
                dispose();
            }
        }

        public boolean isDisposed() {
            return dispose.get();
        }

        public void dispose() {
            synchronized (dispose) {
                if (dispose.getAndSet(true)) {
                    return;
                }
                ads.clearAdLoadCallback();
                ads = null;
                success = null;
                failure = null;
            }
        }
    }

}
