package com.mct.app.helper.admob.configurator;

import static com.mct.app.helper.admob.ads.NativeFullScreenAds.DismissButtonGravity;
import static com.mct.app.helper.admob.ads.NativeFullScreenAds.MediaRatioOptions;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.mct.app.helper.admob.AdsConfigurator;
import com.mct.app.helper.admob.ads.NativeFullScreenAds;
import com.mct.app.helper.admob.ads.natives.NativeTemplate;
import com.mct.app.helper.admob.ads.natives.NativeTemplateStyle;

public class NativeFullScreenAdsConfigurator extends BaseAdsConfigurator<NativeFullScreenAdsConfigurator, NativeFullScreenAds> {

    private int layout;
    private NativeTemplateStyle templateStyle;

    private int dismissButtonGravity = DismissButtonGravity.GRAVITY_TOP_END;
    private long showDismissButtonCountdown = 3000;
    private long clickableDismissButtonCountdown = 0;
    private boolean cancelable = false;
    private boolean startMuted = true;
    private int mediaRatioOptions = MediaRatioOptions.MEDIA_RATIO_ANY;

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

    public NativeFullScreenAdsConfigurator dismissButtonGravity(@DismissButtonGravity int gravity) {
        this.dismissButtonGravity = gravity;
        return this;
    }

    public NativeFullScreenAdsConfigurator showDismissButtonCountdown(long countdown) {
        this.showDismissButtonCountdown = countdown;
        return this;
    }

    public NativeFullScreenAdsConfigurator clickableDismissButtonCountdown(long countdown) {
        this.clickableDismissButtonCountdown = countdown;
        return this;
    }

    public NativeFullScreenAdsConfigurator cancelable(boolean cancelable) {
        this.cancelable = cancelable;
        return this;
    }

    public NativeFullScreenAdsConfigurator startMuted(boolean startMuted) {
        this.startMuted = startMuted;
        return this;
    }

    public NativeFullScreenAdsConfigurator mediaRatioOptions(@MediaRatioOptions int mediaRatioOptions) {
        this.mediaRatioOptions = mediaRatioOptions;
        return this;
    }

    @Override
    protected NativeFullScreenAds makeAds(String adsUnitId, long adsInterval) {
        NativeFullScreenAds ads = new NativeFullScreenAds(adsUnitId, adsInterval, layout);
        ads.setTemplateStyle(templateStyle);
        ads.setDismissButtonGravity(dismissButtonGravity);
        ads.setShowDismissButtonCountdown(showDismissButtonCountdown);
        ads.setClickableDismissButtonCountdown(clickableDismissButtonCountdown);
        ads.setCancelable(cancelable);
        ads.setStartMuted(startMuted);
        ads.setMediaRatioOptions(mediaRatioOptions);
        return ads;
    }
}
