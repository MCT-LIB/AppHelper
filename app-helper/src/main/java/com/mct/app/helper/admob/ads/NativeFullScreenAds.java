package com.mct.app.helper.admob.ads;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.mct.app.helper.R;
import com.mct.app.helper.admob.ads.natives.NativeTemplate;
import com.mct.app.helper.admob.ads.natives.NativeTemplateStyle;
import com.mct.app.helper.admob.ads.natives.NativeTemplateView;

public class NativeFullScreenAds extends BaseFullScreenAds<NativeAd> {

    private final int layoutRes;
    private NativeTemplateStyle templateStyle;

    private FullScreenDialog dialog;

    public NativeFullScreenAds(String adsUnitId) {
        this(adsUnitId, NativeTemplate.MEDIUM_1);
    }

    public NativeFullScreenAds(String adsUnitId, @NonNull NativeTemplate nativeTemplate) {
        super(adsUnitId, 0);
        this.layoutRes = nativeTemplate.layoutRes;
    }

    public NativeFullScreenAds(String adsUnitId, @LayoutRes int layoutRes) {
        super(adsUnitId, 0);
        this.layoutRes = layoutRes;
    }

    public void setTemplateStyle(NativeTemplateStyle templateStyle) {
        this.templateStyle = templateStyle;
    }

    @Override
    protected void onLoadAds(@NonNull Context context, @NonNull AdsLoadCallback<NativeAd> callback) {
        new AdLoader.Builder(context, getLoadAdsUnitId())
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .setVideoOptions(new VideoOptions.Builder().setStartMuted(true).build())
                        .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT)
                        .build())
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        callback.onAdsFailedToLoad(loadAdError);
                    }
                })
                .forNativeAd(nativeAd -> {
                    nativeAd.setOnPaidEventListener(getOnPaidEventListener());
                    callback.onAdsLoaded(nativeAd);
                })
                .build()
                .loadAd(getAdRequest());
    }

    @Override
    protected void onShowAds(@NonNull Activity activity, @NonNull NativeAd nativeAd, @NonNull FullScreenContentCallback callback) {
        NativeTemplateView templateView = new NativeTemplateView(activity, layoutRes);
        templateView.setStyles(templateStyle);
        templateView.setNativeAd(nativeAd);
        dialog = new FullScreenDialog(activity, templateView, callback);
        dialog.show();
    }

    private static class FullScreenDialog extends Dialog {

        private FullScreenContentCallback callback;

        public FullScreenDialog(@NonNull Context context,
                                NativeTemplateView templateView,
                                FullScreenContentCallback callback) {
            super(context, R.style.Gnt_AlertDialog_FullScreen);
            this.callback = callback;
            initWindow(getWindow());
            setContentView(templateView, new ViewGroup.LayoutParams(-1, -1));
            setOnDismissListener(d -> callback.onAdDismissedFullScreenContent());
            // TODO: 12/12/2024  Handle callback
        }

        private static void initWindow(Window window) {
            if (window == null) {
                return;
            }
            WindowCompat.setDecorFitsSystemWindows(window, true);
            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);
            window.setStatusBarColor(Color.BLACK);
            window.setNavigationBarColor(Color.BLACK);
        }
    }


}
