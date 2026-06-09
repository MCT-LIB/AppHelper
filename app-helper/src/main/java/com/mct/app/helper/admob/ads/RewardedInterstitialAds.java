package com.mct.app.helper.admob.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.mct.app.helper.admob.listener.AdCallback;
import com.mct.app.helper.admob.listener.RewardItem;
import com.mct.app.helper.admob.listener.RewardedAdCallback;

public class RewardedInterstitialAds extends BaseFullScreenAds<RewardedInterstitialAd> {

    public RewardedInterstitialAds(String adsUnitId, long adsInterval) {
        super(adsUnitId, adsInterval);
    }

    @Override
    protected void onLoad(@NonNull Context context, @NonNull LoadSession session) {
        RewardedInterstitialAd.load(
                context,
                getLoadAdsUnitId(),
                getAdRequest(),
                new RewardedInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedInterstitialAd rewardedInterstitialAd) {
                        rewardedInterstitialAd.setOnPaidEventListener(RewardedInterstitialAds.this::onPaidEvent);
                        session.loaded(rewardedInterstitialAd);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        session.failed(loadAdError);
                    }
                });
    }

    @Override
    protected void onShowAds(@NonNull Activity activity, @NonNull RewardedInterstitialAd rewardedInterstitialAd,
                             @NonNull FullScreenContentCallback callback,
                             @Nullable AdCallback adCallback) {
        rewardedInterstitialAd.setFullScreenContentCallback(callback);
        rewardedInterstitialAd.show(activity, rewardItem -> {
            Log.d(TAG, "onRewarded: " + rewardItem);
            if (adCallback instanceof RewardedAdCallback) {
                ((RewardedAdCallback) adCallback).onUserEarnedReward(RewardItem.of(rewardItem));
            }
        });
    }

    @Override
    protected void onRelease(@Nullable RewardedInterstitialAd ad) {
        // rewarded interstitial ads have no destroy() API; the reference is dropped by the base.
    }
}
