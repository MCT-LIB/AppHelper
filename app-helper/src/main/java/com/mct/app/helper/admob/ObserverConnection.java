package com.mct.app.helper.admob;

import android.app.ActivityManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.mct.app.helper.admob.ads.AppOpenAds;
import com.mct.app.helper.admob.ads.BaseAds;
import com.mct.app.helper.admob.ads.InterstitialAds;
import com.mct.app.helper.admob.ads.NativeAds;
import com.mct.app.helper.admob.ads.NativeAdsPool;
import com.mct.app.helper.admob.ads.NativeFullScreenAds;
import com.mct.app.helper.admob.utils.DeviceChecker;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

class ObserverConnection {

    private final AtomicBoolean autoCheckDeviceWhenHasInternet = new AtomicBoolean(false);
    private final AtomicBoolean autoLoadFullscreenAdsWhenHasInternet = new AtomicBoolean(true);
    private final AtomicBoolean autoReloadFullscreenAdsWhenOrientationChanged = new AtomicBoolean(true);

    private final Handler handler = new Handler(Looper.getMainLooper());

    // network
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    // orientation
    private BroadcastReceiver orientationReceiver;

    public void init(@NonNull Application application) {
        registerObserver(application);
    }

    public void release(@NonNull Application application) {
        unregisterObserver(application);
    }

    public void setAutoCheckDeviceWhenHasInternet(boolean enable) {
        this.autoCheckDeviceWhenHasInternet.set(enable);
    }

    public void setAutoLoadFullscreenAdsWhenHasInternet(boolean enable) {
        this.autoLoadFullscreenAdsWhenHasInternet.set(enable);
    }

    public void setAutoReloadFullscreenAdsWhenOrientationChanged(boolean enable) {
        this.autoReloadFullscreenAdsWhenOrientationChanged.set(enable);
    }

    private void registerObserver(Application application) {
        if (connectivityManager == null) {
            connectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        if (networkCallback == null) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    if (autoCheckDeviceWhenHasInternet.get()) {
                        handler.post(() -> initCheckDevice(application));
                    }
                    if (autoLoadFullscreenAdsWhenHasInternet.get()) {
                        handler.post(() -> loadFullScreenAds(application));
                    }
                }
            });
        }

        if (orientationReceiver == null) {
            AtomicInteger lastOrientation = new AtomicInteger(Resources.getSystem().getConfiguration().orientation);
            application.registerReceiver(orientationReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int orientation = Resources.getSystem().getConfiguration().orientation;
                    if (orientation != lastOrientation.get()) {
                        lastOrientation.set(orientation);
                        if (autoReloadFullscreenAdsWhenOrientationChanged.get()) {
                            handler.post(() -> reloadFullScreenAds(application));
                        }
                    }
                }
            }, new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));
        }
    }

    private void unregisterObserver(Application application) {
        if (connectivityManager != null) {
            if (networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                networkCallback = null;
            }
            connectivityManager = null;
        }
        if (orientationReceiver != null) {
            application.unregisterReceiver(orientationReceiver);
            orientationReceiver = null;
        }
    }

    private void initCheckDevice(Application application) {
        // check device when has internet and has native ads
        AdsManager.getInstance().getAdsList().stream()
                .filter(ads -> ads instanceof NativeAds ||
                        ads instanceof NativeAdsPool ||
                        ads instanceof NativeFullScreenAds)
                .findFirst()
                .map(BaseAds::getLoadAdsUnitId)
                .ifPresent(unitId -> DeviceChecker.init(application, unitId));
    }

    // just load if not loaded
    private void loadFullScreenAds(Application application) {
        // load app open, interstitial and native ads pool
        AdsManager.getInstance().getAdsList().stream()
                .filter(ads -> ads instanceof AppOpenAds ||
                        ads instanceof InterstitialAds ||
                        ads instanceof NativeAdsPool)
                .forEach(ads -> {
                    if (ads.isCanLoadAds()) {
                        AdsManager.getInstance().load(ads, application, null, null);
                    }
                });
    }

    // clear and load
    private void reloadFullScreenAds(Application application) {
        Predicate<BaseAds<?>> check;
        if (isAppOnForeground(application)) {
            check = ads -> ads instanceof AppOpenAds || ads instanceof InterstitialAds;
        } else {
            check = ads -> ads instanceof AppOpenAds;
        }
        AdsManager.getInstance().getAdsList().stream()
                .filter(check)
                .forEach(ads -> {
                    ads.forceClear();
                    AdsManager.getInstance().load(ads, application, null, null);
                });
    }

    private boolean isAppOnForeground(@NonNull Context context) {
        ActivityManager activityManager = context.getSystemService(ActivityManager.class);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}
