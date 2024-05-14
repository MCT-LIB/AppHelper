package com.mct.app.helper.admob.ads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdLoadCallback;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

public class AppOpenAds extends BaseFullScreenAds<AppOpenAd> {

    public AppOpenAds(String adsUnitId, long adsInterval) {
        super(adsUnitId, adsInterval);
    }

    @Override
    protected void onLoadAds(@NonNull Context context, @NonNull AdLoadCallback<AppOpenAd> callback) {
        AppOpenAd.load(
                context,
                getLoadAdsUnitId(),
                getAdRequest(),
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd openAd) {
                        openAd.setOnPaidEventListener(getOnPaidEventListener());
                        callback.onAdLoaded(openAd);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        callback.onAdFailedToLoad(loadAdError);
                    }
                });
    }

    @Override
    protected void onShowAds(@NonNull Activity activity, @NonNull AppOpenAd appOpenAd, @NonNull FullScreenContentCallback callback) {
        appOpenAd.setFullScreenContentCallback(callback);
        appOpenAd.show(activity);
    }

    @Override
    public AdRequest getAdRequest() {
        return new AdRequest.Builder().setHttpTimeoutMillis(5000).build();
    }
}
