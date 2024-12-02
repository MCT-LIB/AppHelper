package com.mct.app.helper.admob.utils;

import androidx.annotation.Nullable;

import com.mct.app.helper.admob.ads.AppOpenAds;
import com.mct.app.helper.admob.ads.BannerAds;
import com.mct.app.helper.admob.ads.InterstitialAds;
import com.mct.app.helper.admob.ads.NativeAds;
import com.mct.app.helper.admob.ads.NativeAdsPool;
import com.mct.app.helper.admob.ads.RewardedAds;
import com.mct.app.helper.admob.ads.RewardedInterstitialAds;

public class TestAdsUtils {

    public static final String APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921";
    public static final String BANNER_ID = "ca-app-pub-3940256099942544/6300978111";
    public static final String BANNER_COLLAPSE_ID = "ca-app-pub-3940256099942544/2014213617";
    public static final String INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";
    public static final String NATIVE_ID = "ca-app-pub-3940256099942544/2247696110";
    public static final String REWARDED_ID = "ca-app-pub-3940256099942544/5224354917";
    public static final String REWARDED_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/5354046379";

    @Nullable
    public static String getAdsUnitIdTest(Object object) {
        if (object instanceof AppOpenAds) {
            return APP_OPEN_ID;
        }
        if (object instanceof BannerAds) {
            return ((BannerAds) object).isCollapsible() ? BANNER_COLLAPSE_ID : BANNER_ID;
        }
        if (object instanceof InterstitialAds) {
            return INTERSTITIAL_ID;
        }
        if (object instanceof NativeAds) {
            return NATIVE_ID;
        }
        if (object instanceof NativeAdsPool) {
            return NATIVE_ID;
        }
        if (object instanceof RewardedAds) {
            return REWARDED_ID;
        }
        if (object instanceof RewardedInterstitialAds) {
            return REWARDED_INTERSTITIAL_ID;
        }
        return null;
    }

    public static long getIntervalTest(Object object) {
        if (object instanceof BannerAds) {
            return 0;
        }
        if (object instanceof InterstitialAds) {
            return 15 * 1000;
        }
        if (object instanceof NativeAds) {
            return 0;
        }
        if (object instanceof NativeAdsPool) {
            return 0;
        }
        if (object instanceof AppOpenAds) {
            return 30 * 1000;
        }
        if (object instanceof RewardedAds) {
            return 0;
        }
        if (object instanceof RewardedInterstitialAds) {
            return 0;
        }
        return -1;
    }

    private TestAdsUtils() {
        //no instance
    }
}
