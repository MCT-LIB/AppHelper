package com.mct.app.helper.demo;

import android.util.Log;

import com.mct.app.helper.admob.listener.AdCallback;
import com.mct.app.helper.admob.listener.RewardItem;
import com.mct.app.helper.admob.listener.RewardedAdCallback;
import com.mct.app.helper.admob.listener.ShowError;
import com.mct.app.helper.admob.listener.SkipReason;

/**
 * Test-only logging callbacks so each full-screen / rewarded show outcome is
 * observable in logcat under tag {@code ADSTEST} during on-device testing.
 */
final class AdLog {

    static final String TAG = "ADSTEST";

    private AdLog() {
    }

    static AdCallback fs(String name) {
        return new AdCallback() {
            @Override
            public void onShown() {
                Log.i(TAG, name + " -> onShown");
            }

            @Override
            public void onDismissed() {
                Log.i(TAG, name + " -> onDismissed");
            }

            @Override
            public void onFailedToShow(ShowError error) {
                Log.w(TAG, name + " -> onFailedToShow " + error);
            }

            @Override
            public void onSkipped(SkipReason reason) {
                Log.w(TAG, name + " -> onSkipped " + reason);
            }

            @Override
            public void onComplete(boolean wasShown) {
                Log.i(TAG, name + " -> onComplete wasShown=" + wasShown);
            }
        };
    }

    static RewardedAdCallback rewarded(String name) {
        return new RewardedAdCallback() {
            @Override
            public void onUserEarnedReward(RewardItem reward) {
                Log.i(TAG, name + " -> onUserEarnedReward " + reward);
            }

            @Override
            public void onShown() {
                Log.i(TAG, name + " -> onShown");
            }

            @Override
            public void onDismissed() {
                Log.i(TAG, name + " -> onDismissed");
            }

            @Override
            public void onFailedToShow(ShowError error) {
                Log.w(TAG, name + " -> onFailedToShow " + error);
            }

            @Override
            public void onSkipped(SkipReason reason) {
                Log.w(TAG, name + " -> onSkipped " + reason);
            }

            @Override
            public void onComplete(boolean wasShown) {
                Log.i(TAG, name + " -> onComplete wasShown=" + wasShown);
            }
        };
    }
}
