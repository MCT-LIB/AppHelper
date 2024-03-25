package com.mct.app.helper.admob;

import android.util.Log;

import androidx.annotation.NonNull;

import com.mct.app.helper.admob.ads.AppOpenAds;
import com.mct.app.helper.admob.ads.BaseAds;
import com.mct.app.helper.admob.ads.BaseFullScreenAds;
import com.mct.app.helper.admob.ads.InterstitialAds;
import com.mct.app.helper.admob.ads.natives.NativeTemplate;
import com.mct.app.helper.admob.configurator.AppOpenAdsConfigurator;
import com.mct.app.helper.admob.configurator.BannerAdsConfigurator;
import com.mct.app.helper.admob.configurator.InterstitialAdsConfigurator;
import com.mct.app.helper.admob.configurator.NativeAdsConfigurator;
import com.mct.app.helper.admob.configurator.RewardedAdsConfigurator;
import com.mct.app.helper.admob.configurator.RewardedInterstitialAdsConfigurator;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdsConfigurator {

    private static final String TAG = "AdsConfigurator";
    private static final long APP_OPEN_ADS_INTERVAL = 60 * 1000;
    private static final long INTERSTITIAL_ADS_INTERVAL = 30 * 1000;

    private final AdsManager mAdsManager;
    private final Callback mCallback;
    private final Map<String, BaseAds<?>> mAds;

    private boolean mDebug;
    private OnPaidEventListener mOnPaidEventListener;

    AdsConfigurator(AdsManager adsManager) {
        this(adsManager, null);
    }

    AdsConfigurator(AdsManager adsManager, Callback callback) {
        this.mAdsManager = adsManager;
        this.mCallback = callback;
        this.mAds = new LinkedHashMap<>();
    }

    public AdsConfigurator premium(boolean premium) {
        mAdsManager.setPremium(premium);
        return this;
    }

    public AdsConfigurator debug(boolean debug) {
        mDebug = debug;
        return this;
    }

    public AdsConfigurator onPaidEventListener(OnPaidEventListener onPaidEventListener) {
        mOnPaidEventListener = onPaidEventListener;
        return this;
    }

    /* --- App Open Lifecycle Observer --- */

    public AdsConfigurator appOpenObserverEnabled(boolean enabled) {
        if (enabled) {
            mAdsManager.enableAppOpenObserver();
        } else {
            mAdsManager.disableAppOpenObserver();
        }
        return this;
    }

    public AdsConfigurator appOpenObserverBlackListActivity(Class<?>... blackListActivity) {
        mAdsManager.setAppOpenObserverBlackListActivity(blackListActivity);
        return this;
    }

    /* --- Ads AdsConfigurator --- */

    public AppOpenAdsConfigurator appOpenAds(String adsUnitId) {
        return new AppOpenAdsConfigurator(this, adsUnitId).adsInterval(APP_OPEN_ADS_INTERVAL);
    }

    public BannerAdsConfigurator bannerAds(String adsUnitId) {
        return new BannerAdsConfigurator(this, adsUnitId);
    }

    public InterstitialAdsConfigurator interstitialAds(String adsUnitId) {
        return new InterstitialAdsConfigurator(this, adsUnitId).adsInterval(INTERSTITIAL_ADS_INTERVAL);
    }

    public NativeAdsConfigurator nativeAds(String adsUnitId) {
        return new NativeAdsConfigurator(this, adsUnitId).template(NativeTemplate.MEDIUM);
    }

    public RewardedAdsConfigurator rewardedAds(String adsUnitId) {
        return new RewardedAdsConfigurator(this, adsUnitId);
    }

    public RewardedInterstitialAdsConfigurator rewardedInterstitialAds(String adsUnitId) {
        return new RewardedInterstitialAdsConfigurator(this, adsUnitId);
    }

    public AdsConfigurator putAds(@NonNull String alias, @NonNull BaseAds<?> ads) {
        mAds.put(alias, ads);
        return this;
    }

    public void apply() {
        final boolean debug = mDebug;
        final OnPaidEventListener paidEventListener = mOnPaidEventListener;
        final Map<String, BaseAds<?>> adsMap = new HashMap<>(mAds);
        for (Map.Entry<String, BaseAds<?>> adsEntry : adsMap.entrySet()) {
            String alias = adsEntry.getKey();
            BaseAds<?> ads = adsEntry.getValue();

            ads.setDebugMode(debug);
            if (paidEventListener != null) {
                ads.setOnPaidEventListener(adValue -> paidEventListener.onPaidEvent(AdsValue.of(adValue)));
            }
            if (isInterstitialOrOpenAd(ads)) {
                ((BaseFullScreenAds<?>) ads).setOnDismissListener(a -> mAdsManager.getAdsList().stream()
                        .filter(AdsConfigurator::isInterstitialOrOpenAd)
                        .forEach(BaseAds::postDelayShowFlag)
                );
            }
            if (ads instanceof AppOpenAds) {
                mAdsManager.setAppOpenObserverAds((AppOpenAds) ads);
            }
            if (!mAdsManager.putAds(alias, ads)) {
                Log.e(TAG, "Failed to put ads: " + ads + " with alias: " + alias + " already exists");
            }
        }
        if (mCallback != null) {
            mCallback.callback();
        }
    }

    private static boolean isInterstitialOrOpenAd(BaseAds<?> ads) {
        return ads instanceof InterstitialAds || ads instanceof AppOpenAds;
    }
}
