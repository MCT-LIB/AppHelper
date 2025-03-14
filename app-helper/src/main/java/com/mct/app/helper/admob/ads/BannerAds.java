package com.mct.app.helper.admob.ads;

import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

import java.util.UUID;

public class BannerAds extends BaseViewAds<AdView> {

    private boolean collapsible;

    public BannerAds(String adsUnitId) {
        super(adsUnitId);
    }

    public boolean isCollapsible() {
        return collapsible;
    }

    public void setCollapsible(boolean collapsible) {
        this.collapsible = collapsible;
    }

    @Override
    protected void onLoadAds(@NonNull Context context, @NonNull AdsLoadCallback<AdView> callback) {
        AdView adView = new AdView(context);
        adView.setAdUnitId(getLoadAdsUnitId());
        adView.setAdSize(getAdSize(context));
        adView.loadAd(getAdRequest());
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdClicked() {
                BannerAds.this.onAdClicked();
            }

            @Override
            public void onAdLoaded() {
                adView.setOnPaidEventListener(BannerAds.this::onPaidEvent);
                callback.onAdsLoaded(adView);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                callback.onAdsFailedToLoad(loadAdError);
            }
        });
    }

    @Override
    public AdRequest getAdRequest() {
        if (collapsible) {
            Bundle extras = new Bundle();
            extras.putString("collapsible", "bottom");
            extras.putString("collapsible_request_id", UUID.randomUUID().toString());
            return new AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                    .build();
        }
        return super.getAdRequest();
    }

    @NonNull
    private AdSize getAdSize(@NonNull Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float widthPixels = displayMetrics.widthPixels;
        float density = displayMetrics.density;
        int adWidth = (int) (widthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth);
    }
}
