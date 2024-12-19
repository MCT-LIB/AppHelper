package com.mct.app.helper.admob;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;
import androidx.core.util.Supplier;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnPaidEventListener;
import com.mct.app.helper.admob.ads.AppOpenAds;
import com.mct.app.helper.admob.ads.BannerAds;
import com.mct.app.helper.admob.ads.BaseAds;
import com.mct.app.helper.admob.ads.BaseFullScreenAds;
import com.mct.app.helper.admob.ads.BaseRewardedAds;
import com.mct.app.helper.admob.ads.BaseViewAds;
import com.mct.app.helper.admob.ads.InterstitialAds;
import com.mct.app.helper.admob.ads.NativeAds;
import com.mct.app.helper.admob.ads.RewardedAds;
import com.mct.app.helper.admob.ads.RewardedInterstitialAds;
import com.mct.app.helper.admob.ads.natives.NativeAdsAdapter;
import com.mct.app.helper.admob.utils.DeviceChecker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class AdsManager {

    private static final String TAG = "AdsManager";
    private static AdsManager instance;

    // configs
    private Application mApplication;
    private final AtomicBoolean mIsInitialized = new AtomicBoolean(false);
    private final AtomicBoolean mIsPremium = new AtomicBoolean(false);
    private final AtomicBoolean mDebug = new AtomicBoolean(false);
    private final AtomicReference<OnPaidEventListeners> mOnPaidEventListener = new AtomicReference<>();
    private final ObserverConnection mObserverConnection = new ObserverConnection();
    private final ObserverLifecycleAppOpen mObserverLifecycle = new ObserverLifecycleAppOpen();

    // ads holder
    private final Map<String, BaseAds<?>> mAds = new LinkedHashMap<>();
    private final List<NativeAdsAdapter> mNativeAdsAdapters = new ArrayList<>();

    public static AdsManager getInstance() {
        if (instance == null) {
            instance = new AdsManager();
        }
        return instance;
    }

    private AdsManager() {
    }

    ///////////////////////////////////////////////////////////////////////////
    // Init & Config methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Initialize ads(Usually called in Splash)
     *
     * @param activity             activity
     * @param configuratorConsumer configurator
     */
    public void init(@NonNull Activity activity, @NonNull Consumer<AdsConfigurator> configuratorConsumer) {
        if (mIsInitialized.getAndSet(true)) {
            // already initialized
            return;
        }
        mApplication = activity.getApplication();
        configuratorConsumer.accept(new AdsConfigurator(this, () -> {
            GoogleMobileAdsConsentManager.getInstance(mApplication).gatherConsent(activity, e -> {
            });
        }));
    }

    /**
     * Initialize ads(Usually called in Splash)
     *
     * @param activity             activity
     * @param configuratorConsumer configurator
     * @param callback             callback
     */
    public void initAsync(@NonNull Activity activity, @NonNull Consumer<AdsConfigurator> configuratorConsumer, @NonNull Callback callback) {
        if (mIsInitialized.getAndSet(true)) {
            // already initialized
            return;
        }
        mApplication = activity.getApplication();
        configuratorConsumer.accept(new AdsConfigurator(this, () -> {
            AtomicReference<Thread> initThreadAtomic = new AtomicReference<>(new Thread(() -> {
                // Initialize the Google Mobile Ads SDK on a background thread.
                MobileAds.initialize(mApplication, status -> activity.runOnUiThread(callback::callback));
            }));
            Runnable initialize = () -> Optional
                    .ofNullable(initThreadAtomic.getAndSet(null))
                    .ifPresent(Thread::start);
            GoogleMobileAdsConsentManager manager = GoogleMobileAdsConsentManager.getInstance(mApplication);
            manager.gatherConsent(activity, e -> initialize.run());
            if (manager.canRequestAds()) {
                initialize.run();
            }
        }));
    }

    /**
     * Configure ads
     *
     * @param configuratorConsumer configurator
     */
    public void config(@NonNull Consumer<AdsConfigurator> configuratorConsumer) {
        configuratorConsumer.accept(new AdsConfigurator(this));
    }

    void updateObserver() {
        if (mApplication == null) {
            return;
        }
        if (isPremium()) {
            mObserverConnection.release(mApplication);
            mObserverLifecycle.release(mApplication);
        } else {
            mObserverConnection.init(mApplication);
            mObserverLifecycle.init(mApplication);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Settings methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Check if user is premium
     *
     * @return true if user is premium
     */
    public boolean isPremium() {
        return mIsPremium.get();
    }

    /**
     * Set user is premium
     *
     * @param isPremium true if user is premium
     */
    public void setPremium(boolean isPremium) {
        mIsPremium.set(isPremium);
        //updateObserver();
    }

    /**
     * Get debug mode
     *
     * @return true if debug
     */
    public boolean isDebug() {
        return mDebug.get();
    }

    /**
     * Set debug mode
     *
     * @param debug true if debug
     */
    public void setDebug(boolean debug) {
        mDebug.set(debug);
    }

    /**
     * Check if device is real device
     *
     * @return true if real device
     */
    public boolean isRealDevice() {
        return DeviceChecker.isRealDevice(isDebug());
    }

    /**
     * Set auto check device when has internet
     *
     * @param enable true if auto check
     */
    public void setAutoCheckDeviceWhenHasInternet(boolean enable) {
        mObserverConnection.setAutoCheckDeviceWhenHasInternet(enable);
    }

    /**
     * Set auto load when has internet
     *
     * @param enable true if auto load
     */
    public void setAutoLoadFullscreenAdsWhenHasInternet(boolean enable) {
        mObserverConnection.setAutoLoadFullscreenAdsWhenHasInternet(enable);
    }

    /**
     * Set auto reload when orientation changed
     *
     * @param enable true if auto reload
     */
    public void setAutoReloadFullscreenAdsWhenOrientationChanged(boolean enable) {
        mObserverConnection.setAutoReloadFullscreenAdsWhenOrientationChanged(enable);
    }

    /**
     * Pending app open observer one time
     */
    public void pendingAppOpenObserver() {
        mObserverLifecycle.pendingShowAd();
    }

    /**
     * Remove pending app open observer
     */
    public void removePendingAppOpenObserver() {
        mObserverLifecycle.removePendingShowAd();
    }

    /**
     * Set app open observer enabled
     *
     * @param enabled true if enabled
     */
    public void setAppOpenObserverEnabled(boolean enabled) {
        mObserverLifecycle.setEnabled(enabled);
    }

    /**
     * Set app open observer black list activity
     *
     * @param classes black list activity classes
     */
    public void setAppOpenObserverBlackListActivity(Class<?>... classes) {
        mObserverLifecycle.setBlackListActivity(classes);
    }

    /**
     * Set app open observer app open ads model
     *
     * @param alias app open ads alias
     */
    public void setAppOpenAdsAlias(String alias) {
        mObserverLifecycle.setAppOpenAdsAlias(alias);
    }

    /**
     * Get on paid event listener
     *
     * @return on paid event listener
     */
    public OnPaidEventListener getOnPaidEventListener(Supplier<String> alias) {
        return Optional.ofNullable(mOnPaidEventListener.get()).map(l -> l.toGms(alias)).orElse(null);
    }

    /**
     * Set on paid event listener
     *
     * @param listener on paid event listener
     */
    public void setOnPaidEventListener(OnPaidEventListeners listener) {
        mOnPaidEventListener.set(listener);
    }

    public void registerNativeAdsAdapter(NativeAdsAdapter adapter) {
        if (mNativeAdsAdapters.contains(adapter)) {
            return;
        }
        mNativeAdsAdapters.add(adapter);
    }

    public void unregisterNativeAdsAdapter(NativeAdsAdapter adapter) {
        mNativeAdsAdapters.remove(adapter);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Ads interaction methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Load ads
     */
    public void load(String alias, Context context, Callback success, Callback failure) {
        load(getAds(alias, BaseAds.class), context, success, failure);
    }

    /**
     * Load ads
     */
    public void load(BaseAds<?> ads, Context context, Callback success, Callback failure) {
        if (checkAdsCondition(ads, failure)) {
            ads.load(context, success, failure);
        }
    }

    /**
     * Show appOpen or interstitial ads
     *
     * @see AppOpenAds
     * @see InterstitialAds
     */
    public void show(String alias, Activity activity, Callback callback) {
        show(alias, alias, activity, callback);
    }

    /**
     * Show appOpen or interstitial ads
     *
     * @see AppOpenAds
     * @see InterstitialAds
     */
    public void show(String alias, String customAlias, Activity activity, Callback callback) {
        BaseFullScreenAds<?> ads = getAds(alias, BaseFullScreenAds.class);
        if (checkShowFullScreenAdsCondition(ads, callback)) {
            assert ads != null;
            ads.show(customAlias, activity, false, callback);
        }
    }

    /**
     * Show appOpen or interstitial ads, wait load if necessary and show
     *
     * @see AppOpenAds
     * @see InterstitialAds
     */
    public void showSyncLoad(String alias, Activity activity, Callback callback) {
        showSyncLoad(alias, alias, activity, callback);
    }

    /**
     * Show appOpen or interstitial ads, wait load if necessary and show
     *
     * @see AppOpenAds
     * @see InterstitialAds
     */
    public void showSyncLoad(String alias, String customAlias, Activity activity, Callback callback) {
        BaseFullScreenAds<?> ads = getAds(alias, BaseFullScreenAds.class);
        if (checkShowFullScreenAdsCondition(ads, callback)) {
            assert ads != null;
            ads.show(customAlias, activity, true, callback);
        }
    }

    /**
     * Show rewarded ads
     *
     * @see RewardedAds
     * @see RewardedInterstitialAds
     */
    public void show(String alias, Activity activity, Callback callback, Callback onUserEarnedReward) {
        show(alias, alias, activity, callback, onUserEarnedReward);
    }

    /**
     * Show rewarded ads
     *
     * @see RewardedAds
     * @see RewardedInterstitialAds
     */
    public void show(String alias, String customAlias, Activity activity, Callback callback, Callback onUserEarnedReward) {
        BaseRewardedAds<?> ads = getAds(alias, BaseRewardedAds.class);
        if (checkShowFullScreenAdsCondition(ads, callback)) {
            assert ads != null;
            ads.show(customAlias, activity, false, callback, onUserEarnedReward);
        }
    }

    /**
     * Show rewarded ads, wait load if necessary and show
     *
     * @see RewardedAds
     * @see RewardedInterstitialAds
     */
    public void showSyncLoad(String alias, Activity activity, Callback callback, Callback onUserEarnedReward) {
        showSyncLoad(alias, alias, activity, callback, onUserEarnedReward);
    }

    /**
     * Show rewarded ads, wait load if necessary and show
     *
     * @see RewardedAds
     * @see RewardedInterstitialAds
     */
    public void showSyncLoad(String alias, String customAlias, Activity activity, Callback callback, Callback onUserEarnedReward) {
        BaseRewardedAds<?> ads = getAds(alias, BaseRewardedAds.class);
        if (checkShowFullScreenAdsCondition(ads, callback)) {
            assert ads != null;
            ads.show(customAlias, activity, true, callback, onUserEarnedReward);
        }
    }

    /**
     * Show view ads
     *
     * @see BannerAds
     * @see NativeAds
     */
    public void show(String alias, ViewGroup container) {
        show(alias, alias, container);
    }

    /**
     * Show view ads
     *
     * @see BannerAds
     * @see NativeAds
     */
    public void show(String alias, String customAlias, ViewGroup container) {
        BaseViewAds<?> ads = getAds(alias, BaseViewAds.class);
        if (checkAdsCondition(ads, null)) {
            assert ads != null;
            ads.show(customAlias, container);
        }
    }

    /**
     * Force load and show view ads
     *
     * @see BannerAds
     * @see NativeAds
     */
    public void forceShow(String alias, ViewGroup container, boolean multiContainer) {
        forceShow(alias, alias, container, multiContainer);
    }

    /**
     * Force load and show view ads
     *
     * @see BannerAds
     * @see NativeAds
     */
    public void forceShow(String alias, String customAlias, ViewGroup container, boolean multiContainer) {
        BaseViewAds<?> ads = getAds(alias, BaseViewAds.class);
        if (checkAdsCondition(ads, null)) {
            assert ads != null;
            ads.forceShow(customAlias, container, multiContainer);
        }
    }

    /**
     * Hide view ads
     *
     * @param alias alias of ads
     * @see BannerAds
     * @see NativeAds
     */
    public void hide(String alias) {
        BaseViewAds<?> ads = getAds(alias, BaseViewAds.class);
        if (ads == null) {
            return;
        }
        ads.hide();
    }

    /**
     * @return true if manager can load ads
     */
    public boolean isCanLoadAds(String alias) {
        BaseAds<?> ads = getAds(alias, BaseAds.class);
        if (checkAdsCondition(ads, null)) {
            assert ads != null;
            return ads.isCanLoadAds();
        }
        return false;
    }

    /**
     * @return true if manager can show ads
     */
    public boolean isCanShowAds(String alias) {
        BaseAds<?> ads = getAds(alias, BaseAds.class);
        if (checkAdsCondition(ads, null)) {
            assert ads != null;
            return ads.isCanShowAds();
        }
        return false;
    }

    /**
     * @return true if ads is loading
     */
    public boolean isLoading(String alias) {
        BaseAds<?> ads = getAds(alias, BaseAds.class);
        if (checkAdsCondition(ads, null)) {
            assert ads != null;
            return ads.isLoading();
        }
        return false;
    }

    /**
     * @return true if ads is showing
     */
    public boolean isShowing(String alias) {
        BaseAds<?> ads = getAds(alias, BaseAds.class);
        if (checkAdsCondition(ads, null)) {
            assert ads != null;
            return ads.isShowing();
        }
        return false;
    }

    /**
     * @return true if ads is can show
     */
    public boolean isCanShow(String alias) {
        BaseAds<?> ads = getAds(alias, BaseAds.class);
        if (checkAdsCondition(ads, null)) {
            assert ads != null;
            return ads.isCanShow();
        }
        return false;
    }

    /**
     * @return true if ads is dismiss nearly
     */
    public boolean isDismissNearly(String alias) {
        BaseAds<?> ads = getAds(alias, BaseAds.class);
        if (checkAdsCondition(ads, null)) {
            assert ads != null;
            return ads instanceof BaseFullScreenAds && ((BaseFullScreenAds<?>) ads).isDismissNearly();
        }
        return false;
    }

    /**
     * @return true if manager contains ads
     */
    public boolean containsAds(String alias) {
        return mAds.containsKey(alias);
    }

    /**
     * Remove ads from manager
     */
    public void removeAds(String alias) {
        mAds.remove(alias);
    }

    /**
     * Put ads to manager. Used in {@link AdsConfigurator}
     */
    public void putAds(String alias, BaseAds<?> ads) {
        Objects.requireNonNull(alias);
        Objects.requireNonNull(ads);
        if (containsAds(alias)) {
            Log.e(TAG, "Failed to put ads: " + ads + " with alias: " + alias + " already exists");
            return;
        }
        if (ads instanceof BaseFullScreenAds) {
            if (ads instanceof AppOpenAds) {
                setAppOpenAdsAlias(alias);
            }
            BaseFullScreenAds<?> baseFullScreenAds = (BaseFullScreenAds<?>) ads;
            baseFullScreenAds.setOnAdsShowChangeListener(new BaseFullScreenAds.OnAdsShowChangeListener() {

                List<Pair<View, Integer>> viewAds;

                @Override
                public void onShow(BaseFullScreenAds<?> fullScreenAds) {
                    if (fullScreenAds instanceof AppOpenAds) {
                        viewAds = getBannerAndNativeViews();
                        viewAds.forEach(view -> view.first.setVisibility(View.INVISIBLE));
                    }
                }

                @Override
                public void onDismiss(BaseFullScreenAds<?> fullScreenAds) {
                    if (ads.isAllowAdsInterval()) {
                        // post delay show flag
                        getAdsList().stream().filter(Objects::nonNull).forEach(BaseAds::postDelayShowFlag);
                    }
                    if (viewAds != null) {
                        viewAds.forEach(view -> view.first.setVisibility(view.second));
                        viewAds.clear();
                        viewAds = null;
                    }
                }
            });
        }
        ads.setAlias(alias);
        mAds.put(alias, ads);
    }

    /**
     * Get all ads aliases
     */
    @NonNull
    public List<String> getAdsAliases() {
        return new ArrayList<>(mAds.keySet());
    }

    /**
     * Get all ads
     */
    @NonNull
    public List<BaseAds<?>> getAdsList() {
        return new ArrayList<>(mAds.values());
    }

    /**
     * Get instance of ads
     *
     * @param alias alias of ads
     * @param clazz class of ads type to cast
     * @param <A>   type of ads
     * @return instance of ads
     */
    @Nullable
    public <A extends BaseAds<?>> A getAds(String alias, @NonNull Class<A> clazz) {
        BaseAds<?> ads = mAds.get(alias);
        if (clazz.isInstance(ads)) {
            return clazz.cast(ads);
        }
        return null;
    }

    List<NativeAdsAdapter> getNativeAdsAdapters() {
        return mNativeAdsAdapters;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////////////////////

    private static boolean checkShowFullScreenAdsCondition(BaseAds<?> ads, Callback callback) {
        // check ads condition first
        if (!checkAdsCondition(ads, callback)) {
            return false;
        }
        // check has any full screen ads is show
        if (getInstance().getAdsList().stream().anyMatch(a -> a instanceof BaseFullScreenAds && a.isShowing())) {
            invokeCallback(callback);
            return false;
        }
        return true;
    }

    private static boolean checkAdsCondition(BaseAds<?> ads, Callback callback) {
        if (ads == null) {
            invokeCallback(callback);
            return false;
        }
        if (getInstance().isPremium()) {
            invokeCallback(callback);
            return false;
        }
        return true;
    }

    @NonNull
    private static List<Pair<View, Integer>> getBannerAndNativeViews() {
        List<Pair<View, Integer>> viewAds = new ArrayList<>();
        for (BaseAds<?> ads : getInstance().getAdsList()) {
            if (ads instanceof BaseViewAds) {
                BaseViewAds<?> baseViewAds = (BaseViewAds<?>) ads;
                if (baseViewAds.getAds() != null) {
                    viewAds.add(new Pair<>(
                            baseViewAds.getAds(),
                            baseViewAds.getAds().getVisibility())
                    );
                }
            }
        }
        for (NativeAdsAdapter adapter : getInstance().mNativeAdsAdapters) {
            for (RecyclerView.ViewHolder holder : adapter.getBoundAdsViewHolders()) {
                viewAds.add(new Pair<>(
                        holder.itemView,
                        holder.itemView.getVisibility())
                );
            }
        }
        return viewAds;
    }

    private static void invokeCallback(Callback callback) {
        if (callback != null) {
            callback.callback();
        }
    }
}
