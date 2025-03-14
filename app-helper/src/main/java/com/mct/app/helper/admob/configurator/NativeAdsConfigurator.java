package com.mct.app.helper.admob.configurator;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.mct.app.helper.admob.AdsConfigurator;
import com.mct.app.helper.admob.ads.NativeAds;
import com.mct.app.helper.admob.ads.natives.NativeTemplate;
import com.mct.app.helper.admob.ads.natives.NativeTemplateStyle;

public class NativeAdsConfigurator extends BaseAdsConfigurator<NativeAdsConfigurator, NativeAds> {

    private int layout;
    private NativeTemplateStyle templateStyle;

    public NativeAdsConfigurator(AdsConfigurator configurator, String adsUnitId) {
        super(configurator, adsUnitId);
    }

    public NativeAdsConfigurator layout(@LayoutRes int layoutRes) {
        this.layout = layoutRes;
        return self();
    }

    public NativeAdsConfigurator template(@NonNull NativeTemplate template) {
        this.layout = template.layoutRes;
        return self();
    }

    public NativeAdsConfigurator style(NativeTemplateStyle templateStyle) {
        this.templateStyle = templateStyle;
        return self();
    }

    @NonNull
    @Override
    protected NativeAds onCreateAds() {
        return new NativeAds(getAdsUnitId());
    }

    @Override
    protected void onAdsCreated(@NonNull NativeAds ads) {
        super.onAdsCreated(ads);
        ads.setLayoutRes(layout);
        ads.setTemplateStyle(templateStyle);
    }
}
