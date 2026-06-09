package com.mct.app.helper.admob.ads;

import android.content.Context;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.mct.app.helper.admob.ads.natives.NativeTemplateStyle;
import com.mct.app.helper.admob.ads.natives.NativeTemplateView;

public class NativeAds extends BaseViewAds<NativeTemplateView> {

    private int layoutRes;
    private NativeTemplateStyle templateStyle;

    public NativeAds(String adsUnitId) {
        super(adsUnitId);
    }

    public void setLayoutRes(@LayoutRes int layoutRes) {
        this.layoutRes = layoutRes;
    }

    public void setTemplateStyle(NativeTemplateStyle templateStyle) {
        this.templateStyle = templateStyle;
    }

    public void setTemplateStyleAndApply(NativeTemplateStyle templateStyle) {
        this.templateStyle = templateStyle;
        if (getAds() != null) {
            getAds().setStyles(templateStyle);
        }
    }

    @Override
    protected void onLoad(@NonNull Context context, @NonNull LoadSession session) {
        new AdLoader.Builder(context, getLoadAdsUnitId())
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .setVideoOptions(new VideoOptions.Builder().setStartMuted(true).build())
                        .build())
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdClicked() {
                        NativeAds.this.onAdClicked();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        session.failed(loadAdError);
                    }
                })
                .forNativeAd(nativeAd -> {
                    nativeAd.setOnPaidEventListener(NativeAds.this::onPaidEvent);
                    NativeTemplateView templateView = new NativeTemplateView(context, layoutRes);
                    templateView.setStyles(templateStyle);
                    templateView.setNativeAd(nativeAd);
                    session.loaded(templateView);
                })
                .build()
                .loadAd(getAdRequest());
    }

    @Override
    protected void onDestroyView(@NonNull NativeTemplateView view) {
        view.destroyNativeAd();
    }
}
