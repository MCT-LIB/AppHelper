package com.mct.app.helper.apprater;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

class RatePreference {

    private final static String PREF_NAME = "AppRater";

    private final String uniqueName;
    private final SharedPreferences prefs;

    public RatePreference(@NonNull Context context, String name) {
        this.uniqueName = name;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public long getLaunchCount() {
        return prefs.getLong(_keyPrefLaunchCount(), 0);
    }

    public void setLaunchCount(long value) {
        editor().putLong(_keyPrefLaunchCount(), value).apply();
    }

    public long getFirstLaunched() {
        return prefs.getLong(_keyPrefFirstLaunched(), 0);
    }

    public void setFirstLaunched(long value) {
        editor().putLong(_keyPrefFirstLaunched(), value).apply();
    }

    public int getShownTimes() {
        return prefs.getInt(_keyPrefShownTimes(), 0);
    }

    public void setShownTimes(int value) {
        editor().putInt(_keyPrefShownTimes(), value).apply();
    }

    public boolean getDontShowAgain() {
        return prefs.getBoolean(_keyPrefDontShowAgain(), false);
    }

    public void setDontShowAgain(boolean value) {
        editor().putBoolean(_keyPrefDontShowAgain(), value).apply();
    }

    public boolean getRemindLater() {
        return prefs.getBoolean(_keyPrefRemindLater(), false);
    }

    public void setRemindLater(boolean value) {
        editor().putBoolean(_keyPrefRemindLater(), value).apply();
    }

    public String getAppVersionName() {
        return prefs.getString(_keyPrefAppVersionName(), "none");
    }

    public void setAppVersionName(String value) {
        editor().putString(_keyPrefAppVersionName(), value).apply();
    }

    public int getAppVersionCode() {
        return prefs.getInt(_keyPrefAppVersionCode(), -1);
    }

    public void setAppVersionCode(int value) {
        editor().putInt(_keyPrefAppVersionCode(), value).apply();
    }

    public void resetData() {
        editor().putLong(_keyPrefFirstLaunched(), System.currentTimeMillis())
                .putLong(_keyPrefLaunchCount(), 0)
                .putBoolean(_keyPrefDontShowAgain(), false)
                .putBoolean(_keyPrefRemindLater(), false)
                .apply();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private area
    ///////////////////////////////////////////////////////////////////////////

    private SharedPreferences.Editor editor() {
        return prefs.edit();
    }

    private @NonNull String _keyPrefLaunchCount() {
        return "launch_count_" + uniqueName;
    }

    private @NonNull String _keyPrefFirstLaunched() {
        return "date_first_launch_" + uniqueName;
    }

    private @NonNull String _keyPrefShownTimes() {
        return "shown_times_" + uniqueName;
    }

    private @NonNull String _keyPrefDontShowAgain() {
        return "dont_show_again_" + uniqueName;
    }

    private @NonNull String _keyPrefRemindLater() {
        return "remind_me_later_" + uniqueName;
    }

    private @NonNull String _keyPrefAppVersionName() {
        return "app_version_name_" + uniqueName;
    }

    private @NonNull String _keyPrefAppVersionCode() {
        return "app_version_code_" + uniqueName;
    }
}
