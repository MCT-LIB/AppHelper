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
        return this;
    }

    public NativeAdsConfigurator template(@NonNull NativeTemplate template) {
        this.layout = template.layoutRes;
        return this;
    }

    public NativeAdsConfigurator style(NativeTemplateStyle templateStyle) {
        this.templateStyle = templateStyle;
        return this;
    }

    @Override
    protected NativeAds makeAds(String adsUnitId, long adsInterval) {
        NativeAds ads = new NativeAds(adsUnitId, layout);
        ads.setTemplateStyle(templateStyle);
        return ads;
    }
}
