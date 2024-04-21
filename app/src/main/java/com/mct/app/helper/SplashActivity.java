package com.mct.app.helper;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.MaterialColors;
import com.mct.app.helper.admob.AdsManager;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean isGoToMainActivity = new AtomicBoolean(false);
    private long startTime;

    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(MaterialColors.getColor(this, android.R.attr.colorBackground, 0));
        setContentView(R.layout.activity_splash);
        startTime = System.currentTimeMillis();

        AdsManager.getInstance().init(this, adsConfigurator -> adsConfigurator
                .premium(false)
                .debug(BuildConfig.DEBUG)
                .onPaidEventListener(null)
                .appOpenObserverBlackListActivity(SplashActivity.class)
                .appOpenAds(Constant.APP_OPEN_ID).and()
                .bannerAds(Constant.BANNER_ID).and()
                .bannerAds(Constant.BANNER_COLLAPSE_ID).collapsible(true).and()
                .interstitialAds(Constant.INTERSTITIAL_ID).and()
                .nativeAds(Constant.NATIVE_ID).and()
                .rewardedAds(Constant.REWARDED_ID).and()
                .rewardedInterstitialAds(Constant.REWARDED_INTERSTITIAL_ID).and()
                .apply());

        AdsManager.getInstance().load(Constant.INTERSTITIAL_ID, this, null, null);
        AdsManager.getInstance().load(Constant.NATIVE_ID, this, null, null);
        AdsManager.getInstance().showSyncLoad(Constant.APP_OPEN_ID, this, this::gotoMain);
    }

    private void gotoMain() {
        if (isGoToMainActivity.getAndSet(true)) {
            return;
        }
        final long INIT_DURATION = 2000;
        final long MIN_INIT_DURATION = 800;
        long duration = System.currentTimeMillis() - startTime;
        long delay = duration < INIT_DURATION ? INIT_DURATION - duration : 0;
        handler.postDelayed(() -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            finish();
        }, Math.max(MIN_INIT_DURATION, delay));
    }

}
