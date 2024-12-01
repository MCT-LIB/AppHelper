package com.mct.app.helper.admob.configurator;

import com.mct.app.helper.admob.AdsConfigurator;
import com.mct.app.helper.admob.ads.BaseAds;

import java.util.Objects;

@SuppressWarnings("unchecked")
public abstract class BaseAdsConfigurator<C extends BaseAdsConfigurator<C, Ads>, Ads extends BaseAds<?>> {

    private final AdsConfigurator configurator;
    private String alias;
    private String adsUnitId;
    private long adsInterval;

    public BaseAdsConfigurator(AdsConfigurator configurator, String adsUnitId) {
        this.configurator = configurator;
        this.adsUnitId = Objects.requireNonNull(adsUnitId);
    }

    public C alias(String alias) {
        this.alias = Objects.requireNonNull(alias);
        return (C) this;
    }

    public C adsUnitId(String adsUnitId) {
        this.adsUnitId = Objects.requireNonNull(adsUnitId);
        return (C) this;
    }

    public C adsInterval(long adsInterval) {
        this.adsInterval = adsInterval;
        return (C) this;
    }

    public AdsConfigurator and() {
        String id = alias != null ? alias : adsUnitId;
        BaseAds<?> ads = makeAds(adsUnitId, adsInterval);
        return configurator.putAds(id, ads);
    }

    public void apply() {
        and().apply();
    }

    protected abstract Ads makeAds(String adsUnitId, long adsInterval);

}
