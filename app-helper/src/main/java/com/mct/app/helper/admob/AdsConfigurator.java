package com.mct.app.helper.admob;

import androidx.annotation.NonNull;

import com.mct.app.helper.admob.ads.BaseAds;
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

    private static final long APP_OPEN_ADS_INTERVAL = 60 * 1000;
    private static final long INTERSTITIAL_ADS_INTERVAL = 30 * 1000;

    private final @NonNull AdsManager mAdsManager;
    private final @NonNull Callback mCallback;
    private final @NonNull Map<String, BaseAds<?>> mAds;

    private Boolean mPremium;
    private Boolean mDebug;
    private Boolean mAutoLoadFullscreenAdsWhenHasInternet;
    private Boolean mAutoReloadFullscreenAdsWhenOrientationChanged;
    private Boolean mAppOpenObserverEnabled;
    private Class<?>[] mAppOpenObserverBlackListActivity;
    private OnPaidEventListeners mOnPaidEventListener;

    AdsConfigurator(@NonNull AdsManager adsManager) {
        this(adsManager, () -> {
        });
    }

    AdsConfigurator(@NonNull AdsManager adsManager, @NonNull Callback callback) {
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
     * @param listener The listener for paid events
     * @return This AdsConfigurator instance for method chaining
     */
    public AdsConfigurator onPaidEventListener(OnPaidEventListeners listener) {
        mOnPaidEventListener = listener;
        return this;
    }

    /**
     * Sets whether to automatically load ads when the device has internet
     *
     * @param enable true if you want to load ads automatically
     * @return This AdsConfigurator instance for method chaining
     */
    public AdsConfigurator autoLoadWhenHasInternet(boolean enable) {
        mAutoLoadFullscreenAdsWhenHasInternet = enable;
        return this;
    }

    /**
     * Sets whether to automatically reload fullscreen ads when the orientation changes
     *
     * @param enable true if you want to reload fullscreen ads automatically
     * @return This AdsConfigurator instance for method chaining
     */
    public AdsConfigurator autoReloadFullscreenAdsWhenOrientationChanged(boolean enable) {
        mAutoReloadFullscreenAdsWhenOrientationChanged = enable;
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
        return new NativeAdsConfigurator(this, adsUnitId).template(NativeTemplate.MEDIUM_1);
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
        if (mDebug != null) {
            mAdsManager.setDebug(mDebug);
        }
        if (mAutoLoadFullscreenAdsWhenHasInternet != null) {
            mAdsManager.setAutoLoadFullscreenAdsWhenHasInternet(mAutoLoadFullscreenAdsWhenHasInternet);
        }
        if (mAutoReloadFullscreenAdsWhenOrientationChanged != null) {
            mAdsManager.setAutoReloadFullscreenAdsWhenOrientationChanged(mAutoReloadFullscreenAdsWhenOrientationChanged);
        }
        if (mAppOpenObserverEnabled != null) {
            mAdsManager.setAppOpenObserverEnabled(mAppOpenObserverEnabled);
        }
        if (mAppOpenObserverBlackListActivity != null) {
            mAdsManager.setAppOpenObserverBlackListActivity(mAppOpenObserverBlackListActivity);
        }
        if (mOnPaidEventListener != null) {
            mAdsManager.setOnPaidEventListener(mOnPaidEventListener);
        }
        mAds.forEach(mAdsManager::putAds);
        mAdsManager.updateObserver();
        mCallback.callback();
    }

}
