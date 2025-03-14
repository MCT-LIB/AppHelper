package com.mct.app.helper.admob.configurator;

import androidx.annotation.NonNull;

import com.mct.app.helper.admob.AdsConfigurator;
import com.mct.app.helper.admob.ads.RewardedAds;

public class RewardedAdsConfigurator extends BaseFullScreenAdsConfigurator<RewardedAdsConfigurator, RewardedAds> {

    public RewardedAdsConfigurator(AdsConfigurator configurator, String adsUnitId) {
        super(configurator, adsUnitId);
    }

    @NonNull
    @Override
    protected RewardedAds onCreateAds() {
        return new RewardedAds(getAdsUnitId(), getAdsInterval());
    }
}
