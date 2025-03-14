package com.mct.app.helper.admob.configurator;

import androidx.annotation.NonNull;

import com.mct.app.helper.admob.AdsConfigurator;
import com.mct.app.helper.admob.ads.BaseFullScreenAds;

public abstract class BaseFullScreenAdsConfigurator<C extends BaseAdsConfigurator<C, Ads>, Ads extends BaseFullScreenAds<?>> extends BaseAdsConfigurator<C, Ads> {

    private boolean autoLoadAfterDismiss = true;

    public BaseFullScreenAdsConfigurator(AdsConfigurator configurator, String adsUnitId) {
        super(configurator, adsUnitId);
    }

    public C autoLoadAfterDismiss(boolean autoLoadAfterDismiss) {
        this.autoLoadAfterDismiss = autoLoadAfterDismiss;
        return self();
    }

    @Override
    protected void onAdsCreated(@NonNull Ads ads) {
        super.onAdsCreated(ads);
        ads.setAutoLoadAfterDismiss(autoLoadAfterDismiss);
    }
}
