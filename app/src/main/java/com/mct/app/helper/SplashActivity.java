package com.mct.app.helper;

import android.annotation.SuppressLint;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.MaterialColors;
import com.mct.app.helper.admob.AdsManager;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(MaterialColors.getColor(this, android.R.attr.colorBackground, 0));
        setContentView(R.layout.activity_splash);

        AdsManager.getInstance().init(this)
                .premium(false)
                .debug(BuildConfig.DEBUG)
                .onPaidEventListener(null)
                .appOpenObserverBlackListActivity(SplashActivity.class)
                .bannerAds(Constant.BANNER_ID).and()
                .interstitialAds(Constant.INTERSTITIAL_ID).and()
                .nativeAds(Constant.NATIVE_ID).and()
                .appOpenAds(Constant.APP_OPEN_ID).and()
                .rewardedAds(Constant.REWARDED_ID).and()
                .rewardedInterstitialAds(Constant.REWARDED_INTERSTITIAL_ID).and()
                .apply();

        AdsManager.getInstance().load(Constant.INTERSTITIAL_ID, this, null, null);
        AdsManager.getInstance().load(Constant.NATIVE_ID, this, null, null);
        AdsManager.getInstance().show(Constant.APP_OPEN_ID, this, this::gotoMain);
    }

    private void gotoMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

}
