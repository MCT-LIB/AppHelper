package com.mct.app.helper.admob.ads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.mct.app.helper.admob.listener.AdCallback;

public class InterstitialAds extends BaseFullScreenAds<InterstitialAd> {

    public InterstitialAds(String adsUnitId, long adsInterval) {
        super(adsUnitId, adsInterval);
    }

    @Override
    protected void onLoad(@NonNull Context context, @NonNull LoadSession session) {
        InterstitialAd.load(
                context,
                getLoadAdsUnitId(),
                getAdRequest(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        interstitialAd.setOnPaidEventListener(InterstitialAds.this::onPaidEvent);
                        session.loaded(interstitialAd);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        session.failed(loadAdError);
                    }
                });
    }

    @Override
    protected void onShowAds(@NonNull Activity activity, @NonNull InterstitialAd interstitialAd, @NonNull FullScreenContentCallback callback, @Nullable AdCallback adCallback) {
        interstitialAd.setFullScreenContentCallback(callback);
        interstitialAd.show(activity);
    }

    @Override
    protected void onRelease(@Nullable InterstitialAd ad) {
        // interstitial ads have no destroy() API; the reference is dropped by the base.
    }

    @Override
    protected boolean allowAdsInterval() {
        return true;
    }

}
