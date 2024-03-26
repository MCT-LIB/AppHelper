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

import java.util.LinkedHashMap;
import java.util.Map;

public class AdsConfigurator {

    private static final String TAG = "AdsConfigurator";
    private static final long APP_OPEN_ADS_INTERVAL = 60 * 1000;
    private static final long INTERSTITIAL_ADS_INTERVAL = 30 * 1000;

    private final AdsManager mAdsManager;
    private final Callback mCallback;
    private final Map<String, BaseAds<?>> mAds;

    private Boolean mPremium;
    private Boolean mDebug;
    private Boolean mAppOpenObserverEnabled;
    private Class<?>[] mAppOpenObserverBlackListActivity;
    private OnPaidEventListener mOnPaidEventListener;

    AdsConfigurator(AdsManager adsManager) {
        this(adsManager, null);
    }

    AdsConfigurator(AdsManager adsManager, Callback callback) {
        this.mAdsManager = adsManager;
        this.mCallback = callback;
        this.mAds = new LinkedHashMap<>();
    }

    /**
     * Sets whether the app is premium or not
     *
     * @param premium True if the app is premium, false otherwise
     * @return This AdsConfigurator instance for method chaining
     */
    public AdsConfigurator premium(boolean premium) {
        mPremium = premium;
        return this;
    }

    /**
     * Sets whether the app is in debug mode
     *
     * @param debug True if the app is in debug mode, false otherwise
     * @return This AdsConfigurator instance for method chaining
     */
    public AdsConfigurator debug(boolean debug) {
        mDebug = debug;
        return this;
    }

    /**
     * Sets the listener for paid events
     *
     * @param onPaidEventListener The listener for paid events
     * @return This AdsConfigurator instance for method chaining
     */
    public AdsConfigurator onPaidEventListener(OnPaidEventListener onPaidEventListener) {
        mOnPaidEventListener = onPaidEventListener;
        return this;
    }

    /**
     * Enables or disables the app open lifecycle observer
     *
     * @param enabled True to enable the observer, false to disable it
     * @return This AdsConfigurator instance for method chaining
     */
    public AdsConfigurator appOpenObserverEnabled(boolean enabled) {
        mAppOpenObserverEnabled = enabled;
        return this;
    }

    /**
     * Sets a list of activities to be blacklisted from the app open lifecycle observer
     *
     * @param blackListActivity The list of activities to be blacklisted
     * @return This AdsConfigurator instance for method chaining
     */
    public AdsConfigurator appOpenObserverBlackListActivity(Class<?>... blackListActivity) {
        mAppOpenObserverBlackListActivity = blackListActivity;
        return this;
    }

    /**
     * Configures settings for app open ads
     *
     * @param adsUnitId The unit ID for app open ads
     * @return An AppOpenAdsConfigurator instance for further configuration
     */
    public AppOpenAdsConfigurator appOpenAds(String adsUnitId) {
        return new AppOpenAdsConfigurator(this, adsUnitId).adsInterval(APP_OPEN_ADS_INTERVAL);
    }

    /**
     * Configures settings for banner ads
     *
     * @param adsUnitId The unit ID for banner ads
     * @return A BannerAdsConfigurator instance for further configuration
     */
    public BannerAdsConfigurator bannerAds(String adsUnitId) {
        return new BannerAdsConfigurator(this, adsUnitId);
    }

    /**
     * Configures settings for interstitial ads
     *
     * @param adsUnitId The unit ID for interstitial ads
     * @return An InterstitialAdsConfigurator instance for further configuration
     */
    public InterstitialAdsConfigurator interstitialAds(String adsUnitId) {
        return new InterstitialAdsConfigurator(this, adsUnitId).adsInterval(INTERSTITIAL_ADS_INTERVAL);
    }

    /**
     * Configures settings for native ads
     *
     * @param adsUnitId The unit ID for native ads
     * @return A NativeAdsConfigurator instance for further configuration
     */
    public NativeAdsConfigurator nativeAds(String adsUnitId) {
        return new NativeAdsConfigurator(this, adsUnitId).template(NativeTemplate.MEDIUM);
    }

    /**
     * Configures settings for rewarded ads
     *
     * @param adsUnitId The unit ID for rewarded ads
     * @return A RewardedAdsConfigurator instance for further configuration
     */
    public RewardedAdsConfigurator rewardedAds(String adsUnitId) {
        return new RewardedAdsConfigurator(this, adsUnitId);
    }

    /**
     * Configures settings for rewarded interstitial ads
     *
     * @param adsUnitId The unit ID for rewarded interstitial ads
     * @return A RewardedInterstitialAdsConfigurator instance for further configuration
     */
    public RewardedInterstitialAdsConfigurator rewardedInterstitialAds(String adsUnitId) {
        return new RewardedInterstitialAdsConfigurator(this, adsUnitId);
    }

    /**
     * Associates an alias with a specific type of ads
     *
     * @param alias The alias for the ads
     * @param ads   The ads instance
     * @return This AdsConfigurator instance for method chaining
     */
    public AdsConfigurator putAds(@NonNull String alias, @NonNull BaseAds<?> ads) {
        mAds.put(alias, ads);
        return this;
    }

    /**
     * Applies the configuration to the AdsManager
     */
    public void apply() {
        if (mPremium != null) {
            mAdsManager.setPremium(mPremium);
        }
        if (mAppOpenObserverEnabled != null) {
            mAdsManager.setAppOpenObserverEnabled(mAppOpenObserverEnabled);
        }
        if (mAppOpenObserverBlackListActivity != null) {
            mAdsManager.setAppOpenObserverBlackListActivity(mAppOpenObserverBlackListActivity);
        }
        final OnPaidEventListener paidEventListener = mOnPaidEventListener;
        for (Map.Entry<String, BaseAds<?>> adsEntry : mAds.entrySet()) {
            String alias = adsEntry.getKey();
            BaseAds<?> ads = adsEntry.getValue();
            if (mDebug != null) {
                ads.setDebugMode(mDebug);
            }
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
