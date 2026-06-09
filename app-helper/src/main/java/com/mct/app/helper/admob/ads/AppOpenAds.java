package com.mct.app.helper.admob.ads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.mct.app.helper.admob.listener.AdCallback;

public class AppOpenAds extends BaseFullScreenAds<AppOpenAd> {

    public AppOpenAds(String adsUnitId, long adsInterval) {
        super(adsUnitId, adsInterval);
    }

    @Override
    protected void onLoad(@NonNull Context context, @NonNull LoadSession session) {
        AppOpenAd.load(
                context,
                getLoadAdsUnitId(),
                getAdRequest(),
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd openAd) {
                        openAd.setOnPaidEventListener(AppOpenAds.this::onPaidEvent);
                        session.loaded(openAd);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        session.failed(loadAdError);
                    }
                });
    }

    @Override
    protected void onShowAds(@NonNull Activity activity, @NonNull AppOpenAd appOpenAd, @NonNull FullScreenContentCallback callback, @Nullable AdCallback adCallback) {
        appOpenAd.setFullScreenContentCallback(callback);
        appOpenAd.show(activity);
    }

    @Override
    protected void onRelease(@Nullable AppOpenAd ad) {
        // app open ads have no destroy() API; the reference is dropped by the base.
    }

    @Override
    protected boolean allowAdsInterval() {
        return true;
    }

    @Override
    public AdRequest getAdRequest() {
        return new AdRequest.Builder().setHttpTimeoutMillis(5000).build();
    }
}
