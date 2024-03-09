package com.mct.app.helper.admob.ads;

import android.content.Context;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoadCallback;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

public class BannerAds extends BaseViewAds<AdView> {

    public BannerAds(String adsUnitId) {
        super(adsUnitId);
    }

    @Override
    protected void onLoadAds(@NonNull Context context, @NonNull AdLoadCallback<AdView> callback) {
        AdView adView = new AdView(context);
        adView.setAdUnitId(getLoadAdsUnitId());
        adView.setAdSize(getAdSize(context));
        adView.loadAd(getAdRequest());
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                callback.onAdLoaded(adView);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                callback.onAdFailedToLoad(loadAdError);
            }
        });
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
