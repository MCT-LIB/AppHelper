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
import com.mct.app.helper.admob.Callback;

public class RewardedInterstitialAds extends BaseRewardedAds<RewardedInterstitialAd> {

    public RewardedInterstitialAds(String adsUnitId, long adsInterval) {
        super(adsUnitId, adsInterval);
    }

    @Override
    protected void onLoadAds(@NonNull Context context, @NonNull AdsLoadCallback<RewardedInterstitialAd> callback) {
        RewardedInterstitialAd.load(
                context,
                getLoadAdsUnitId(),
                getAdRequest(),
                new RewardedInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedInterstitialAd rewardedInterstitialAd) {
                        rewardedInterstitialAd.setOnPaidEventListener(RewardedInterstitialAds.this::onPaidEvent);
                        callback.onAdsLoaded(rewardedInterstitialAd);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        callback.onAdsFailedToLoad(loadAdError);
                    }
                });
    }

    @Override
    protected void onShowAds(@NonNull Activity activity, @NonNull RewardedInterstitialAd rewardedInterstitialAd,
                             @NonNull FullScreenContentCallback callback,
                             @Nullable Callback onUserEarnedReward) {
        rewardedInterstitialAd.setFullScreenContentCallback(callback);
        rewardedInterstitialAd.show(activity, rewardItem -> {
            Log.d(TAG, "onRewarded: " + rewardItem);
            invokeCallback(onUserEarnedReward);
        });
    }
}
