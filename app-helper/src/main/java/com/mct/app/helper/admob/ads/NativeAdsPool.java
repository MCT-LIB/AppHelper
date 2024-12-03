package com.mct.app.helper.admob.ads;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple implementation of cache for preloaded native ads
 */
public class NativeAdsPool extends BaseAds<Object> {

    private static final int POOL_MAX_SIZE = 10;
    private static final int POOL_MAX_LOAD_SIZE = 5;

    private final int poolSize;
    private final List<NativeAd> nativeAdsList = new ArrayList<>();
    private final Set<OnPoolRefreshedListener> onPoolRefreshedListeners = new HashSet<>();

    private int position = -1;

    public NativeAdsPool(String adsUnitId, int poolSize) {
        super(adsUnitId, 0);
        this.poolSize = Math.min(poolSize, POOL_MAX_SIZE);
    }

    public NativeAd get() {
        return get(++position);
    }

    public NativeAd get(int position) {
        return nativeAdsList.isEmpty() ? null : nativeAdsList.get(position % nativeAdsList.size());
    }

    public void destroy() {
        forceClear();
        nativeAdsList.forEach(NativeAd::destroy);
        nativeAdsList.clear();
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

    private void notifyPoolRefreshed() {
        for (OnPoolRefreshedListener listener : onPoolRefreshedListeners) {
            listener.onPoolRefreshed();
        }
    }

    @Override
    protected void onLoadAds(@NonNull Context context, @NonNull AdsLoadCallback<Object> callback) {
        // Check if pool size is valid
        if (poolSize <= 0) {
            callback.onAdsFailedToLoad(new LoadAdError(88, "Pool size is 0", AdError.UNDEFINED_DOMAIN, null, null));
            return;
        }
        // Check if pool is already full
        if (nativeAdsList.size() >= poolSize) {
            callback.onAdsLoaded(new Object());
            return;
        }
        // Load ads
        notifyPoolRefreshed();
        AtomicInteger adRequestSize = new AtomicInteger();
        AdLoader adLoader = new AdLoader.Builder(context, getLoadAdsUnitId())
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .setVideoOptions(new VideoOptions.Builder().setStartMuted(true).build())
                        .build())
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        boolean disposed = callback.isDisposed();
                        callback.onAdsFailedToLoad(loadAdError);
                        if (!disposed) {
                            notifyPoolRefreshed();
                        }
                    }
                })
                .forNativeAd(nativeAd -> {
                    nativeAd.setOnPaidEventListener(getOnPaidEventListener());
                    nativeAdsList.add(nativeAd);
                    if (adRequestSize.decrementAndGet() == 0) {
                        boolean disposed = callback.isDisposed();
                        callback.onAdsLoaded(1);
                        if (!disposed) {
                            notifyPoolRefreshed();
                        }
                    }
                }).build();

        adRequestSize.set(Math.min(poolSize - nativeAdsList.size(), POOL_MAX_LOAD_SIZE)); // Number of ads to load

        if (adRequestSize.get() == 1) {
            adLoader.loadAd(getAdRequest());
        } else {
            adLoader.loadAds(getAdRequest(), adRequestSize.get());
        }
    }

    public interface OnPoolRefreshedListener {

        void onPoolRefreshed();
    }

}