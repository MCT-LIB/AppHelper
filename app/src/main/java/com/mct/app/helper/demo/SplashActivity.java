package com.mct.app.helper.demo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.MaterialColors;
import com.mct.app.helper.admob.AdsManager;
import com.mct.app.helper.admob.OnAdsLoadListener;
import com.mct.app.helper.admob.ads.BaseAds;
import com.mct.app.helper.admob.utils.SplashUtils;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(MaterialColors.getColor(this, android.R.attr.colorBackground, 0));
        setContentView(R.layout.activity_splash);

        AdsManager.getInstance().init(this, adsConfigurator -> adsConfigurator
                .premium(false)
                .debug(BuildConfig.DEBUG)
                .onAdsLoadListener(new OnAdsLoadListener() {
                    @Override
                    public void onAdsFailedToLoad(BaseAds<?> ads, int code, String message, String domain) {
                        Log.e("ddd", "onAdsFailedToLoad: " + ads.getClass().getSimpleName() + "-" + ads.getAlias() + " code: " + code + " message: " + message);
                    }

                    @Override
                    public void onAdsLoaded(BaseAds<?> ads) {
                        Log.d("ddd", "onAdsLoaded: " + ads.getClass().getSimpleName() + "-" + ads.getAlias());
                    }
                })
                .onPaidEventListener(null)
                .autoCheckDeviceWhenHasInternet(Constant.NATIVE_ID)
                .autoLoadFullscreenAdsWhenHasInternet(true)
                .autoReloadFullscreenAdsWhenOrientationChanged(true)
                .appOpenObserverBlackListActivity(SplashActivity.class)
                .appOpenAds(Constant.APP_OPEN_ID).adsInterval(5000).and()
                .bannerAds(Constant.BANNER_ID).and()
                .bannerAds(Constant.BANNER_COLLAPSE_ID).collapsible(true).and()
                .interstitialAds(Constant.INTERSTITIAL_ID).adsInterval(5000).and()
                .nativeAds(Constant.NATIVE_ID).and()
                .rewardedAds(Constant.REWARDED_ID).and()
                .rewardedInterstitialAds(Constant.REWARDED_INTERSTITIAL_ID).and()
                .apply());

        SplashUtils.with(this, Constant.APP_OPEN_ID, Constant.INTERSTITIAL_ID)
                .setGoToNextScreen(() -> {
                    if (isDestroyed() || isFinishing()) {
                        return;
                    }
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                    finish();
                })
                .start();
    }

}
