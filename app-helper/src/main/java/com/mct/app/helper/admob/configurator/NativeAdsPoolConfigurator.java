package com.mct.app.helper.admob.configurator;

import androidx.annotation.NonNull;

import com.mct.app.helper.admob.AdsConfigurator;
import com.mct.app.helper.admob.ads.NativeAdsPool;

public class NativeAdsPoolConfigurator extends BaseAdsConfigurator<NativeAdsPoolConfigurator, NativeAdsPool> {

    private int poolSize;

    public NativeAdsPoolConfigurator(AdsConfigurator configurator, String adsUnitId) {
        super(configurator, adsUnitId);
    }

    public NativeAdsPoolConfigurator setPoolSize(int poolSize) {
        this.poolSize = poolSize;
        return self();
    }

    @NonNull
    @Override
    protected NativeAdsPool onCreateAds() {
        return new NativeAdsPool(getAdsUnitId());
    }

    @Override
    protected void onAdsCreated(@NonNull NativeAdsPool ads) {
        super.onAdsCreated(ads);
        ads.setPoolSize(poolSize);
    }
}
