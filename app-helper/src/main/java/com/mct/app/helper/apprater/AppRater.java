package com.mct.app.helper.apprater;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class AppRater {

    private static final String TAG = "AppRater";
    private static String marketLink;
    private static String packageName;
    private static Uri marketUri;
    private static Function<Context, RateDialog> rateDialogProvider;

    public static void setMarketLink(String marketLink) {
        AppRater.marketLink = marketLink;
    }

    public static void setPackageName(String packageName) {
        AppRater.packageName = packageName;
    }

    public static void setMarketUri(Uri marketUri) {
        AppRater.marketUri = marketUri;
    }

    public static void setRateDialogProvider(Function<Context, RateDialog> rateDialogProvider) {
        AppRater.rateDialogProvider = rateDialogProvider;
    }

    public static void launched(@NonNull Context context, @NonNull RateConfig config) {
        RatePreference preference = new RatePreference(context, config.getUniqueName());
        AppInfo ratingInfo = AppInfo.createApplicationInfo(context);

        // Check version name
        if (config.isCheckVersionName()) {
            if (!ratingInfo.getApplicationVersionName().equals(preference.getAppVersionName())) {
                preference.resetData();
                preference.setAppVersionName(ratingInfo.getApplicationVersionName());
            }
        }
        // Check version code
        if (config.isCheckVersionCode()) {
            if (ratingInfo.getApplicationVersionCode() != preference.getAppVersionCode()) {
                preference.resetData();
                preference.setAppVersionCode(ratingInfo.getApplicationVersionCode());
            }
        }
        // Check dont show again
        if (preference.getDontShowAgain()) {
            return;
        }
        // get condition values
        long millisecond, launches;
        if (preference.getRemindLater()) {
            millisecond = config.getTimeUntilPromptForRemindLater();
            launches = config.getLaunchesUntilPromptForRemindLater();
        } else {
            millisecond = config.getTimeUntilPrompt();
            launches = config.getLaunchesUntilPrompt();
        }

        // Increment launch counter
        long launchCount = preference.getLaunchCount() + 1;
        preference.setLaunchCount(launchCount);

        // Get date of first launch
        long firstLaunch = preference.getFirstLaunched();
        if (firstLaunch == 0) {
            firstLaunch = System.currentTimeMillis();
            preference.setFirstLaunched(firstLaunch);
        }

        // Check if it's time to show
        if (launchCount >= launches || System.currentTimeMillis() >= firstLaunch + millisecond) {
            if (AppRater.rateDialogProvider == null) {
                return;
            }

            // Check max shown times
            if (config.getMaxShownTimes() > 0) {
                int shownCount = preference.getShownTimes();
                if (shownCount >= config.getMaxShownTimes()) {
                    return;
                }
                preference.setShownTimes(shownCount + 1);
            }

            // Show dialog
            AtomicBoolean isPreferenceUpdated = new AtomicBoolean(false);
            RateDialog dialog = AppRater.rateDialogProvider.apply(context);
            dialog.setDontRemindButtonVisible(config.isShowDontRemind());
            dialog.setCancelable(config.isCancelable());
            dialog.setOnRateNowListener(() -> {
                rateNow(context);
                isPreferenceUpdated.set(true);
                preference.setDontShowAgain(true);
                dialog.dismiss();
            });
            dialog.setOnRemindLaterListener(() -> {
                isPreferenceUpdated.set(true);
                preference.resetData();
                preference.setRemindLater(true);
                dialog.dismiss();
            });
            dialog.setDontRemindListener(() -> {
                isPreferenceUpdated.set(true);
                preference.resetData();
                preference.setDontShowAgain(true);
                dialog.dismiss();
            });
            dialog.setOnDismissListener(() -> {
                if (isPreferenceUpdated.get()) {
                    return;
                }
                preference.resetData();
                preference.setRemindLater(true);
            });
            dialog.show();
        }
    }

    /**
     * Call this method directly if you want
     */
    public static void rateNow(@NonNull Context context) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, getMarketUri(context)));
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Market Intent not found", e);
        }
    }

    private static Uri getMarketUri(Context context) {
        if (AppRater.marketUri != null) {
            return AppRater.marketUri;
        } else {
            String mktLink = AppRater.marketLink != null ? AppRater.marketLink : "market://details?id=";
            String pkgName = AppRater.packageName != null ? AppRater.packageName : context.getPackageName();
            return Uri.parse(mktLink + pkgName);
        }
    }
}
