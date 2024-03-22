package com.mct.app.helper.admob.ads;

import android.content.Context;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoadCallback;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.mct.app.helper.admob.ads.natives.NativeTemplate;
import com.mct.app.helper.admob.ads.natives.NativeTemplateStyle;
import com.mct.app.helper.admob.ads.natives.NativeTemplateView;

public class NativeAds extends BaseViewAds<NativeTemplateView> {

    private final int layoutRes;
    private NativeTemplateView templateView;
    private NativeTemplateStyle templateStyle;

    public NativeAds(String adsUnitId) {
        this(adsUnitId, NativeTemplate.MEDIUM);
    }

    public NativeAds(String adsUnitId, @NonNull NativeTemplate nativeTemplate) {
        super(adsUnitId);
        this.layoutRes = nativeTemplate.layoutRes;
    }

    public NativeAds(String adsUnitId, @LayoutRes int layoutRes) {
        super(adsUnitId);
        this.layoutRes = layoutRes;
    }

    public void setTemplateStyle(NativeTemplateStyle templateStyle) {
        this.templateStyle = templateStyle;
    }

    @Override
    protected void onLoadAds(@NonNull Context context, @NonNull AdLoadCallback<NativeTemplateView> callback) {
        new AdLoader.Builder(context, getLoadAdsUnitId())
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .setVideoOptions(new VideoOptions.Builder().setStartMuted(true).build())
                        .build())
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        callback.onAdFailedToLoad(loadAdError);
                    }
                })
                .forNativeAd(nativeAd -> {
                    templateView = new NativeTemplateView(context, layoutRes);
                    templateView.setStyles(templateStyle);
                    templateView.setNativeAd(nativeAd);
                    nativeAd.setOnPaidEventListener(getOnPaidEventListener());
                    callback.onAdLoaded(templateView);
                })
                .build()
                .loadAd(getAdRequest());
    }

}
