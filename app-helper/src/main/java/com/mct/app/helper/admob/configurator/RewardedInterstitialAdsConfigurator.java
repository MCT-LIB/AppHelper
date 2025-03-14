package com.mct.app.helper.admob.configurator;

import androidx.annotation.NonNull;

import com.mct.app.helper.admob.AdsConfigurator;
import com.mct.app.helper.admob.ads.RewardedInterstitialAds;

public class RewardedInterstitialAdsConfigurator extends BaseFullScreenAdsConfigurator<RewardedInterstitialAdsConfigurator, RewardedInterstitialAds> {

    public RewardedInterstitialAdsConfigurator(AdsConfigurator configurator, String adsUnitId) {
        super(configurator, adsUnitId);
    }

    @NonNull
    @Override
    protected RewardedInterstitialAds onCreateAds() {
        return new RewardedInterstitialAds(getAdsUnitId(), getAdsInterval());
    }
}
