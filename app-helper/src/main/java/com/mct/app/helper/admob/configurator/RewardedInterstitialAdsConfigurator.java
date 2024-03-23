package com.mct.app.helper.admob.configurator;

import com.mct.app.helper.admob.AdsConfigurator;
import com.mct.app.helper.admob.ads.RewardedInterstitialAds;

public class RewardedInterstitialAdsConfigurator extends BaseAdsConfigurator<RewardedInterstitialAdsConfigurator, RewardedInterstitialAds> {

    public RewardedInterstitialAdsConfigurator(AdsConfigurator configurator, String adsUnitId) {
        super(configurator, adsUnitId);
    }

    @Override
    protected RewardedInterstitialAds makeAds(String adsUnitId, long adsInterval) {
        return new RewardedInterstitialAds(adsUnitId, adsInterval);
    }
}
