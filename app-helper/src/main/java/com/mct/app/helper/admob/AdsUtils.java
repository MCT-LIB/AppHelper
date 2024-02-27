package com.mct.app.helper.admob;

import androidx.annotation.Nullable;

import com.mct.app.helper.R;
import com.mct.app.helper.admob.ads.AppOpenAds;
import com.mct.app.helper.admob.ads.BannerAds;
import com.mct.app.helper.admob.ads.InterstitialAds;
import com.mct.app.helper.admob.ads.NativeAds;
import com.mct.app.helper.admob.ads.RewardedAds;
import com.mct.app.helper.admob.ads.RewardedInterstitialAds;

public class AdsUtils {

    public static final int NATIVE_LAYOUT = R.layout.layout_ad_native_default;

    @Nullable
    public static String getAdsUnitIdTest(Object object) {
        if (object instanceof BannerAds) {
            return "ca-app-pub-3940256099942544/6300978111";
        }
        if (object instanceof InterstitialAds) {
            return "ca-app-pub-3940256099942544/1033173712";
        }
        if (object instanceof NativeAds) {
            return "ca-app-pub-3940256099942544/2247696110";
        }
        if (object instanceof AppOpenAds) {
            return "ca-app-pub-3940256099942544/9257395921";
        }
        if (object instanceof RewardedAds) {
            return "ca-app-pub-3940256099942544/5224354917";
        }
        if (object instanceof RewardedInterstitialAds) {
            return "ca-app-pub-3940256099942544/5354046379";
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

    private AdsUtils() {
        //no instance
    }
}
