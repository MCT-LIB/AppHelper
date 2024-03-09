package com.mct.app.helper.apprater;


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Supplier;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

/**
 * @noinspection unused
 */
public class AppRater {

    private static final String TAG = "AppRater";

    // Preference Constants
    private final static String PREF_NAME = "AppRater";
    private final static String PREF_LAUNCH_COUNT = "launch_count";
    private final static String PREF_FIRST_LAUNCHED = "date_first_launch";
    private final static String PREF_DONT_SHOW_AGAIN = "dont_show_again";
    private final static String PREF_REMIND_LATER = "remind_me_later";
    private final static String PREF_APP_VERSION_NAME = "app_version_name";
    private final static String PREF_APP_VERSION_CODE = "app_version_code";

    private final static int DAYS_UNTIL_PROMPT = 3;
    private final static int LAUNCHES_UNTIL_PROMPT = 7;
    private static int DAYS_UNTIL_PROMPT_FOR_REMIND_LATER = 3;
    private static int LAUNCHES_UNTIL_PROMPT_FOR_REMIND_LATER = 7;
    private static boolean hideNoButton;
    private static boolean isVersionNameCheckEnabled;
    private static boolean isVersionCodeCheckEnabled;
    private static boolean isCancelable = true;

    private static Market market = new GoogleMarket();
    private static Supplier<RateDialog> rateDialogSupplier;

    /**
     * Decides if the version name check is active or not
     */
    public static void setVersionNameCheckEnabled(boolean versionNameCheck) {
        isVersionNameCheckEnabled = versionNameCheck;
    }

    /**
     * Decides if the version code check is active or not
     */
    public static void setVersionCodeCheckEnabled(boolean versionCodeCheck) {
        isVersionCodeCheckEnabled = versionCodeCheck;
    }

    /**
     * sets number of day until rating dialog pops up for next time when remind
     * me later option is chosen
     */
    public static void setNumDaysForRemindLater(int daysUntilPromt) {
        DAYS_UNTIL_PROMPT_FOR_REMIND_LATER = daysUntilPromt;
    }

    /**
     * sets the number of launches until the rating dialog pops up for next time
     * when remind me later option is chosen
     */
    public static void setNumLaunchesForRemindLater(int launchesUntilPrompt) {

        LAUNCHES_UNTIL_PROMPT_FOR_REMIND_LATER = launchesUntilPrompt;
    }

    /**
     * decides if No thanks button appear in dialog or not
     */
    public static void setDontRemindButtonVisible(boolean isNoButtonVisible) {
        AppRater.hideNoButton = isNoButtonVisible;
    }

    /**
     * sets whether the rating dialog is cancelable or not, default is true.
     */
    public static void setCancelable(boolean cancelable) {
        isCancelable = cancelable;
    }

    public static void setRateDialogSupplier(Supplier<RateDialog> rateDialogSupplier) {
        AppRater.rateDialogSupplier = rateDialogSupplier;
    }

    /**
     * Call this method at the end of your OnCreate method to determine whether
     * to show the rate prompt using the specified or default day, launch count
     * values and checking if the version is changed or not
     */
    public static void app_launched(Context context) {
        app_launched(context, DAYS_UNTIL_PROMPT, LAUNCHES_UNTIL_PROMPT);
    }

    /**
     * Call this method at the end of your OnCreate method to determine whether
     * to show the rate prompt using the specified or default day, launch count
     * values with additional day and launch parameter for remind me later option
     * and checking if the version is changed or not
     */
    public static void app_launched(Context context, int daysUntilPrompt, int launchesUntilPrompt, int daysForRemind, int launchesForRemind) {
        setNumDaysForRemindLater(daysForRemind);
        setNumLaunchesForRemindLater(launchesForRemind);
        app_launched(context, daysUntilPrompt, launchesUntilPrompt);
    }

    /**
     * Call this method at the end of your OnCreate method to determine whether
     * to show the rate prompt
     */
    public static void app_launched(Context context, int daysUntilPrompt, int launchesUntilPrompt) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        ApplicationRatingInfo ratingInfo = ApplicationRatingInfo.createApplicationInfo(context);
        int days;
        int launches;
        if (isVersionNameCheckEnabled) {
            if (!ratingInfo.getApplicationVersionName().equals(prefs.getString(PREF_APP_VERSION_NAME, "none"))) {
                resetData(context);
                editor.putString(PREF_APP_VERSION_NAME, ratingInfo.getApplicationVersionName());
                editor.apply();
            }
        }
        if (isVersionCodeCheckEnabled) {
            if (ratingInfo.getApplicationVersionCode() != (prefs.getInt(PREF_APP_VERSION_CODE, -1))) {
                resetData(context);
                editor.putInt(PREF_APP_VERSION_CODE, ratingInfo.getApplicationVersionCode());
                editor.apply();
            }
        }
        if (prefs.getBoolean(PREF_DONT_SHOW_AGAIN, false)) {
            return;
        } else if (prefs.getBoolean(PREF_REMIND_LATER, false)) {
            days = DAYS_UNTIL_PROMPT_FOR_REMIND_LATER;
            launches = LAUNCHES_UNTIL_PROMPT_FOR_REMIND_LATER;
        } else {
            days = daysUntilPrompt;
            launches = launchesUntilPrompt;
        }

