package com.mct.app.helper.admob.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.mct.app.helper.admob.Callback;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseFullScreenAds<Ads> extends BaseAds<Ads> {

    public BaseFullScreenAds(String adsUnitId, long adsInterval) {
        super(adsUnitId, adsInterval);
    }

    protected abstract void onShowAds(@NonNull Activity activity, @NonNull Ads ads, @NonNull FullScreenContentCallback callback);

    public final void show(@NonNull Activity activity, Callback callback) {
        if (isCanLoadAds()) {
            load(activity.getApplicationContext(),
                    () -> show(activity, callback),
                    () -> invokeCallback(callback)
            );
            return;
        }
        if (isCanShowAds()) {
            setShowing(true);
            onShowAds(activity, getAds(), new FullScreenContentCallbackImpl(this, activity.getApplicationContext(), callback));
        } else {
            invokeCallback(callback);
        }
    }

    protected static class FullScreenContentCallbackImpl extends FullScreenContentCallback {

        private final AtomicBoolean dispose;
        private BaseFullScreenAds<?> ads;
        private Context context;
        private Callback callback;

        public FullScreenContentCallbackImpl(BaseFullScreenAds<?> ads, Context context, Callback callback) {
            this.dispose = new AtomicBoolean(false);
            this.ads = ads;
            this.context = context;
            this.callback = callback;
        }

        @Override
        public void onAdDismissedFullScreenContent() {
            if (isDispose()) {
                return;
            }
            Log.d(TAG, "onAdDismissedFullScreenContent");
            ads.postDelayShowFlag();
            ads.setAds(null);
            ads.setShowing(false);
            ads.load(context, null, null);
            invokeCallback(callback);
            dispose();
        }

        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
            if (isDispose()) {
                return;
            }
            Log.d(TAG, "onAdFailedToShowFullScreenContent: " + adError);
            ads.setAds(null);
            ads.setShowing(false);
            invokeCallback(callback);
            dispose();
        }

        @Override
        public void onAdShowedFullScreenContent() {
            if (isDispose()) {
                return;
            }
            Log.d(TAG, "onAdShowedFullScreenContent");
        }

        private boolean isDispose() {
            return dispose.get();
        }

        private void dispose() {
            if (dispose.getAndSet(true)) {
                return;
            }
            ads = null;
            context = null;
            callback = null;
        }
    }

}
