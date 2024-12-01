package com.mct.app.helper.admob.ads.natives;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
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

    private boolean isLoading;
    private boolean isDispose;
    private OnPoolRefreshedListener onPoolRefreshedListener;

    public NativeAdsPool(Context context, String adsUnitId) {
        this(context, adsUnitId, adsUnitId);
    }

    public NativeAdsPool(Context context, String adsUnitId, String alias) {
        if (AdsManager.getInstance().isPremium()) {
            adLoader = null;
        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            String adsUnit = AdsManager.getInstance().isDebug() ? AdsUtils.NATIVE_ID : adsUnitId;
            adLoader = new AdLoader.Builder(context, adsUnit)
                    .withNativeAdOptions(new NativeAdOptions.Builder()
                            .setVideoOptions(new VideoOptions.Builder().setStartMuted(true).build())
                            .build())
                    .withAdListener(new AdListener() {
                        @Override
                        public void onAdLoaded() {
                            isLoading = false;
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            isLoading = false;
                            handler.removeCallbacksAndMessages(null);
                            handler.postDelayed(() -> {
                                if (onPoolRefreshedListener != null) {
                                    onPoolRefreshedListener.onPoolRefreshed();
                                }
                            }, 200);
                        }
                    })
                    .forNativeAd(nativeAd -> {
                        if (isDispose) {
                            nativeAd.destroy();
                        } else {
                            nativeAd.setOnPaidEventListener(AdsManager.getInstance().getOnPaidEventListener(() -> alias));
                            nativeAdsList.addLast(nativeAd);
                            handler.removeCallbacksAndMessages(null);
                            handler.postDelayed(() -> {
                                if (onPoolRefreshedListener != null) {
                                    onPoolRefreshedListener.onPoolRefreshed();
                                }
                            }, 100);
                        }
                    }).build();
        }
    }

    public void loadAds(int numberOfAds) {
        if (isAdsUnavailable()) {
            return;
        }
        if (isLoading) {
            return;
        }
        isLoading = true;
        adLoader.loadAds(new AdRequest.Builder().build(), numberOfAds);
    }

    public void clearAds() {
        for (NativeAd nativeAd : nativeAdsList) {
            nativeAd.destroy();
        }
        nativeAdsList.clear();
    }

    public boolean isLoading() {
        return isLoading;
    }

    public boolean isDispose() {
        return isDispose;
    }

    public void dispose() {
        if (isDispose) {
            return;
        }
        isDispose = true;
    }

    public boolean isAdsUnavailable() {
        return adLoader == null || isDispose;
    }

    public void setOnPoolRefreshedListener(OnPoolRefreshedListener listener) {
        onPoolRefreshedListener = listener;
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

    public interface OnPoolRefreshedListener {
        void onPoolRefreshed();
    }

}