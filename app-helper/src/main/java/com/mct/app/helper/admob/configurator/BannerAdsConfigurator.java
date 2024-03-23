package com.mct.app.helper.admob.configurator;

import com.mct.app.helper.admob.AdsConfigurator;
import com.mct.app.helper.admob.ads.BannerAds;

public class BannerAdsConfigurator extends BaseAdsConfigurator<BannerAdsConfigurator, BannerAds> {

    private boolean collapsible;

    public BannerAdsConfigurator(AdsConfigurator configurator, String adsUnitId) {
        super(configurator, adsUnitId);
    }

    public BannerAdsConfigurator collapsible(boolean collapsible) {
        this.collapsible = collapsible;
        return this;
    }

    @Override
    protected BannerAds makeAds(String adsUnitId, long adsInterval) {
        return new BannerAds(adsUnitId, collapsible);
    }
}
