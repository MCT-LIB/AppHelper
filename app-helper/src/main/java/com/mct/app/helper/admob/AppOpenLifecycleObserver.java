package com.mct.app.helper.admob;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.mct.app.helper.admob.ads.AppOpenAds;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppOpenLifecycleObserver {

    private AppOpenAds appOpenAds;
    private Activity currentActivity;

    private final LifecycleImpl lifecycleImpl = new LifecycleImpl();
    private final AtomicBoolean pendingShowAd = new AtomicBoolean();
    private final List<Class<? extends Activity>> blackListActivity = new ArrayList<>();

    public void init(@NonNull Application application, @NonNull AppOpenAds appOpenAds) {
        this.appOpenAds = appOpenAds;
        registerLifecycle(application);
    }

    public void release(Application application) {
        unregisterLifecycle(application);
    }

    public boolean isPendingShowAd() {
        return pendingShowAd.get();
    }

    public void pendingShowAd() {
        pendingShowAd.set(true);
    }

    public void removePendingShowAd() {
        pendingShowAd.set(false);
    }

    public void addBlackListActivity(Class<? extends Activity> activity) {
        blackListActivity.add(activity);
    }

    public void setBlackListActivity(List<Class<? extends Activity>> activity) {
        blackListActivity.clear();
        blackListActivity.addAll(activity);
    }

    private void registerLifecycle(@NonNull Application application) {
        unregisterLifecycle(application);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(lifecycleImpl);
        application.registerActivityLifecycleCallbacks(lifecycleImpl);
    }

    private void unregisterLifecycle(@NonNull Application application) {
        ProcessLifecycleOwner.get().getLifecycle().removeObserver(lifecycleImpl);
        application.unregisterActivityLifecycleCallbacks(lifecycleImpl);
    }

    private class LifecycleImpl implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
        /**
         * DefaultLifecycleObserver method that shows the app open ad when the app moves to foreground.
         */
        @Override
        public void onStart(@NonNull LifecycleOwner owner) {
            if (appOpenAds == null || currentActivity == null) {
                // Ads is not ready or activity is not resumed yet.
                return;
            }
            for (Class<? extends Activity> activity : blackListActivity) {
                if (activity.isInstance(currentActivity)) {
                    // Don't show app open ad when current activity is in the black list
                    return;
                }
            }
            if (pendingShowAd.get()) {
                pendingShowAd.set(false);
                return;
            }
            AdsManager.show(appOpenAds, currentActivity, null);
        }

        /* --- ActivityLifecycleCallback methods. --- */

        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            // An ad activity is started when an ad is showing, which could be AdActivity class from Google
            // SDK or another activity class implemented by a third party mediation partner. Updating the
            // currentActivity only when an ad is not showing will ensure it is not an ad activity, but the
            // one that shows the ad.
            if (appOpenAds != null && !appOpenAds.isShowing()) {
                currentActivity = activity;
            }
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
        }
    }

}