        // Increment launch counter
        long launch_count = prefs.getLong(PREF_LAUNCH_COUNT, 0) + 1;
        editor.putLong(PREF_LAUNCH_COUNT, launch_count);
        // Get date of first launch
        long date_firstLaunch = prefs.getLong(PREF_FIRST_LAUNCHED, 0);
        if (date_firstLaunch == 0) {
            date_firstLaunch = System.currentTimeMillis();
            editor.putLong(PREF_FIRST_LAUNCHED, date_firstLaunch);
        }
        editor.apply();
        // Wait for at least the number of launches or the number of days used
        // until prompt
        if (launch_count >= launches || (System.currentTimeMillis() >= date_firstLaunch + ((long) days * 24 * 60 * 60 * 1000))) {
            if (rateDialogSupplier != null) {
                RateDialog dialog = AppRater.rateDialogSupplier.get();
                dialog.setDontRemindButtonVisible(AppRater.hideNoButton);
                dialog.setCancelable(AppRater.isCancelable);
                dialog.setOnRateNowListener(() -> {
                    rateNow(context);
                    editor.putBoolean(PREF_DONT_SHOW_AGAIN, true).apply();
                    dialog.dismiss();
                });
                dialog.setOnRateLaterListener(() -> {
                    editor.putLong(PREF_FIRST_LAUNCHED, System.currentTimeMillis());
                    editor.putLong(PREF_LAUNCH_COUNT, 0);
                    editor.putBoolean(PREF_DONT_SHOW_AGAIN, false);
                    editor.putBoolean(PREF_REMIND_LATER, true);
                    editor.apply();
                    dialog.dismiss();
                });
                dialog.setOnNoThanksListener(() -> {
                    editor.putLong(PREF_FIRST_LAUNCHED, System.currentTimeMillis());
                    editor.putLong(PREF_LAUNCH_COUNT, 0);
                    editor.putBoolean(PREF_DONT_SHOW_AGAIN, true);
                    editor.putBoolean(PREF_REMIND_LATER, false);
                    editor.apply();
                    dialog.dismiss();
                });
                dialog.show();
            }
        }
    }

    /**
     * Call this method directly if you want to show the rate prompt immediately
     */
    public static void rateNow(@NonNull final Context context) {
        if (context instanceof Activity) {
            ReviewManager manager = ReviewManagerFactory.create(context);
            Task<ReviewInfo> request = manager.requestReviewFlow();
            request.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // We can get the ReviewInfo object
                    ReviewInfo reviewInfo = task.getResult();
                    Task<Void> flow = manager.launchReviewFlow((Activity) context, reviewInfo);
                    flow.addOnCompleteListener(task1 -> {
                        // The flow has finished. The API does not indicate whether the user
                        // reviewed or not, or even whether the review dialog was shown. Thus, no
                        // matter the result, we continue our app flow.
                    });
                } else {
                    // There was some problem, log or handle the error code.
                    Log.e(TAG, "rateNow: ", task.getException());
                    rateNow2(context);
                }
            });
        } else {
            rateNow2(context);
        }
    }

    /**
     * Call this method directly if you want to show the rate prompt immediately
     */
    public static void rateNow2(@NonNull Context context) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, market.getMarketURI(context)));
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Market Intent not found", e);
        }
    }

    public static void setPackageName(String packageName) {
        AppRater.market.overridePackageName(packageName);
    }

    /**
     * Set an alternate Market, defaults to Google Play
     */
    public static void setMarket(Market market) {
        AppRater.market = market;
    }

    /**
     * Get the currently set Market
     *
     * @return market
     */
    public static Market getMarket() {
        return market;
    }

    public static void resetData(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_DONT_SHOW_AGAIN, false);
        editor.putBoolean(PREF_REMIND_LATER, false);
        editor.putLong(PREF_LAUNCH_COUNT, 0);
        editor.putLong(PREF_FIRST_LAUNCHED, System.currentTimeMillis());
        editor.apply();
    }
}