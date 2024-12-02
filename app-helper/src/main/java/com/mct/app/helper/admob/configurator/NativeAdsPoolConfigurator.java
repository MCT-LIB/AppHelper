package com.mct.app.helper.admob.configurator;

import com.mct.app.helper.admob.AdsConfigurator;
import com.mct.app.helper.admob.ads.NativeAdsPool;

public class NativeAdsPoolConfigurator extends BaseAdsConfigurator<NativeAdsPoolConfigurator, NativeAdsPool> {

    private int poolSize;

    public NativeAdsPoolConfigurator(AdsConfigurator configurator, String adsUnitId) {
        super(configurator, adsUnitId);
    }

    public NativeAdsPoolConfigurator setPoolSize(int poolSize) {
        this.poolSize = poolSize;
        return this;
    }

    @Override
    protected NativeAdsPool makeAds(String adsUnitId, long adsInterval) {
        return new NativeAdsPool(adsUnitId, poolSize);
    }
}
