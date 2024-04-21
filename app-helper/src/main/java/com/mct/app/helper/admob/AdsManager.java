package com.mct.app.helper.admob;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class AdsManager {

    private static final String TAG = "AdsManager";
    private static AdsManager instance;
    private final AtomicBoolean mIsInitialized = new AtomicBoolean(false);
    private final AtomicBoolean mIsPremium = new AtomicBoolean(false);
    private final AtomicBoolean mDebug = new AtomicBoolean(false);
    private final AtomicReference<OnPaidEventListener> mOnPaidEventListener = new AtomicReference<>();
    private final Map<String, BaseAds<?>> mAds = new LinkedHashMap<>();
    private final AppOpenLifecycleObserver mAppOpenObserver = new AppOpenLifecycleObserver();
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
    // Config methods
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
        AtomicBoolean invokeFlag = new AtomicBoolean(false);
        configuratorConsumer.accept(new AdsConfigurator(this, () -> {
            GoogleMobileAdsConsentManager manager = GoogleMobileAdsConsentManager.getInstance(activity.getApplicationContext());
            manager.gatherConsent(activity, consentError -> {
                if (invokeFlag.get()) {
                    return;
                }
                mAppOpenObserver.init(activity.getApplication());
                MobileAds.initialize(activity.getApplicationContext());
            });
            if (manager.canRequestAds()) {
                invokeFlag.set(true);
                mAppOpenObserver.init(activity.getApplication());
                MobileAds.initialize(activity.getApplicationContext());
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
     * Pending app open observer one time
     */
    public void pendingAppOpenObserver() {
        mAppOpenObserver.pendingShowAd();
    }

    /**
     * Remove pending app open observer
     */
    public void removePendingAppOpenObserver() {
        mAppOpenObserver.removePendingShowAd();
    }

    /**
     * Set app open observer enabled
     *
     * @param enabled true if enabled
     */
    public void setAppOpenObserverEnabled(boolean enabled) {
        mAppOpenObserver.setEnabled(enabled);
    }

    /**
     * Set app open observer black list activity
     *
     * @param classes black list activity classes
     */
    public void setAppOpenObserverBlackListActivity(Class<?>... classes) {
        mAppOpenObserver.setBlackListActivity(classes);
    }

    /**
     * Set app open observer app open ads model
     *
     * @param ads app open ads
     */
    public void setAppOpenObserverAds(AppOpenAds ads) {
        mAppOpenObserver.setAppOpenAds(ads);
    }

    /**
     * Get on paid event listener
     *
     * @return on paid event listener
     */
    public OnPaidEventListener getOnPaidEventListener() {
        return mOnPaidEventListener.get();
    }

    /**
     * Set on paid event listener
     *
     * @param listener on paid event listener
     */
    public void setOnPaidEventListener(OnPaidEventListeners listener) {
        mOnPaidEventListener.set(Optional.ofNullable(listener).map(OnPaidEventListeners::toGms).orElse(null));
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

    /* --- Ads actions methods --- */

    /**
     * Load ads by alias
     *
     * @param alias   alias of ads
     * @param context context
     * @param success callback when success
     * @param failure callback when failure
     */
    public void load(String alias, Context context, Callback success, Callback failure) {
        load(getAds(alias, BaseAds.class), context, success, failure);
    }

    /**
     * Load ads by model
     *
     * @param ads     ads model
     * @param context context
     * @param success callback when success
     * @param failure callback when failure
     */
    public void load(BaseAds<?> ads, Context context, Callback success, Callback failure) {
        if (checkAdsCondition(ads, failure)) {
            ads.load(context, success, failure);
        }
    }

    /**
     * Show full screen ads by alias
     *
     * @param alias    alias of ads
     * @param activity activity
     * @param callback callback when finish(always called)
     * @see AppOpenAds
     * @see InterstitialAds
     * @see RewardedAds
     * @see RewardedInterstitialAds
     */
    public void show(String alias, Activity activity, Callback callback) {
        show(getAds(alias, BaseFullScreenAds.class), activity, callback);
    }

    /**
     * Show full screen ads by model
     *
     * @param ads      ads model
     * @param activity activity
     * @param callback callback when finish(always called)
     * @see AppOpenAds
     * @see InterstitialAds
     * @see RewardedAds
     * @see RewardedInterstitialAds
     */
    public void show(BaseFullScreenAds<?> ads, Activity activity, Callback callback) {
        if (checkAdsCondition(ads, callback)) {
            ads.show(activity, false, callback);
        }
    }

    /**
     * Show rewarded ads by alias
     *
     * @param alias              alias of ads
     * @param activity           activity
     * @param callback           callback when finish(always called)
     * @param onUserEarnedReward callback when user earned reward
     * @see RewardedAds
     * @see RewardedInterstitialAds
     */
    public void show(String alias, Activity activity, Callback callback, Callback onUserEarnedReward) {
        show(getAds(alias, BaseRewardedAds.class), activity, callback, onUserEarnedReward);
    }

    /**
     * Show rewarded ads by model
     *
     * @param ads                ads model
     * @param activity           activity
     * @param callback           callback when finish(always called)
     * @param onUserEarnedReward callback when user earned reward
     * @see RewardedAds
     * @see RewardedInterstitialAds
     */
    public void show(BaseRewardedAds<?> ads, Activity activity, Callback callback, Callback onUserEarnedReward) {
        if (checkAdsCondition(ads, callback)) {
            ads.show(activity, false, callback, onUserEarnedReward);
        }
    }


    /**
     * Show full screen ads by alias, wait load and show if necessary
     *
     * @param alias    alias of ads
     * @param activity activity
     * @param callback callback when finish(always called)
     * @see AppOpenAds
     * @see InterstitialAds
     * @see RewardedAds
     * @see RewardedInterstitialAds
     */
    public void showSyncLoad(String alias, Activity activity, Callback callback) {
        showSyncLoad(getAds(alias, BaseFullScreenAds.class), activity, callback);
    }

    /**
     * Show full screen ads by model, wait load and show if necessary
     *
     * @param ads      ads model
     * @param activity activity
     * @param callback callback when finish(always called)
     * @see AppOpenAds
     * @see InterstitialAds
     * @see RewardedAds
     * @see RewardedInterstitialAds
     */
    public void showSyncLoad(BaseFullScreenAds<?> ads, Activity activity, Callback callback) {
        if (checkAdsCondition(ads, callback)) {
            ads.show(activity, true, callback);
        }
    }

    /**
     * Show rewarded ads by alias, wait load and show if necessary
     *
     * @param alias              alias of ads
     * @param activity           activity
     * @param callback           callback when finish(always called)
     * @param onUserEarnedReward callback when user earned reward
     * @see RewardedAds
     * @see RewardedInterstitialAds
     */
    public void showSyncLoad(String alias, Activity activity, Callback callback, Callback onUserEarnedReward) {
        showSyncLoad(getAds(alias, BaseRewardedAds.class), activity, callback, onUserEarnedReward);
    }

    /**
     * Show rewarded ads by model, wait load and show if necessary
     *
     * @param ads                ads model
     * @param activity           activity
     * @param callback           callback when finish(always called)
     * @param onUserEarnedReward callback when user earned reward
     * @see RewardedAds
     * @see RewardedInterstitialAds
     */
    public void showSyncLoad(BaseRewardedAds<?> ads, Activity activity, Callback callback, Callback onUserEarnedReward) {
        if (checkAdsCondition(ads, callback)) {
            ads.show(activity, true, callback, onUserEarnedReward);
        }
    }


    /**
     * Show view ads by alias
     *
     * @param alias     alias of ads
     * @param container container to show ads
     * @see BannerAds
     * @see NativeAds
     */
    public void show(String alias, ViewGroup container) {
        show(getAds(alias, BaseViewAds.class), container);
    }

    /**
     * Show view ads by model
     *
     * @param ads       ads model
     * @param container container to show ads
     * @see BannerAds
     * @see NativeAds
     */
    public void show(BaseViewAds<?> ads, ViewGroup container) {
        if (checkAdsCondition(ads, null)) {
            ads.show(container);
        }
    }

    /**
     * Force load and show view ads by alias
     *
     * @param alias          alias of ads
     * @param container      container to show ads
     * @param multiContainer enable multi container
     * @see BannerAds
     * @see NativeAds
     */
    public void forceShow(String alias, ViewGroup container, boolean multiContainer) {
        forceShow(getAds(alias, BaseViewAds.class), container, multiContainer);
    }

    /**
     * Force load and show view ads by model
     *
     * @param ads            ads model
     * @param container      container to show ads
     * @param multiContainer enable multi container
     * @see BannerAds
     * @see NativeAds
     */
    public void forceShow(BaseViewAds<?> ads, ViewGroup container, boolean multiContainer) {
        if (checkAdsCondition(ads, null)) {
            ads.forceShow(container, multiContainer);
        }
    }

    /**
     * Hide view ads by alias
     *
     * @param alias alias of ads
     * @see BannerAds
     * @see NativeAds
     */
    public void hide(String alias) {
        hide(getAds(alias, BaseViewAds.class));
    }

    /**
     * Hide view ads by model
     *
     * @param ads ads model
     * @see BannerAds
     * @see NativeAds
     */
    public void hide(BaseViewAds<?> ads) {
        if (ads == null) {
            return;
        }
        ads.hide();
    }

    /**
     * Check if manager can load ads by alias
     *
     * @param alias alias of ads
     * @return true if manager can load ads
     */
    public boolean isCanLoadAds(String alias) {
        return isCanLoadAds(getAds(alias, BaseAds.class));
    }

    /**
     * Check if manager can load ads by model
     *
     * @param ads ads model
     * @return true if manager can load ads
     */
    public boolean isCanLoadAds(BaseAds<?> ads) {
        if (checkAdsCondition(ads, null)) {
            return ads.isCanLoadAds();
        }
        return false;
    }

    /**
     * Check if manager can show ads by alias
     *
     * @param alias alias of ads
     * @return true if manager can show ads
     */
    public boolean isCanShowAds(String alias) {
        return isCanShowAds(getAds(alias, BaseAds.class));
    }

    /**
     * Check if manager can show ads by model
     *
     * @param ads ads model
     * @return true if manager can show ads
     */
    public boolean isCanShowAds(BaseAds<?> ads) {
        if (checkAdsCondition(ads, null)) {
            return ads.isCanShowAds();
        }
        return false;
    }

    /**
     * Check if manager contains ads by alias
     *
     * @param alias alias of ads
     * @return true if manager contains ads
     */
    public boolean containsAds(String alias) {
        return mAds.containsKey(alias);
    }

    /**
     * Remove ads from manager by alias
     *
     * @param alias alias of ads
     */
    public void removeAds(String alias) {
        mAds.remove(alias);
    }

    /**
     * Put ads to manager. Used in {@link AdsConfigurator}
     */
    public void putAds(String alias, BaseAds<?> ads) {
        if (containsAds(alias)) {
            Log.e(TAG, "Failed to put ads: " + ads + " with alias: " + alias + " already exists");
            return;
        }
        ads.setDebugSupplier(mDebug::get);
        ads.setOnPaidEventListenerSupplier(mOnPaidEventListener::get);
        if (ads instanceof BaseFullScreenAds) {
            if (ads instanceof AppOpenAds) {
                setAppOpenObserverAds((AppOpenAds) ads);
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
                    if (isInterstitialOrOpenAd(ads)) {
                        // post delay show flag
                        getAdsList().stream().filter(AdsManager::isInterstitialOrOpenAd).forEach(BaseAds::postDelayShowFlag);
                    }
                    if (viewAds != null) {
                        viewAds.forEach(view -> view.first.setVisibility(view.second));
                        viewAds.clear();
                        viewAds = null;
                    }
                }
            });
        }
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

    ///////////////////////////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////////////////////////

    private boolean checkAdsCondition(BaseAds<?> ads, Callback callback) {
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
    private List<Pair<View, Integer>> getBannerAndNativeViews() {
        List<Pair<View, Integer>> viewAds = new ArrayList<>();
        for (BaseAds<?> ads : mAds.values()) {
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
        for (NativeAdsAdapter adapter : mNativeAdsAdapters) {
            for (RecyclerView.ViewHolder holder : adapter.getBoundAdsViewHolders()) {
                viewAds.add(new Pair<>(
                        holder.itemView,
                        holder.itemView.getVisibility())
                );
            }
        }
        return viewAds;
    }

    private static boolean isInterstitialOrOpenAd(BaseAds<?> ads) {
        return ads instanceof InterstitialAds || ads instanceof AppOpenAds;
    }

    private static void invokeCallback(Callback callback) {
        if (callback != null) {
            callback.callback();
        }
    }
}
