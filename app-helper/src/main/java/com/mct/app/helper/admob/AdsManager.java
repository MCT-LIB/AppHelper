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
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.MobileAds;
import com.mct.app.helper.admob.ads.AppOpenAds;
import com.mct.app.helper.admob.ads.BannerAds;
import com.mct.app.helper.admob.ads.BaseAds;
import com.mct.app.helper.admob.ads.BaseFullScreenAds;
import com.mct.app.helper.admob.ads.BaseViewAds;
import com.mct.app.helper.admob.ads.InterstitialAds;
import com.mct.app.helper.admob.ads.NativeAds;
import com.mct.app.helper.admob.ads.RewardedAds;
import com.mct.app.helper.admob.ads.RewardedInterstitialAds;
import com.mct.app.helper.admob.ads.natives.NativeAdsAdapter;
import com.mct.app.helper.admob.listener.AdCallback;
import com.mct.app.helper.admob.listener.LoadCallback;
import com.mct.app.helper.admob.listener.LoadError;
import com.mct.app.helper.admob.listener.RewardedAdCallback;
import com.mct.app.helper.admob.listener.ShowMode;
import com.mct.app.helper.admob.listener.SkipReason;
import com.mct.app.helper.admob.utils.DVC;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class AdsManager {

    private static final String TAG = "AdsManager";

    /**
     * Default minimum gap between ANY two full-screen ads, to comply with AdMob's
     * "no back-to-back full-screen ads" policy without mutually locking different
     * placements for the whole of each other's interval.
     */
    static final long DEFAULT_FULL_SCREEN_ADS_GAP_MS = 15 * 1000;

    private static AdsManager instance;

    // configs
    private Application mApplication;
    private final AtomicBoolean mIsInitialized = new AtomicBoolean(false);
    private final AtomicBoolean mIsPremium = new AtomicBoolean(false);
    private final AtomicBoolean mDebug = new AtomicBoolean(false);
    private final AtomicLong mFullScreenAdsGapMs = new AtomicLong(DEFAULT_FULL_SCREEN_ADS_GAP_MS);
    private final AtomicReference<OnAdsLoadListener> mOnAdsLoadListener = new AtomicReference<>();
    private final AtomicReference<OnPaidEventListeners> mOnPaidEventListener = new AtomicReference<>();
    private final ObserverConnection mObserverConnection = new ObserverConnection();
    private final ObserverLifecycleAppOpen mObserverLifecycle = new ObserverLifecycleAppOpen();

    // ads holder
    private final Map<String, BaseAds<?>> mAds = new LinkedHashMap<>();
    private final List<NativeAdsAdapter> mNativeAdsAdapters = new ArrayList<>();
    private volatile String lastConsentError;

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
     * Check if ads is initialized
     *
     * @return is initialized
     */
    public boolean isInit() {
        return mIsInitialized.get();
    }

    /**
     * Initialize ads
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
            // Mirror initAsync: explicitly initialize the GMS SDK once consent allows it
            // (on a background thread, per Google's guidance). Relying on the SDK's lazy
            // init makes the first ad load slower and behaves differently from initAsync.
            AtomicReference<Thread> initThreadAtomic = new AtomicReference<>(new Thread(() ->
                    MobileAds.initialize(mApplication, status -> {
                    })));
            Runnable initialize = () -> Optional.ofNullable(initThreadAtomic.getAndSet(null)).ifPresent(Thread::start);
            GoogleMobileAdsConsentManager manager = GoogleMobileAdsConsentManager.getInstance(mApplication);
            ConsentGate.get().begin(manager::canRequestAds);
            manager.gatherConsent(activity, e -> {
                lastConsentError = e == null ? null : e.getMessage();
                ConsentGate.get().complete();
                initialize.run();
            });
            if (manager.canRequestAds()) {
                initialize.run();
            }
        }));
    }

    /**
     * Initialize ads
     *
     * @param activity             activity
     * @param configuratorConsumer configurator
     * @param onReady              callback
     */
    public void initAsync(@NonNull Activity activity, @NonNull Consumer<AdsConfigurator> configuratorConsumer, @NonNull Runnable onReady) {
        if (mIsInitialized.getAndSet(true)) {
            // already initialized
            onReady.run();
            return;
        }
        mApplication = activity.getApplication();
        configuratorConsumer.accept(new AdsConfigurator(this, () -> {
            AtomicReference<Thread> initThreadAtomic = new AtomicReference<>(new Thread(() -> {
                // Initialize the Google Mobile Ads SDK on a background thread.
                MobileAds.initialize(mApplication, status -> activity.runOnUiThread(onReady));
            }));
            Runnable initialize = () -> Optional.ofNullable(initThreadAtomic.getAndSet(null)).ifPresent(Thread::start);
            GoogleMobileAdsConsentManager manager = GoogleMobileAdsConsentManager.getInstance(mApplication);
            ConsentGate.get().begin(manager::canRequestAds);
            manager.gatherConsent(activity, e -> {
                lastConsentError = e == null ? null : e.getMessage();
                ConsentGate.get().complete();
                initialize.run();
            });
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

    /**
     * Clear ads
     */
    public void clear() {
        if (mApplication != null) {
            mObserverConnection.release(mApplication);
            mObserverLifecycle.release(mApplication);
            mApplication = null;
        }
        mAds.values().forEach(BaseAds::clear);
        mAds.clear();
        mNativeAdsAdapters.clear();
        mOnAdsLoadListener.set(null);
        mOnPaidEventListener.set(null);
        mDebug.set(false);
        mIsPremium.set(false);
        mFullScreenAdsGapMs.set(DEFAULT_FULL_SCREEN_ADS_GAP_MS);
        ConsentGate.get().reset();
        lastConsentError = null;
        mIsInitialized.set(false);
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
    // Consent (UMP)
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Whether UMP currently allows ad requests. Reads the consent stored by UMP, so it is meaningful
     * even before {@link #init} on a returning user. Returns {@code false} if no context is known yet.
     *
     * @param context any context (used only to reach UMP's ConsentInformation)
     */
    public boolean canRequestAds(@NonNull Context context) {
        return GoogleMobileAdsConsentManager.getInstance(context.getApplicationContext()).canRequestAds();
    }

    /**
     * Same as {@link #canRequestAds(Context)}, using the application captured by {@link #init}.
     * Returns {@code false} before init.
     */
    public boolean canRequestAds() {
        Application app = mApplication;
        return app != null && canRequestAds(app);
    }

    /**
     * Whether the app must offer an entry point to the UMP privacy-options form
     * (PrivacyOptionsRequirementStatus == REQUIRED). Show a "Privacy options" row only when true.
     * Only reliable after consent has resolved (see {@link #runWhenConsentResolved}).
     */
    public boolean isPrivacyOptionsRequired(@NonNull Context context) {
        return GoogleMobileAdsConsentManager.getInstance(context.getApplicationContext()).isPrivacyOptionsRequired();
    }

    /**
     * Shows the UMP privacy-options form so the user can change their consent choice.
     *
     * @param activity the current activity
     * @param callback called when the form is dismissed; {@code errorMessage} is null on success
     */
    public void showPrivacyOptionsForm(@NonNull Activity activity, @Nullable OnPrivacyOptionsFormDismissListener callback) {
        GoogleMobileAdsConsentManager.getInstance(activity.getApplicationContext())
                .showPrivacyOptionsForm(activity, formError -> {
                    if (callback != null) {
                        callback.onDismissed(formError == null ? null : formError.getMessage());
                    }
                });
    }

    /**
     * True once the UMP flow started by {@link #init}/{@link #initAsync} has finished (consent form
     * dismissed, not required, or failed). After that, {@link #canRequestAds()} is final for this
     * launch until the user changes it in the privacy-options form.
     */
    public boolean isConsentResolved() {
        return ConsentGate.get().isResolved();
    }

    /**
     * Error message from the last consent gathering, or {@code null} if it succeeded / has not run.
     * A non-null value usually means no network: {@link #canRequestAds()} then reflects the
     * consent stored from a previous launch.
     */
    @Nullable
    public String getLastConsentError() {
        return lastConsentError;
    }

    /**
     * Runs {@code action} on the main thread once consent has resolved (immediately if it already
     * has). Use it to delay the splash / first ad show until the consent form is gone. If
     * {@link #init} is never called, {@code action} never runs.
     */
    public void runWhenConsentResolved(@NonNull Runnable action) {
        ConsentGate.get().runWhenResolved(action);
    }

    /**
     * When {@code true} (default) every ad load waits for the UMP consent flow started by
     * {@link #init}/{@link #initAsync} and is only sent if {@link #canRequestAds()} is true; loads
     * issued while consent is still being gathered are replayed once it resolves, and loads issued
     * after a "no" fail with {@link com.mct.app.helper.admob.listener.LoadError#CODE_CONSENT_NOT_OBTAINED}.
     * Turn it off only if the app runs its own consent platform.
     */
    public void setConsentRequiredBeforeLoad(boolean required) {
        ConsentGate.get().setEnabled(required);
    }

    public boolean isConsentRequiredBeforeLoad() {
        return ConsentGate.get().isEnabled();
    }

    /**
     * Callback for {@link #showPrivacyOptionsForm}.
     */
    public interface OnPrivacyOptionsFormDismissListener {
        /**
         * @param errorMessage null if the form was shown and dismissed normally
         */
        void onDismissed(@Nullable String errorMessage);
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
        // Keep observers in sync with the premium flag. Without this, an app that
        // applied config while premium (observers released) and later calls
        // setPremium(false) directly would never get the app-open foreground
        // observer back - app-open ads silently stop for the session.
        // init()/release() are idempotent, so calling this repeatedly is safe.
        updateObserver();
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
        return DVC.isRdv(isDebug());
    }

    /**
     * Get the minimum gap (millis) enforced between any two full-screen ads.
     *
     * @return the global full-screen gap in millis
     */
    public long getFullScreenAdsGapMs() {
        return mFullScreenAdsGapMs.get();
    }

    /**
     * Set the minimum gap (millis) enforced between any two full-screen ads
     * (app-open, interstitial, native-full-screen, rewarded). This prevents
     * showing two full-screen ads back-to-back as required by AdMob policy.
     * <br/>
     * Each placement still honours its own {@code adsInterval} independently;
     * this gap only governs the global spacing across different placements.
     * Pass {@code 0} to disable (only the overlap guard remains).
     *
     * @param gapMs the gap in millis (negative values are clamped to 0)
     */
    public void setFullScreenAdsGapMs(long gapMs) {
        mFullScreenAdsGapMs.set(Math.max(0, gapMs));
    }

    /**
     * Set auto check device when has internet
     *
     * @param unitId ad unit id
     */
    public void setAutoCheckDeviceWhenHasInternet(String unitId) {
        mObserverConnection.setAutoCheckDeviceWhenHasInternet(unitId);
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
     * Get on ads load listener
     *
     * @return on ads load listener
     */
    public OnAdsLoadListener getOnAdsLoadListener() {
        return mOnAdsLoadListener.get();
    }

    /**
     * Set on ads load listener
     *
     * @param listener on ads load listener
     */
    public void setOnAdsLoadListener(OnAdsLoadListener listener) {
        mOnAdsLoadListener.set(listener);
    }

    /**
     * Get on paid event listener
     *
     * @return on paid event listener
     */
    public OnPaidEventListeners getOnPaidEventListener() {
        return mOnPaidEventListener.get();
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

    public <Ads> void onAdsFailedToLoad(BaseAds<Ads> ads, LoadError error) {
        Optional.ofNullable(getOnAdsLoadListener()).ifPresent(listener -> listener.onAdsFailedToLoad(ads, error));
    }

    public <Ads> void onAdsLoaded(BaseAds<Ads> ads) {
        Optional.ofNullable(getOnAdsLoadListener()).ifPresent(listener -> listener.onAdsLoaded(ads));
    }

    ///////////////////////////////////////////////////////////////////////////
    // Ads interaction methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Load an ad (no callback).
     */
    public void load(String alias, Context context) {
        load(alias, context, null);
    }

    /**
     * Load an ad. The callback reports success / failure with an error.
     */
    public void load(String alias, Context context, @Nullable LoadCallback callback) {
        load(getAds(alias, BaseAds.class), context, callback);
    }

    void load(@Nullable BaseAds<?> ads, Context context, @Nullable LoadCallback callback) {
        if (ads == null) {
            if (callback != null) {
                callback.onAdFailedToLoad(null, new LoadError(-1, "No ad registered", "com.mct.app.helper"));
            }
            return;
        }
        if (isPremium()) {
            if (callback != null) {
                callback.onAdFailedToLoad(ads, new LoadError(-1, "Premium user", "com.mct.app.helper"));
            }
            return;
        }
        ads.load(context, callback);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Full-screen (app-open / interstitial / native-full-screen)
    ///////////////////////////////////////////////////////////////////////////

    /** Show a full-screen ad if one is cached. */
    public void showFullScreen(String alias, Activity activity) {
        showFullScreen(alias, alias, activity, ShowMode.IF_READY, null);
    }

    /** Show a full-screen ad if one is cached, reporting the outcome. */
    public void showFullScreen(String alias, Activity activity, @Nullable AdCallback callback) {
        showFullScreen(alias, alias, activity, ShowMode.IF_READY, callback);
    }

    /** Show a full-screen ad, optionally loading first ({@link ShowMode#LOAD_IF_NEEDED}). */
    public void showFullScreen(String alias, Activity activity, @NonNull ShowMode mode, @Nullable AdCallback callback) {
        showFullScreen(alias, alias, activity, mode, callback);
    }

    /**
     * Show a full-screen ad.
     *
     * @param alias       ad alias to show
     * @param reportAlias alias used for paid-event / revenue attribution
     * @param mode        {@link ShowMode#IF_READY} or {@link ShowMode#LOAD_IF_NEEDED}
     * @param callback    outcome callback (nullable)
     * @see AppOpenAds
     * @see InterstitialAds
     */
    public void showFullScreen(String alias, String reportAlias, Activity activity, @NonNull ShowMode mode, @Nullable AdCallback callback) {
        showFullScreenInternal(getAds(alias, BaseFullScreenAds.class), reportAlias, activity, mode, callback);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Rewarded (rewarded / rewarded-interstitial)
    ///////////////////////////////////////////////////////////////////////////

    /** Show a rewarded ad if one is cached. */
    public void showRewarded(String alias, Activity activity, @Nullable RewardedAdCallback callback) {
        showRewarded(alias, alias, activity, ShowMode.IF_READY, callback);
    }

    /** Show a rewarded ad, optionally loading first. */
    public void showRewarded(String alias, Activity activity, @NonNull ShowMode mode, @Nullable RewardedAdCallback callback) {
        showRewarded(alias, alias, activity, mode, callback);
    }

    /**
     * Show a rewarded ad. The reward is delivered via
     * {@link RewardedAdCallback#onUserEarnedReward}.
     *
     * @see RewardedAds
     * @see RewardedInterstitialAds
     */
    public void showRewarded(String alias, String reportAlias, Activity activity, @NonNull ShowMode mode, @Nullable RewardedAdCallback callback) {
        showFullScreenInternal(getAds(alias, BaseFullScreenAds.class), reportAlias, activity, mode, callback);
    }

    ///////////////////////////////////////////////////////////////////////////
    // View ads (banner / native)
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Show a view ad in {@code container}, loading first if necessary.
     *
     * @see BannerAds
     * @see NativeAds
     */
    public void showView(String alias, ViewGroup container) {
        showView(alias, alias, container);
    }

    /** Show a view ad with a separate revenue-attribution alias. */
    public void showView(String alias, String reportAlias, ViewGroup container) {
        if (isPremium()) {
            return;
        }
        BaseViewAds<?> ads = getAds(alias, BaseViewAds.class);
        if (ads != null) {
            ads.show(reportAlias, container);
        }
    }

    /**
     * Show a view ad. When {@code reload} is true the current ad is cleared and a
     * fresh one is loaded (e.g. for collapsible banners); otherwise a ready ad is
     * reused.
     *
     * @see BannerAds
     * @see NativeAds
     */
    public void showView(String alias, ViewGroup container, boolean reload) {
        if (isPremium()) {
            return;
        }
        BaseViewAds<?> ads = getAds(alias, BaseViewAds.class);
        if (ads != null) {
            ads.forceShow(alias, container, reload);
        }
    }

    /**
     * Hide a view ad.
     *
     * @see BannerAds
     * @see NativeAds
     */
    public void hide(String alias) {
        BaseViewAds<?> ads = getAds(alias, BaseViewAds.class);
        if (ads != null) {
            ads.hide();
        }
    }

    /**
     * Hide a view ad only if it is still attached to {@code container}.
     *
     * @see BannerAds
     * @see NativeAds
     */
    public void hide(String alias, ViewGroup container) {
        BaseViewAds<?> ads = getAds(alias, BaseViewAds.class);
        if (ads != null) {
            ads.hide(container);
        }
    }

    /**
     * Pause a cached view ad.
     *
     * @see BannerAds
     * @see NativeAds
     */
    public void pauseView(String alias) {
        BaseViewAds<?> ads = getAds(alias, BaseViewAds.class);
        if (ads != null) {
            ads.pause();
        }
    }

    /**
     * Resume a cached view ad.
     *
     * @see BannerAds
     * @see NativeAds
     */
    public void resumeView(String alias) {
        BaseViewAds<?> ads = getAds(alias, BaseViewAds.class);
        if (ads != null) {
            ads.resume();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Status queries
    ///////////////////////////////////////////////////////////////////////////

    /** @return true if the ad can start a load now */
    public boolean isCanLoadAds(String alias) {
        BaseAds<?> ads = getAds(alias, BaseAds.class);
        return ads != null && !isPremium() && ads.isCanLoadAds();
    }

    /** @return true if the ad is cached and eligible to show now (ignoring the global gap) */
    public boolean isCanShowAds(String alias) {
        BaseAds<?> ads = getAds(alias, BaseAds.class);
        return ads != null && !isPremium() && ads.isCanShowAds();
    }

    /** @return true if the ad is currently loading */
    public boolean isLoading(String alias) {
        BaseAds<?> ads = getAds(alias, BaseAds.class);
        return ads != null && ads.isLoading();
    }

    /** @return true if the ad is currently showing */
    public boolean isShowing(String alias) {
        BaseAds<?> ads = getAds(alias, BaseAds.class);
        return ads != null && ads.isShowing();
    }

    /**
     * @return true if a full-screen ad was dismissed within the last few seconds
     * @deprecated prefer {@link AdCallback#onDismissed()} to know an ad was shown.
     */
    @Deprecated
    public boolean isDismissNearly(String alias) {
        BaseAds<?> ads = getAds(alias, BaseAds.class);
        return ads != null && ads.isDismissNearly();
    }

    /**
     * Clear ads
     */
    public void clearAds(String alias) {
        BaseAds<?> ads = getAds(alias, BaseAds.class);
        if (ads != null) {
            ads.clear();
        }
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
                    // NOTE: the dismissing ad already re-armed its own interval in
                    // BaseFullScreenAds#onAdDismissedFullScreenContent. We deliberately
                    // do NOT re-arm every other placement here: cross-cooldown made one
                    // impression lock all other full-screen placements for the whole of
                    // their interval, crushing the show rate. Global back-to-back spacing
                    // is now enforced centrally by checkShowFullScreenAdsCondition().
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

    /**
     * Apply the global full-screen gate (registered / premium / overlap / global
     * back-to-back gap), reporting the precise {@link SkipReason} on rejection,
     * then delegate the per-placement show to the ad itself.
     */
    private void showFullScreenInternal(@Nullable BaseFullScreenAds<?> ads,
                                        String reportAlias,
                                        Activity activity,
                                        @NonNull ShowMode mode,
                                        @Nullable AdCallback callback) {
        if (ads == null) {
            skip(callback, SkipReason.NOT_REGISTERED);
            return;
        }
        if (isPremium()) {
            skip(callback, SkipReason.PREMIUM);
            return;
        }
        boolean isRewarded = ads instanceof RewardedAds || ads instanceof RewardedInterstitialAds;
        boolean isRewardedRequest = isRewarded && callback instanceof RewardedAdCallback;
        if (isRewarded && !isRewardedRequest) {
            Log.w(TAG, "showFullScreen called for rewarded alias '" + ads.getAlias()
                    + "' without a RewardedAdCallback - onUserEarnedReward will NOT be delivered."
                    + " Use showRewarded(...) instead.");
        }
        releaseStaleShowingAds();
        boolean anyFullScreenShowing = false;
        long lastFullScreenDismissTime = 0L;
        for (BaseAds<?> a : getAdsList()) {
            if (a instanceof BaseFullScreenAds) {
                if (a.isShowing()) {
                    anyFullScreenShowing = true;
                }
                long dismissTime = ((BaseFullScreenAds<?>) a).getLastDismissedAt();
                if (dismissTime > lastFullScreenDismissTime) {
                    lastFullScreenDismissTime = dismissTime;
                }
            }
        }
        if (anyFullScreenShowing) {
            skip(callback, SkipReason.OVERLAP);
            return;
        }
        // Rewarded ads are shown after an explicit user opt-in. Do not reject
        // that request just because another full-screen ad recently dismissed;
        // overlap, premium, activity validity, load state, and failures still
        // apply below.
        if (!isRewardedRequest
                && AdsShowPolicy.isWithinGlobalGap(System.currentTimeMillis(), lastFullScreenDismissTime, getFullScreenAdsGapMs())) {
            skip(callback, SkipReason.GLOBAL_GAP);
            return;
        }
        // per-placement state (ready / cooldown / load-if-needed) handled by the ad
        ads.show(reportAlias, activity, mode, callback);
    }

    /**
     * Stuck-SHOWING watchdog sweep. A full-screen ad whose terminal GMS callbacks
     * were dropped (ad activity killed, SDK bug) stays {@code SHOWING} forever and
     * blocks every other full-screen ad with {@link SkipReason#OVERLAP}. Force-release
     * any ad that has been "showing" longer than its max show duration so the
     * pipeline can recover. Late terminal events for the released show are ignored
     * by the state guards in {@code transitionDismissed}/{@code transitionFailedToShow}.
     */
    void releaseStaleShowingAds() {
        for (BaseAds<?> a : getAdsList()) {
            if (a instanceof BaseFullScreenAds && a.isShowingStale()) {
                Log.w(TAG, "Full-screen ad '" + a.getAlias() + "' stuck in SHOWING for over "
                        + "its max show duration - force releasing (watchdog).");
                a.clear();
            }
        }
    }

    private static void skip(@Nullable AdCallback callback, @NonNull SkipReason reason) {
        if (callback != null) {
            callback.onSkipped(reason);
            callback.onComplete(false);
        }
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
}
