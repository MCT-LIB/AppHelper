package com.mct.app.helper.admob.ads;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.FullScreenContentCallback;
import com.mct.app.helper.admob.Callback;

public abstract class BaseRewardedAds<Ads> extends BaseFullScreenAds<Ads> {

    public BaseRewardedAds(String adsUnitId, long adsInterval) {
        super(adsUnitId, adsInterval);
    }

    protected abstract void onShowAds(@NonNull Activity activity, @NonNull Ads ads,
                                      @NonNull FullScreenContentCallback callback,
                                      @Nullable Callback onUserEarnedReward);

    public final void show(@NonNull Activity activity, Callback callback, Callback onUserEarnedReward) {
        if (isCanLoadAds()) {
            load(activity.getApplicationContext(),
                    () -> show(activity, callback, onUserEarnedReward),
                    () -> invokeCallback(callback)
            );
            return;
        }
        if (isCanShowAds() && validateActivityToShow(activity)) {
            setShowing(true);
            onShowAds(activity, getAds(), new FullScreenContentCallbackImpl(this, activity.getApplicationContext(), callback), onUserEarnedReward);
        } else {
            invokeCallback(callback);
        }
    }

    @Override
    protected void onShowAds(@NonNull Activity activity, @NonNull Ads ads, @NonNull FullScreenContentCallback callback) {
        onShowAds(activity, ads, callback, null);
    }

}
