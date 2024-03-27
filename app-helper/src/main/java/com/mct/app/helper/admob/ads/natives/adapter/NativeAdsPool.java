package com.mct.app.helper.admob.ads.natives.adapter;

import android.content.Context;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.mct.app.helper.admob.AdsManager;
import com.mct.app.helper.admob.utils.AdsUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A simple implementation of cache for preloaded native ads, this is not supposed to be used in any
 * production use case.
 */
public class NativeAdsPool {

    private final String adsUnitId;
    private final AtomicBoolean isAdsUnavailable;
    private final LinkedBlockingQueue<NativeAd> nativeAdsPool;
    private final List<OnPoolRefreshedListener> onPoolRefreshedListeners;
    private AdLoader adLoader;

    public NativeAdsPool(String adsUnitId) {
        this.adsUnitId = adsUnitId;
        this.isAdsUnavailable = new AtomicBoolean();
        this.nativeAdsPool = new LinkedBlockingQueue<>();
        this.onPoolRefreshedListeners = new CopyOnWriteArrayList<>();
    }

    public void init(Context context) {
        boolean isPremium = AdsManager.getInstance().isPremium();
        String adsUnitID = AdsManager.getInstance().isDebug() ? AdsUtils.NATIVE_ID : adsUnitId;
        OnPaidEventListener onPaidEventListener = AdsManager.getInstance().getOnPaidEventListener();
        isAdsUnavailable.set(isPremium);
        adLoader = new AdLoader.Builder(context, adsUnitID)
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .setVideoOptions(new VideoOptions.Builder().setStartMuted(true).build())
                        .build())
                .forNativeAd(nativeAd -> {
                    nativeAd.setOnPaidEventListener(onPaidEventListener);
                    push(nativeAd);
                    for (OnPoolRefreshedListener onPoolRefreshedListener : onPoolRefreshedListeners) {
                        onPoolRefreshedListener.onPoolRefreshed();
                    }
                }).build();
    }

    public void load(int numberOfAds) {
        if (isAdsUnavailable()) {
            return;
        }
        adLoader.loadAds(new AdRequest.Builder().build(), numberOfAds);
    }

    public String getAdsUnitId() {
        return adsUnitId;
    }

    public boolean isAdsUnavailable() {
        return isAdsUnavailable.get();
    }

    public void registerOnPoolRefreshedListener(OnPoolRefreshedListener listener) {
        if (onPoolRefreshedListeners.contains(listener)) {
            return;
        }
        onPoolRefreshedListeners.add(listener);
    }

    public void unregisterOnPoolRefreshedListener(OnPoolRefreshedListener listener) {
        onPoolRefreshedListeners.remove(listener);
    }

    public void push(NativeAd ad) {
        if (ad == null || nativeAdsPool.contains(ad)) {
            return;
        }
        nativeAdsPool.add(ad);
    }

    public NativeAd pop() {
        if (isAdsUnavailable()) {
            return null;
        }
        return nativeAdsPool.poll();
    }

    public NativeAd peek() {
        if (isAdsUnavailable()) {
            return null;
        }
        return nativeAdsPool.peek();
    }

    /**
     * Listen to the refresh event from pool to handle new feeds.
     */
    public interface OnPoolRefreshedListener {
        void onPoolRefreshed();
    }

}