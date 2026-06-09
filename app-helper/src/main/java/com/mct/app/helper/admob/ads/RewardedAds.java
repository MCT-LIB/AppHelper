package com.mct.app.helper.admob.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.mct.app.helper.admob.listener.AdCallback;
import com.mct.app.helper.admob.listener.RewardItem;
import com.mct.app.helper.admob.listener.RewardedAdCallback;

public class RewardedAds extends BaseFullScreenAds<RewardedAd> {

    public RewardedAds(String adsUnitId, long adsInterval) {
        super(adsUnitId, adsInterval);
    }

    @Override
    protected void onLoad(@NonNull Context context, @NonNull LoadSession session) {
        RewardedAd.load(
                context,
                getLoadAdsUnitId(),
                getAdRequest(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        rewardedAd.setOnPaidEventListener(RewardedAds.this::onPaidEvent);
                        session.loaded(rewardedAd);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        session.failed(loadAdError);
                    }
                });
    }

    @Override
    protected void onShowAds(@NonNull Activity activity, @NonNull RewardedAd rewardedAd,
                             @NonNull FullScreenContentCallback callback,
                             @Nullable AdCallback adCallback) {
        rewardedAd.setFullScreenContentCallback(callback);
        rewardedAd.show(activity, rewardItem -> {
            Log.d(TAG, "onRewarded: " + rewardItem);
            if (adCallback instanceof RewardedAdCallback) {
                ((RewardedAdCallback) adCallback).onUserEarnedReward(RewardItem.of(rewardItem));
            }
        });
    }

    @Override
    protected void onRelease(@Nullable RewardedAd ad) {
        // rewarded ads have no destroy() API; the reference is dropped by the base.
    }
}
