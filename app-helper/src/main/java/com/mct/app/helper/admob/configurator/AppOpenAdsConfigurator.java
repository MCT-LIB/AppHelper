package com.mct.app.helper.admob.configurator;

import androidx.annotation.NonNull;

import com.mct.app.helper.admob.AdsConfigurator;
import com.mct.app.helper.admob.ads.AppOpenAds;

public class AppOpenAdsConfigurator extends BaseFullScreenAdsConfigurator<AppOpenAdsConfigurator, AppOpenAds> {

    public AppOpenAdsConfigurator(AdsConfigurator configurator, String adsUnitId) {
        super(configurator, adsUnitId);
    }

    @NonNull
    @Override
    protected AppOpenAds onCreateAds() {
        return new AppOpenAds(getAdsUnitId(), getAdsInterval());
    }
}
