package com.mct.app.helper.admob.configurator;

import com.mct.app.helper.admob.AdsConfigurator;
import com.mct.app.helper.admob.ads.RewardedAds;

public class RewardedAdsConfigurator extends BaseAdsConfigurator<RewardedAdsConfigurator, RewardedAds> {

    public RewardedAdsConfigurator(AdsConfigurator configurator, String adsUnitId) {
        super(configurator, adsUnitId);
    }

    @Override
    protected RewardedAds makeAds(String adsUnitId, long adsInterval) {
        return new RewardedAds(adsUnitId, adsInterval);
    }
}
