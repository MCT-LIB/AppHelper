package com.mct.app.helper.admob.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DeviceChecker {

    private static Boolean isEmulator;
    private static Boolean isTestDevice;
    private static TestDeviceChecker testDeviceChecker;

    public static boolean isRealDevice() {
        if (!isInit()) {
            return false;
        }
        return !isEmulator && !isTestDevice;
    }

    public static boolean isInit() {
        return isEmulator != null && isTestDevice != null;
    }

    public static void init(Context context, String nativeId) {
        // already initialized
        if (isInit()) {
            return;
        }
        // check emulator
        if (isEmulator == null) {
            isEmulator = checkEmulator();
        }
        // check test device
        if (testDeviceChecker == null) {
            testDeviceChecker = new TestDeviceChecker();
        }
        testDeviceChecker.checkTestDevice(context, nativeId, result -> {
            testDeviceChecker = null;
            isTestDevice = result;
        }, error -> {
            testDeviceChecker = null;
            isTestDevice = null;
        });
    }

    /* --- emulator check --- */

    private static boolean checkEmulator() {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HOST.startsWith("Build")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("sdk_gphone64_arm64")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
                || SystemProperties.getInt("ro.kernel.qemu", 0) == 1;
    }

    private static class SystemProperties {

        public static String get(String key) {
            return get(key, "", String.class);
        }

        public static String get(String key, String def) {
            return get(key, def, String.class);
        }

        public static int getInt(String key, int def) {
            return get(key, def, int.class);
        }

        public static long getLong(String key, long def) {
            return get(key, def, long.class);
        }

        public static boolean getBoolean(String key, boolean def) {
            return get(key, def, boolean.class);
        }

        private static <V> V get(String key, V def, Class<V> valueType) {
            Object value = null;
            try {
                // try to call method
                Method method = Objects.requireNonNull(getPropMethod(valueType));
                value = method.invoke(null, key, def);
            } catch (Exception ignored) {
                // try to call process
                Process process = null;
                try {
                    process = Runtime.getRuntime().exec("getprop " + key + " " + def);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    value = reader.readLine();
                } catch (Exception ignored1) {
                } finally {
                    if (process != null) {
                        process.destroy();
                    }
                }
            }
            return cast(value, def, valueType);
        }

        private static final Map<Class<?>, Method> propMethods = new HashMap<>();

        @Nullable
        private static Method getPropMethod(Class<?> valueType) {
            // check if method is cached
            if (propMethods.containsKey(valueType)) {
                return propMethods.get(valueType);
            }

            String methodName;
            if (valueType == String.class) methodName = "get";
            else if (valueType == int.class) methodName = "getInt";
            else if (valueType == long.class) methodName = "getLong";
            else if (valueType == boolean.class) methodName = "getBoolean";
            else throw new IllegalArgumentException("Unsupported valueType: " + valueType);

            Class<?> keyType = String.class;

            try {
                @SuppressLint("PrivateApi")
                Class<?> clazz = Class.forName("android.os.SystemProperties");
                Method method = clazz.getMethod(
                        /* name */ methodName,
                        /* key */  keyType,
                        /* def */  valueType
                );
                propMethods.put(valueType, method);
                return method;
            } catch (Exception e) {
                propMethods.put(valueType, null);
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        private static <V> V cast(Object value, V def, Class<V> c) {
            try {
                if (value == null) return def;
                if (c.isInstance(value)) return c.cast(value);
                if (c == String.class) return c.cast(value.toString());
                if (c == int.class) return (V) Integer.valueOf(value.toString());
                if (c == long.class) return (V) Long.valueOf(value.toString());
                if (c == boolean.class) return (V) Boolean.valueOf(value.toString());
            } catch (NumberFormatException | ClassCastException ignored) {
            }
            return def;
        }
    }

    /* --- test device check --- */

    private static class TestDeviceChecker {
        private AdLoader adLoader;

        public void checkTestDevice(Context context, String nativeId, Consumer<Boolean> onSuccess, Consumer<Throwable> onFailure) {
            // ad is already loading
            if (adLoader != null && adLoader.isLoading()) {
                return;
            }

            // create ad loader
            adLoader = new AdLoader.Builder(context, nativeId)
                    .withAdListener(new AdListener() {
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            onFailure.accept(new RuntimeException(loadAdError.getMessage()));
                        }
                    })
                    .forNativeAd(nativeAd -> {
                        String t1 = "Test Ad";
                        String t2 = context.getString(com.google.android.gms.ads.impl.R.string.s7);
                        String header = nativeAd.getHeadline();
                        nativeAd.destroy();
                        onSuccess.accept(Optional.ofNullable(header)
                                .map(String::toLowerCase)
                                .map(String::trim)
                                .map(h -> h.startsWith(t1) || h.startsWith(t2))
                                .orElse(false));
                    })
                    .build();

            // load ad
            adLoader.loadAd(new AdRequest.Builder().build());
        }

    }

}
