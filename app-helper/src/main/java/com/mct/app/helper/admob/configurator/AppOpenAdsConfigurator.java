package com.mct.app.helper.admob.configurator;

import com.mct.app.helper.admob.AdsConfigurator;
import com.mct.app.helper.admob.ads.AppOpenAds;

public class AppOpenAdsConfigurator extends BaseAdsConfigurator<AppOpenAdsConfigurator, AppOpenAds> {

    public AppOpenAdsConfigurator(AdsConfigurator configurator, String adsUnitId) {
        super(configurator, adsUnitId);
    }

    @Override
    protected AppOpenAds makeAds(String adsUnitId, long adsInterval) {
        return new AppOpenAds(adsUnitId, adsInterval);
    }

}
