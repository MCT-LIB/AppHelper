package com.mct.app.helper.admob.ads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class InterstitialAds extends BaseFullScreenAds<InterstitialAd> {

    public InterstitialAds(String adsUnitId, long adsInterval) {
        super(adsUnitId, adsInterval);
    }

    @Override
    protected void onLoadAds(@NonNull Context context, @NonNull AdLoadCallback<InterstitialAd> callback) {
        InterstitialAd.load(
                context,
                getAdsUnitId(),
                getAdRequest(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        callback.onAdLoaded(interstitialAd);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        callback.onAdFailedToLoad(loadAdError);
                    }
                });
    }

    @Override
    protected void onShowAds(@NonNull Activity activity, @NonNull InterstitialAd interstitialAd, @NonNull FullScreenContentCallback callback) {
        interstitialAd.setFullScreenContentCallback(callback);
        interstitialAd.show(activity);
    }

}
