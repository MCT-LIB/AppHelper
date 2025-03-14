package com.mct.app.helper.admob.configurator;

import androidx.annotation.NonNull;

import com.mct.app.helper.admob.AdsConfigurator;
import com.mct.app.helper.admob.ads.BannerAds;

public class BannerAdsConfigurator extends BaseAdsConfigurator<BannerAdsConfigurator, BannerAds> {

    private boolean collapsible;

    public BannerAdsConfigurator(AdsConfigurator configurator, String adsUnitId) {
        super(configurator, adsUnitId);
    }

    public BannerAdsConfigurator collapsible(boolean collapsible) {
        this.collapsible = collapsible;
        return self();
    }

    @NonNull
    @Override
    protected BannerAds onCreateAds() {
        return new BannerAds(getAdsUnitId());
    }

    @Override
    protected void onAdsCreated(@NonNull BannerAds ads) {
        super.onAdsCreated(ads);
        ads.setCollapsible(collapsible);
    }
}
