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

    public enum AdaptiveSize {
        COMPACT,
        LARGE
    }

    private boolean collapsible;
    private AdaptiveSize adaptiveSize = AdaptiveSize.COMPACT;

    public BannerAds(String adsUnitId) {
        super(adsUnitId);
    }

    public boolean isCollapsible() {
        return collapsible;
    }

    public void setCollapsible(boolean collapsible) {
        this.collapsible = collapsible;
    }

    public AdaptiveSize getAdaptiveSize() {
        return adaptiveSize;
    }

    public void setAdaptiveSize(@NonNull AdaptiveSize adaptiveSize) {
        this.adaptiveSize = adaptiveSize;
    }

    @Override
    protected void onLoad(@NonNull Context context, @NonNull LoadSession session) {
        AdView adView = new AdView(context);
        adView.setAdUnitId(getLoadAdsUnitId());
        adView.setAdSize(getAdSize(context));
        // listener MUST be attached before loadAd() so no callback can ever be missed
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdClicked() {
                BannerAds.this.onAdClicked();
            }

            @Override
            public void onAdLoaded() {
                adView.setOnPaidEventListener(BannerAds.this::onPaidEvent);
                session.loaded(adView);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                session.failed(loadAdError);
            }
        });
        adView.loadAd(getAdRequest());
    }

    @Override
    protected void onDestroyView(@NonNull AdView view) {
        view.destroy();
    }

    @Override
    protected void onPauseView(@NonNull AdView view) {
        view.pause();
    }

    @Override
    protected void onResumeView(@NonNull AdView view) {
        view.resume();
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
    @SuppressWarnings("deprecation")
    private AdSize getAdSize(@NonNull Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float widthPixels = displayMetrics.widthPixels;
        float density = displayMetrics.density;
        int adWidth = (int) (widthPixels / density);
        if (adaptiveSize == AdaptiveSize.LARGE) {
            return AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, adWidth);
        }
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth);
    }
}
