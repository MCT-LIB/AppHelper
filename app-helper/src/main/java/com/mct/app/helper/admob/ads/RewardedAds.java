package com.mct.app.helper.admob.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.mct.app.helper.admob.Callback;

public class RewardedAds extends BaseRewardedAds<RewardedAd> {

    public RewardedAds(String adsUnitId, long adsInterval) {
        super(adsUnitId, adsInterval);
    }

    @Override
    protected void onLoadAds(@NonNull Context context, @NonNull AdLoadCallback<RewardedAd> callback) {
        RewardedAd.load(
                context,
                getAdsUnitId(),
                getAdRequest(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        callback.onAdLoaded(rewardedAd);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        callback.onAdFailedToLoad(loadAdError);
                    }
                });
    }

    @Override
    protected void onShowAds(@NonNull Activity activity, @NonNull RewardedAd rewardedAd,
                             @NonNull FullScreenContentCallback callback,
                             @Nullable Callback onUserEarnedReward) {
        rewardedAd.setFullScreenContentCallback(callback);
        rewardedAd.show(activity, rewardItem -> {
            Log.d(TAG, "onRewarded: " + rewardItem);
            invokeCallback(onUserEarnedReward);
        });
    }

}
