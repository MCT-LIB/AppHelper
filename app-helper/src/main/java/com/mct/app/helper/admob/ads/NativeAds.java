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

import java.util.Optional;

public class NativeAds extends BaseViewAds<NativeTemplateView> {

    private final int layoutRes;
    private NativeTemplateStyle templateStyle;

    public NativeAds(String adsUnitId, @LayoutRes int layoutRes) {
        super(adsUnitId);
        this.layoutRes = layoutRes;
    }

    public void setTemplateStyle(NativeTemplateStyle templateStyle) {
        this.templateStyle = templateStyle;
    }

    public void applyStyle() {
        Optional.ofNullable(getAds()).ifPresent(ads -> ads.setStyles(templateStyle));
    }

    public void destroy() {
        Optional.ofNullable(getAds()).ifPresent(NativeTemplateView::destroyNativeAd);
    }

    @Override
    protected void onLoadAds(@NonNull Context context, @NonNull AdsLoadCallback<NativeTemplateView> callback) {
        new AdLoader.Builder(context, getLoadAdsUnitId())
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .setVideoOptions(new VideoOptions.Builder().setStartMuted(true).build())
                        .build())
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        callback.onAdsFailedToLoad(loadAdError);
                    }
                })
                .forNativeAd(nativeAd -> {
                    nativeAd.setOnPaidEventListener(getOnPaidEventListener());
                    NativeTemplateView templateView = new NativeTemplateView(context, layoutRes);
                    templateView.setStyles(templateStyle);
                    templateView.setNativeAd(nativeAd);
                    callback.onAdsLoaded(templateView);
                })
                .build()
                .loadAd(getAdRequest());
    }
}
