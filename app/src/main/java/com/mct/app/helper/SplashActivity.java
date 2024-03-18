package com.mct.app.helper;

import android.annotation.SuppressLint;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.MaterialColors;
import com.mct.app.helper.admob.AdsManager;
import com.mct.app.helper.admob.AdsProvider;
import com.mct.app.helper.admob.ads.AppOpenAds;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(MaterialColors.getColor(this, android.R.attr.colorBackground, 0));
        setContentView(R.layout.activity_splash);

        AdsManager.getInstance().init(this, new AdsProvider.Builder()
                        .putBannerAds(Constant.BANNER_ID)
                        .putInterstitialAds(Constant.INTERSTITIAL_ID)
                        .putNativeAds(Constant.NATIVE_ID)
                        .putAppOpenAds(Constant.APP_OPEN_ID)
                        .putRewardedAds(Constant.REWARDED_ID)
                        .putRewardedInterstitialAds(Constant.REWARDED_INTERSTITIAL_ID)
                        .build(),
                null,
                () -> AdsManager.getInstance().show(Constant.APP_OPEN_ID, this, this::gotoMain));

        AppOpenAds ads = AdsManager.getInstance().getAds(Constant.APP_OPEN_ID, AppOpenAds.class);
        AdsManager.getInstance().getAppOpenObserver().init(getApplication(), ads);

    }

    private void gotoMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }


}
