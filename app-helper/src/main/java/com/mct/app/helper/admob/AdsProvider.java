package com.mct.app.helper.admob;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.mct.app.helper.admob.ads.AppOpenAds;
import com.mct.app.helper.admob.ads.BannerAds;
import com.mct.app.helper.admob.ads.BaseAds;
import com.mct.app.helper.admob.ads.InterstitialAds;
import com.mct.app.helper.admob.ads.NativeAds;
import com.mct.app.helper.admob.ads.RewardedAds;
import com.mct.app.helper.admob.ads.RewardedInterstitialAds;

import java.util.HashMap;
import java.util.Map;

public class AdsProvider {

    private final Map<String, BaseAds<?>> ads;

    private AdsProvider(@NonNull Builder builder) {
        ads = new HashMap<>(builder.adsMaps);
    }

    public Map<String, BaseAds<?>> getAds() {
        return ads;
    }

    public static class Builder {

        private static final long APP_OPEN_ADS_INTERVAL = 60 * 1000;
        private static final long INTERSTITIAL_ADS_INTERVAL = 30 * 1000;
        private static final long REWARDED_ADS_INTERVAL = 0;
        private static final long REWARDED_INTERSTITIAL_ADS_INTERVAL = 0;

        private final boolean forceTestAds;
        private final Map<String, BaseAds<?>> adsMaps;

        public Builder() {
            this(false);
        }

        public Builder(boolean forceTestAds) {
            this.forceTestAds = forceTestAds;
            this.adsMaps = new HashMap<>();
        }

        public Builder putAppOpenAds(String adsUnitId) {
            return putAds(new AppOpenAds(adsUnitId, APP_OPEN_ADS_INTERVAL));
        }

        public Builder putAppOpenAds(String adsUnitId, long adsInterval) {
            return putAds(new AppOpenAds(adsUnitId, adsInterval));
        }

        public Builder putBannerAds(String adsUnitId) {
            return putAds(new BannerAds(adsUnitId));
        }

        public Builder putInterstitialAds(String adsUnitId) {
            return putAds(new InterstitialAds(adsUnitId, INTERSTITIAL_ADS_INTERVAL));
        }

        public Builder putInterstitialAds(String adsUnitId, long adsInterval) {
            return putAds(new InterstitialAds(adsUnitId, adsInterval));
        }

        public Builder putNativeAds(String adsUnitId) {
            return putAds(new NativeAds(adsUnitId, AdsUtils.NATIVE_LAYOUT));
        }

        public Builder putNativeAds(String adsUnitId, @LayoutRes int nativeLayout) {
            return putAds(new NativeAds(adsUnitId, nativeLayout));
        }

        public Builder putRewardedAds(String adsUnitId) {
            return putAds(new RewardedAds(adsUnitId, REWARDED_ADS_INTERVAL));
        }

        public Builder putRewardedAds(String adsUnitId, long adsInterval) {
            return putAds(new RewardedAds(adsUnitId, adsInterval));
        }

        public Builder putRewardedInterstitialAds(String adsUnitId) {
            return putAds(new RewardedInterstitialAds(adsUnitId, REWARDED_INTERSTITIAL_ADS_INTERVAL));
        }

        public Builder putRewardedInterstitialAds(String adsUnitId, long adsInterval) {
            return putAds(new RewardedInterstitialAds(adsUnitId, adsInterval));
        }

        public Builder putAds(@NonNull BaseAds<?> ads) {
            ads.setForceTestAds(forceTestAds);
            adsMaps.put(ads.getAdsUnitId(), ads);
            return this;
        }

        public Builder putAds(@NonNull String id, @NonNull BaseAds<?> ads) {
            ads.setForceTestAds(forceTestAds);
            adsMaps.put(id, ads);
            return this;
        }

        public Builder removeAds(@NonNull String id) {
            adsMaps.remove(id);
            return this;
        }

        public AdsProvider build() {
            return new AdsProvider(this);
        }
    }

}
