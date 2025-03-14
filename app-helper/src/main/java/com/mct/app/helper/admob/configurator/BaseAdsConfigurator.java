package com.mct.app.helper.admob.configurator;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import com.mct.app.helper.admob.AdsConfigurator;
import com.mct.app.helper.admob.ads.BaseAds;

import java.util.Objects;
import java.util.Optional;

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
        return self();
    }

    public C adsUnitId(String adsUnitId) {
        this.adsUnitId = Objects.requireNonNull(adsUnitId);
        return self();
    }

    public C adsInterval(long adsInterval) {
        this.adsInterval = adsInterval;
        return self();
    }

    public AdsConfigurator and() {
        Ads ads = onCreateAds();
        onAdsCreated(ads);
        return configurator.putAds(Optional.ofNullable(alias).orElse(adsUnitId), ads);
    }

    public void apply() {
        and().apply();
    }

    @NonNull
    protected abstract Ads onCreateAds();

    @CallSuper
    protected void onAdsCreated(@NonNull Ads ads) {
    }

    protected final String getAdsUnitId() {
        return adsUnitId;
    }

    protected final long getAdsInterval() {
        return adsInterval;
    }

    protected final C self() {
        //noinspection unchecked
        return (C) this;
    }
}
