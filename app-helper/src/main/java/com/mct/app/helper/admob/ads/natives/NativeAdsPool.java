package com.mct.app.helper.admob.ads.natives;

import android.content.Context;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.mct.app.helper.admob.AdsManager;
import com.mct.app.helper.admob.utils.AdsUtils;

import java.util.ArrayDeque;

/**
 * A simple implementation of cache for preloaded native ads, this is not supposed to be used in any
 * production use case.
 */
public class NativeAdsPool {

    private final AdLoader adLoader;
    private final ArrayDeque<NativeAd> nativeAdsList = new ArrayDeque<>();

    private boolean isDispose;
    private OnPoolRefreshedListener onPoolRefreshedListener;

    public NativeAdsPool(Context context, String adsUnitId) {
        if (AdsManager.getInstance().isPremium()) {
            adLoader = null;
        } else {
            String id = AdsManager.getInstance().isDebug() ? AdsUtils.NATIVE_ID : adsUnitId;
            adLoader = new AdLoader.Builder(context, id)
                    .withNativeAdOptions(new NativeAdOptions.Builder()
                            .setVideoOptions(new VideoOptions.Builder().setStartMuted(true).build())
                            .build())
                    .forNativeAd(nativeAd -> {
                        if (isDispose) {
                            nativeAd.destroy();
                        } else {
                            nativeAd.setOnPaidEventListener(AdsManager.getInstance().getOnPaidEventListener());
                            nativeAdsList.addLast(nativeAd);
                            if (onPoolRefreshedListener != null) {
                                onPoolRefreshedListener.onPoolRefreshed();
                            }
                        }
                    }).build();
        }
    }

    public void loadAds(int numberOfAds) {
        if (isAdsUnavailable()) {
            return;
        }
        adLoader.loadAds(new AdRequest.Builder().build(), numberOfAds);
    }

    public void clearAds() {
        for (NativeAd nativeAd : nativeAdsList) {
            nativeAd.destroy();
        }
        nativeAdsList.clear();
    }

    public void dispose() {
        isDispose = true;
        clearAds();
    }

    public boolean isAdsUnavailable() {
        return adLoader == null || isDispose;
    }

    public void setOnPoolRefreshedListener(OnPoolRefreshedListener listener) {
        onPoolRefreshedListener = listener;
    }

    public NativeAd get() {
        if (nativeAdsList.isEmpty()) {
            return null;
        }
        NativeAd nativeAd = nativeAdsList.pollLast();
        nativeAdsList.addFirst(nativeAd);
        return nativeAd;
    }

    public interface OnPoolRefreshedListener {
        void onPoolRefreshed();
    }

}