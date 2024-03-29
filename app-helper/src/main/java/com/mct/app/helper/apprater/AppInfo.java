package com.mct.app.helper.apprater;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

class AppInfo {

    private String applicationName;
    private String applicationVersionName;
    private int applicationVersionCode;

    public String getApplicationName() {
        return applicationName;
    }

    public String getApplicationVersionName() {
        return applicationVersionName;
    }

    public int getApplicationVersionCode() {
        return applicationVersionCode;
    }

    private AppInfo() {
    }

    @NonNull
    public static AppInfo createApplicationInfo(@NonNull Context context) {
        PackageManager packageManager = context.getPackageManager();
        AppInfo resultInfo = new AppInfo();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getApplicationInfo().packageName, 0);
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getApplicationInfo().packageName, 0);
            resultInfo.applicationName = packageManager.getApplicationLabel(applicationInfo).toString();
            resultInfo.applicationVersionName = packageInfo.versionName;
            resultInfo.applicationVersionCode = packageInfo.versionCode;
        } catch (final PackageManager.NameNotFoundException ignored) {
        }
        return resultInfo;
    }
}