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

import java.util.concurrent.LinkedBlockingQueue;

/**
 * A simple implementation of cache for preloaded native ads, this is not supposed to be used in any
 * production use case.
 */
public class NativeAdsPool {

    private final String adsUnitId;
    private final LinkedBlockingQueue<NativeAd> nativeAdsPool;
    private OnPoolRefreshedListener onPoolRefreshedListener;
    private AdLoader adLoader;

    public NativeAdsPool(String adsUnitId) {
        this.adsUnitId = adsUnitId;
        this.nativeAdsPool = new LinkedBlockingQueue<>();
    }

    public void init(Context context) {
        adLoader = new AdLoader.Builder(context, getAdsUnitId())
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .setVideoOptions(new VideoOptions.Builder().setStartMuted(true).build())
                        .build())
                .forNativeAd(nativeAd -> {
                    nativeAd.setOnPaidEventListener(getOnPaidEventListener());
                    push(nativeAd);
                    if (onPoolRefreshedListener != null) {
                        onPoolRefreshedListener.onPoolRefreshed();
                    }
                }).build();
    }

    private String getAdsUnitId() {
        return AdsManager.getInstance().isDebug() ? AdsUtils.NATIVE_ID : adsUnitId;
    }

    private OnPaidEventListener getOnPaidEventListener() {
        return AdsManager.getInstance().getOnPaidEventListener();
    }

    public void load(int numberOfAds) {
        adLoader.loadAds(new AdRequest.Builder().build(), numberOfAds);
    }

    public void setRefreshListener(OnPoolRefreshedListener listener) {
        this.onPoolRefreshedListener = listener;
    }

    public void push(NativeAd ad) {
        nativeAdsPool.add(ad);
    }

    public NativeAd pop() {
        return nativeAdsPool.poll();
    }

    public NativeAd peek() {
        return nativeAdsPool.peek();
    }

    /**
     * Listen to the refresh event from pool to handle new feeds.
     */
    public interface OnPoolRefreshedListener {
        void onPoolRefreshed();
    }

}