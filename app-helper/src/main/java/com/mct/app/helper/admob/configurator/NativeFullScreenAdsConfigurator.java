package com.mct.app.helper.admob.configurator;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.mct.app.helper.admob.AdsConfigurator;
import com.mct.app.helper.admob.ads.NativeFullScreenAds;
import com.mct.app.helper.admob.ads.natives.NativeTemplate;
import com.mct.app.helper.admob.ads.natives.NativeTemplateStyle;

public class NativeFullScreenAdsConfigurator extends BaseAdsConfigurator<NativeFullScreenAdsConfigurator, NativeFullScreenAds> {

    private int layout;
    private NativeTemplateStyle templateStyle;

    public NativeFullScreenAdsConfigurator(AdsConfigurator configurator, String adsUnitId) {
        super(configurator, adsUnitId);
    }

    public NativeFullScreenAdsConfigurator layout(@LayoutRes int layoutRes) {
        this.layout = layoutRes;
        return this;
    }

    public NativeFullScreenAdsConfigurator template(@NonNull NativeTemplate template) {
        this.layout = template.layoutRes;
        return this;
    }

    public NativeFullScreenAdsConfigurator style(NativeTemplateStyle templateStyle) {
        this.templateStyle = templateStyle;
        return this;
    }

    @Override
    protected NativeFullScreenAds makeAds(String adsUnitId, long adsInterval) {
        NativeFullScreenAds ads = new NativeFullScreenAds(adsUnitId, layout);
        ads.setTemplateStyle(templateStyle);
        return ads;
    }
}
