package com.mct.app.helper.admob.configurator;

import androidx.annotation.NonNull;

import com.mct.app.helper.admob.AdsConfigurator;
import com.mct.app.helper.admob.ads.BannerAds;

public class BannerAdsConfigurator extends BaseAdsConfigurator<BannerAdsConfigurator, BannerAds> {

    private boolean collapsible;
    private BannerAds.AdaptiveSize adaptiveSize = BannerAds.AdaptiveSize.COMPACT;

    public BannerAdsConfigurator(AdsConfigurator configurator, String adsUnitId) {
        super(configurator, adsUnitId);
    }

    public BannerAdsConfigurator collapsible(boolean collapsible) {
        this.collapsible = collapsible;
        return self();
    }

    public BannerAdsConfigurator compactAdaptive() {
        this.adaptiveSize = BannerAds.AdaptiveSize.COMPACT;
        return self();
    }

    public BannerAdsConfigurator largeAdaptive() {
        this.adaptiveSize = BannerAds.AdaptiveSize.LARGE;
        return self();
    }

    public BannerAdsConfigurator adaptiveSize(@NonNull BannerAds.AdaptiveSize adaptiveSize) {
        this.adaptiveSize = adaptiveSize;
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
        ads.setAdaptiveSize(adaptiveSize);
    }
}
