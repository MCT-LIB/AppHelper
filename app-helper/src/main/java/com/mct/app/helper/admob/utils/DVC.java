package com.mct.app.helper.admob.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * - Call either function in splash screen to initialize device checker<br/>
 * {@link DVC#init(Context, String)}<br/>
 * {@link DVC#init(Context, NativeAd)}<br/>
 * <br/>
 * - Call {@link DVC#isRdv()} to check if device is real device<br/>
 */
public class DVC {

    private static Boolean isEMU;
    private static Boolean isTDV;
    private static TDV tdv;

    public static boolean isRdv(boolean debug) {
        return debug || isRdvInternal();
    }

    public static boolean isRdv() {
        return isRdvInternal();
    }

    private static boolean isRdvInternal() {
        if (!isInit()) {
            return false;
        }
        return !isEMU && !isTDV;
    }

    public static boolean isInit() {
        return isEMU != null && isTDV != null;
    }

    public static void init(@NonNull Context context, @NonNull String nativeId) {
        initInternal(context, nativeId, null);
    }

    public static void init(@NonNull Context context, @NonNull NativeAd nativeAd) {
        initInternal(context, null, nativeAd);
    }

    private static void initInternal(@NonNull Context context, @Nullable String nativeId, @Nullable NativeAd nativeAd) {
        // already initialized
        if (isInit()) {
            return;
        }
        // check emu
        if (isEMU == null) {
            isEMU = checkEMU();
        }
        // check tdv
        if (tdv == null) {
            tdv = new TDV();
        }
        Consumer<Boolean> onSuccess = result -> {
            tdv = null;
            isTDV = result;
        };
        Consumer<Throwable> onFailure = error -> {
            tdv = null;
            isTDV = null;
        };
        if (nativeId != null) {
            tdv.checkTDV(context.getApplicationContext(), nativeId, onSuccess, onFailure);
        }
        if (nativeAd != null) {
            tdv.checkTDV(context.getApplicationContext(), nativeAd, onSuccess, onFailure);
        }
    }

    /* --- emulator check --- */

    /**
     * @noinspection SpellCheckingInspection
     */
    private static boolean checkEMU() {
        return (Build.BRAND.startsWith(XOR.decrypt("DhQPEBANCg==", "aXFhdWJkaXY="))         // generic
                && Build.DEVICE.startsWith(XOR.decrypt("AwsLER4YCg==", "ZG5ldGxxaXE=")))        // generic
                || Build.FINGERPRINT.startsWith(XOR.decrypt("Ah8JHBEIBg==", "ZXpneWNhZXU="))    // generic
                || Build.FINGERPRINT.startsWith(XOR.decrypt("GhQABgAVGA==", "b3praG9idmk="))    // unknown
                || Build.HARDWARE.contains(XOR.decrypt("HQQWHR8TBBA=", "emt6eXl6d3g="))         // goldfish
                || Build.HARDWARE.contains(XOR.decrypt("EBYGDxEc", "YndobHlpZ3k="))             // ranchu
                || Build.MODEL.contains(XOR.decrypt("DRcVCAMEOAAOEw==", "anh6b29hZ3M="))        // google_sdk
                || Build.MODEL.contains(XOR.decrypt("JgMeGREDFRQ=", "Y25rdXB3emY="))            // Emulator
                || Build.MODEL.contains(XOR.decrypt("KxsJGRUYBVM5MSZLGAQIHx5VCwQIURlLXA==", "anVta3pxYXM="))    // Android SDK built for x86
                || Build.MANUFACTURER.contains(XOR.decrypt("LAAJFxwBBQIECw==", "a2VnbnFucWs=")) // Genymotion
                || Build.HOST.startsWith(XOR.decrypt("IwAEGBU=", "YXVtdHF5ZHU="))               // Build
                || Build.PRODUCT.contains(XOR.decrypt("EAEbOAkMFxUPAA==", "Y2VwZ25jeHI="))      // sdk_google
                || Build.PRODUCT.contains(XOR.decrypt("DAMABBYOOxkPBw==", "a2xvY3prZGo="))      // google_sdk
                || Build.PRODUCT.contains(XOR.decrypt("GAsB", "a29qYnpic3I="))                  // sdk
                || Build.PRODUCT.contains(XOR.decrypt("HgYFLxdRQQ==", "bWJucG9pd2U="))          // sdk_x86
                || Build.PRODUCT.contains(XOR.decrypt("EgUYLx0TBAkPBEVEJQIeC1dV", "YWFzcHpjbGY="))  // sdk_gphone64_arm64
                || Build.PRODUCT.contains(XOR.decrypt("AAkaHEBXGQ==", "dmt1ZHhhaWQ="))          // vbox86p
                || Build.PRODUCT.contains(XOR.decrypt("FRoXGAIGAhs=", "cHdidGNybWk="))          // emulator
                || Build.PRODUCT.contains(XOR.decrypt("BAEdEQkAFgIF", "d2hwZGVhYm0="))          // simulator
                || Build.MODEL.contains(XOR.decrypt("OAUVAAY=", "aGxtZWpkZGQ="))                // ₱łӾɆⱠ
                || SystemProperties.getInt(XOR.decrypt("HAJWDBwRDxUCQwkCFBY=", "bm14Z3ljYXA="), 0) == 1 // ro.kernel.qemu
                ;
    }

    /* --- test device check --- */

    private static class TDV {

        private AdLoader adLoader;

        public void checkTDV(@NonNull Context context,
                             @NonNull String nativeId,
                             @NonNull Consumer<Boolean> onSuccess,
                             @NonNull Consumer<Throwable> onFailure) {

            Objects.requireNonNull(context);
            Objects.requireNonNull(nativeId);
            Objects.requireNonNull(onSuccess);
            Objects.requireNonNull(onFailure);

            // ad is already loading
            if (adLoader != null && adLoader.isLoading()) {
                return;
            }

            // create ad loader
            adLoader = new AdLoader.Builder(context, nativeId)
                    .withNativeAdOptions(new NativeAdOptions.Builder()
                            .setVideoOptions(new VideoOptions.Builder().setStartMuted(true).build())
                            .build())
                    .withAdListener(new AdListener() {
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            onFailure(loadAdError, onFailure);
                        }
                    })
                    .forNativeAd(nativeAd -> {
                        onSuccess(context, nativeAd.getHeadline(), onSuccess);
                        nativeAd.destroy();
                    })
                    .build();

            // load ad
            adLoader.loadAd(new AdRequest.Builder().build());
        }

        public void checkTDV(@NonNull Context context,
                             @NonNull NativeAd nativeAd,
                             @NonNull Consumer<Boolean> onSuccess,
                             @NonNull Consumer<Throwable> onFailure) {

            Objects.requireNonNull(context);
            Objects.requireNonNull(nativeAd);
            Objects.requireNonNull(onSuccess);
            Objects.requireNonNull(onFailure);

            onSuccess(context, nativeAd.getHeadline(), onSuccess);
        }

        private static void onFailure(@NonNull LoadAdError error, @NonNull Consumer<Throwable> onFailure) {
            onFailure.accept(new RuntimeException(error.getMessage()));
        }

        private static void onSuccess(@NonNull Context context, @Nullable String hl, @NonNull Consumer<Boolean> onSuccess) {
            onSuccess.accept(Optional.ofNullable(hl)
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .map(h -> h.startsWith(aa(context, false)) || h.startsWith(aa(context, true)))
                    .orElse(false));
        }

        @NonNull
        private static String aa(Context ctx, boolean r) {
            String result = null;
            if (r) {
                try {
                    String id = cc(bb(61, 99, 122, 85));
                    String tp = bb(83, 116, 82, 105, 78, 103);
                    result = ctx.getString(ctx.getResources().getIdentifier(id, tp.toLowerCase(), ctx.getPackageName()));
                } catch (Exception ignored) {
                }
            }
            if (result == null) {
                result = cc(bb(61, 61, 65, 82, 66, 66, 67, 100, 84, 86, 71, 86));
            }
            return result.toLowerCase();
        }

        @NonNull
        private static String bb(@NonNull int... chars) {
            return Optional.of(new StringBuilder())
                    .map(s -> {
                        for (int c : chars) s.append((char) c);
                        return s;
                    })
                    .map(StringBuilder::toString)
                    .orElse("");
        }

        private static String cc(String input) {
            return Optional.of(new StringBuilder(input))
                    .map(StringBuilder::reverse)
                    .map(StringBuilder::toString)
                    .map(s -> Base64.decode(s, Base64.DEFAULT))
                    .map(String::new)
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .orElse("");
        }
    }

    /* --- Utils --- */

    /**
     * @noinspection unused
     */
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

    /**
     * @noinspection unused
     */
    private static class XOR {

        public static String decrypt(String data, String key) {
            byte[] decoded = Base64.decode(data, Base64.DEFAULT);
            byte[] secret = Base64.decode(key, Base64.DEFAULT);
            return decrypt(decoded, secret);
        }

        public static String encrypt(String data, String key) {
            byte[] secret = Base64.decode(key, Base64.DEFAULT);
            byte[] encoded = encrypt(data, secret);
            return Base64.encodeToString(encoded, Base64.DEFAULT);
        }

        private static byte[] encrypt(String data, byte[] key) {
            return xor(data.getBytes(StandardCharsets.UTF_8), key);
        }

        private static String decrypt(byte[] data, byte[] key) {
            return new String(xor(data, key), StandardCharsets.UTF_8);
        }

        private static byte[] xor(byte[] data, byte[] key) {
            int len = data.length;
            int lenKey = key.length;
            int i = 0;
            int j = 0;
            while (i < len) {
                if (j >= lenKey) {
                    j = 0;
                }
                data[i] = (byte) (data[i] ^ key[j]);
                i++;
                j++;
            }
            return data;
        }

        private XOR() {
            //no instance
        }
    }

}
