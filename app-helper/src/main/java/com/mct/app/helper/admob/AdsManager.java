package com.mct.app.helper.admob;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnPaidEventListener;
import com.mct.app.helper.admob.ads.AppOpenAds;
import com.mct.app.helper.admob.ads.BaseAds;
import com.mct.app.helper.admob.ads.BaseFullScreenAds;
import com.mct.app.helper.admob.ads.BaseRewardedAds;
import com.mct.app.helper.admob.ads.BaseViewAds;
import com.mct.app.helper.admob.ads.InterstitialAds;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdsManager {

    private static AdsManager instance;
    private final AtomicBoolean mIsInitialized = new AtomicBoolean(false);
    private final AtomicBoolean mIsPremium = new AtomicBoolean(false);
    private final Map<String, BaseAds<?>> mAds = new LinkedHashMap<>();
    private final AppOpenLifecycleObserver mAppOpenObserver = new AppOpenLifecycleObserver();
    private OnPaidEventListener mOnPaidEventListener;

    public static AdsManager getInstance() {
        if (instance == null) {
            instance = new AdsManager();
        }
        return instance;
    }

    private AdsManager() {
    }

    public void init(@NonNull Activity activity, @NonNull AdsProvider provider,
                     @Nullable OnPaidEventListener onPaidEventListener,
                     @Nullable Callback callback) {
        if (mIsInitialized.getAndSet(true)) {
            invokeCallback(callback);
            return;
        }
        mOnPaidEventListener = onPaidEventListener;
        putAds(provider.getAds());
        AtomicBoolean invokeFlag = new AtomicBoolean(false);
        GoogleMobileAdsConsentManager manager = GoogleMobileAdsConsentManager.getInstance(activity.getApplicationContext());
        manager.gatherConsent(activity, consentError -> {
            if (invokeFlag.get()) {
                return;
            }
            MobileAds.initialize(activity.getApplicationContext());
            invokeCallback(callback);
        });
        if (manager.canRequestAds()) {
            MobileAds.initialize(activity.getApplicationContext());
            invokeFlag.set(true);
            invokeCallback(callback);
        }
    }

    public AppOpenLifecycleObserver getAppOpenObserver() {
        return mAppOpenObserver;
    }

    public void load(String id, Context context, Callback success, Callback failure) {
        AdsManager.load(getAds(id, BaseAds.class), context, success, failure);
    }

    public void show(String id, Activity activity, Callback callback) {
        show(getAds(id, BaseFullScreenAds.class), activity, callback);
    }

    public void show(String id, Activity activity, Callback callback, Callback onUserEarnedReward) {
        show(getAds(id, BaseRewardedAds.class), activity, callback, onUserEarnedReward);
    }

    public void show(String id, ViewGroup container) {
        show(getAds(id, BaseViewAds.class), container);
    }

    public void hide(String id) {
        hide(getAds(id, BaseViewAds.class));
    }

    public boolean isCanLoadAds(String id) {
        return isCanLoadAds(getAds(id, BaseAds.class));
    }

    public boolean isCanShowAds(String id) {
        return isCanShowAds(getAds(id, BaseAds.class));
    }

    public boolean isPremium() {
        return mIsPremium.get();
    }

    public void setPremium(boolean isPremium) {
        mIsPremium.set(isPremium);
    }

    public boolean containsAds(String id) {
        return mAds.containsKey(id);
    }

    private void putAds(@NonNull Map<String, BaseAds<?>> ads) {
        for (Map.Entry<String, BaseAds<?>> entry : ads.entrySet()) {
            putAds(entry.getKey(), entry.getValue());
        }
    }

    public void putAds(BaseAds<?> ads) {
        if (ads != null) {
            putAds(ads.getAdsUnitId(), ads);
        }
    }

    public void putAds(String id, BaseAds<?> ads) {
        if (isInterstitialOrOpenAd(ads)) {
            ((BaseFullScreenAds<?>) ads).setOnDismissListener(a -> mAds.values().stream()
                    .filter(this::isInterstitialOrOpenAd)
                    .forEach(BaseAds::postDelayShowFlag)
            );
        }
        ads.setOnPaidEventListener(this::onPaidEvent);
        mAds.put(id, ads);
    }

    public void removeAds(String id) {
        mAds.remove(id);
    }

    public <A extends BaseAds<?>> A getAds(String id, @NonNull Class<A> clazz) {
        BaseAds<?> ads = mAds.get(id);
        if (clazz.isInstance(ads)) {
            return clazz.cast(ads);
        }
        return null;
    }

    private boolean isInterstitialOrOpenAd(BaseAds<?> ads) {
        return ads instanceof InterstitialAds || ads instanceof AppOpenAds;
    }

    private void onPaidEvent(AdValue adValue) {
        if (mOnPaidEventListener != null) {
            mOnPaidEventListener.onPaidEvent(adValue);
        }
    }

    /* --- Static methods --- */

    public static void load(BaseAds<?> ads, Context context, Callback success, Callback failure) {
        if (checkAdsCondition(ads, failure)) {
            ads.load(context, success, failure);
        }
    }

    public static void show(BaseFullScreenAds<?> ads, @NonNull Activity activity, Callback callback) {
        if (checkAdsCondition(ads, callback)) {
            ads.show(activity, handleFullScreenCallback(callback));
        }
    }

    public static void show(BaseRewardedAds<?> ads, @NonNull Activity activity, Callback callback, Callback onUserEarnedReward) {
        if (checkAdsCondition(ads, callback)) {
            ads.show(activity, handleFullScreenCallback(callback), onUserEarnedReward);
        }
    }

    public static void show(BaseViewAds<?> ads, @NonNull ViewGroup container) {
        if (checkAdsCondition(ads, null)) {
            ads.show(container);
        }
    }

    public static void hide(BaseViewAds<?> ads) {
        if (ads == null) {
            return;
        }
        ads.hide();
    }

    public static boolean isCanLoadAds(BaseAds<?> ads) {
        if (checkAdsCondition(ads, null)) {
            return ads.isCanLoadAds();
        }
        return false;
    }

    public static boolean isCanShowAds(BaseAds<?> ads) {
        if (checkAdsCondition(ads, null)) {
            return ads.isCanShowAds();
        }
        return false;
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
    private static Callback handleFullScreenCallback(Callback callback) {
        List<Pair<View, Integer>> viewAds = getBannerAndNativeViews();
        viewAds.forEach(view -> view.first.setVisibility(View.INVISIBLE));
        return () -> {
            viewAds.forEach(view -> view.first.setVisibility(view.second));
            invokeCallback(callback);
        };
    }

    @NonNull
    private static List<Pair<View, Integer>> getBannerAndNativeViews() {
        List<Pair<View, Integer>> viewAds = new ArrayList<>();
        for (BaseAds<?> ads : getInstance().mAds.values()) {
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
        return viewAds;
    }

    private static void invokeCallback(Callback callback) {
        if (callback != null) {
            callback.callback();
        }
    }
}
