package com.mct.app.helper.admob.configurator;

import androidx.annotation.NonNull;

import com.mct.app.helper.admob.AdsConfigurator;
import com.mct.app.helper.admob.ads.InterstitialAds;

public class InterstitialAdsConfigurator extends BaseFullScreenAdsConfigurator<InterstitialAdsConfigurator, InterstitialAds> {

    public InterstitialAdsConfigurator(AdsConfigurator configurator, String adsUnitId) {
        super(configurator, adsUnitId);
    }

    @NonNull
    @Override
    protected InterstitialAds onCreateAds() {
        return new InterstitialAds(getAdsUnitId(), getAdsInterval());
    }
}
