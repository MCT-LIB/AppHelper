package com.mct.app.helper.admob.ads;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoadCallback;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * A simple implementation of cache for preloaded native ads
 */
public class NativeAdsPool extends BaseAds<Object> {

    private static final int POOL_MAX_SIZE = 10;

    private final int poolSize;
    private final ArrayDeque<NativeAd> nativeAdsList = new ArrayDeque<>();
    private final Set<OnPoolRefreshedListener> onPoolRefreshedListeners = new HashSet<>();

    public NativeAdsPool(String adsUnitId, int poolSize) {
        super(adsUnitId, 0);
        this.poolSize = Math.min(poolSize, POOL_MAX_SIZE);
    }

    public void addOnPoolRefreshedListener(OnPoolRefreshedListener listener) {
        if (listener == null) {
            return;
        }
        onPoolRefreshedListeners.add(listener);
    }

    public void removeOnPoolRefreshedListener(OnPoolRefreshedListener listener) {
        if (listener == null) {
            return;
        }
        onPoolRefreshedListeners.remove(listener);
    }

    public int size() {
        return nativeAdsList.size();
    }

    public NativeAd get() {
        if (nativeAdsList.isEmpty()) {
            return null;
        }
        NativeAd nativeAd = nativeAdsList.pollLast();
        nativeAdsList.addFirst(nativeAd);
        return nativeAd;
    }

    public void destroy() {
        forceClear();
        nativeAdsList.forEach(NativeAd::destroy);
        nativeAdsList.clear();
    }

    private void notifyPoolRefreshed() {
        for (OnPoolRefreshedListener listener : onPoolRefreshedListeners) {
            listener.onPoolRefreshed();
        }
    }

    @Override
    protected void onLoadAds(@NonNull Context context, @NonNull AdLoadCallback<Object> callback) {
        // Check if pool size is valid
        if (poolSize <= 0) {
            callback.onAdFailedToLoad(new LoadAdError(88, "Pool size is 0", AdError.UNDEFINED_DOMAIN, null, null));
            return;
        }
        // Check if pool is already full
        if (nativeAdsList.size() >= poolSize) {
            callback.onAdLoaded(new Object());
            return;
        }
        // Load ads
        final long refreshedDelay = 100;
        Runnable refreshed = this::notifyPoolRefreshed;
        AdLoader adLoader = new AdLoader.Builder(context, getLoadAdsUnitId())
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .setVideoOptions(new VideoOptions.Builder().setStartMuted(true).build())
                        .build())
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        postDelayed(() -> callback.onAdLoaded(new Object()), 200);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        callback.onAdFailedToLoad(loadAdError);
                        removeCallbacks(refreshed);
                        postDelayed(refreshed, refreshedDelay);
                    }
                })
                .forNativeAd(nativeAd -> {
                    nativeAd.setOnPaidEventListener(getOnPaidEventListener());
                    nativeAdsList.addLast(nativeAd);
                    removeCallbacks(refreshed);
                    postDelayed(refreshed, refreshedDelay);
                }).build();

        int size = poolSize - nativeAdsList.size();
        if (size == 1) {
            adLoader.loadAd(getAdRequest());
        } else {
            adLoader.loadAds(getAdRequest(), size);
        }
    }

    public interface OnPoolRefreshedListener {

        void onPoolRefreshed();
    }

}