package com.mct.app.helper.admob.configurator;

import com.mct.app.helper.admob.AdsConfigurator;
import com.mct.app.helper.admob.ads.InterstitialAds;

public class InterstitialAdsConfigurator extends BaseAdsConfigurator<InterstitialAdsConfigurator, InterstitialAds> {

    public InterstitialAdsConfigurator(AdsConfigurator configurator, String adsUnitId) {
        super(configurator, adsUnitId);
    }

    @Override
    protected InterstitialAds makeAds(String adsUnitId, long adsInterval) {
        return new InterstitialAds(adsUnitId, adsInterval);
    }
}
