package com.mct.app.helper.admob.ads;

import static com.mct.app.helper.admob.ads.NativeFullScreenAds.DismissButtonGravity.GRAVITY_RANDOM;
import static com.mct.app.helper.admob.ads.NativeFullScreenAds.DismissButtonGravity.GRAVITY_TOP_END;
import static com.mct.app.helper.admob.ads.NativeFullScreenAds.DismissButtonGravity.GRAVITY_TOP_START;
import static com.mct.app.helper.admob.ads.NativeFullScreenAds.MediaRatioOptions.MEDIA_RATIO_ANY;
import static com.mct.app.helper.admob.ads.NativeFullScreenAds.MediaRatioOptions.MEDIA_RATIO_LANDSCAPE;
import static com.mct.app.helper.admob.ads.NativeFullScreenAds.MediaRatioOptions.MEDIA_RATIO_PORTRAIT;
import static com.mct.app.helper.admob.ads.NativeFullScreenAds.MediaRatioOptions.MEDIA_RATIO_SQUARE;
import static com.mct.app.helper.admob.ads.NativeFullScreenAds.MediaRatioOptions.MEDIA_RATIO_UNKNOWN;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.mct.app.helper.admob.ads.natives.NativeTemplateStyle;
import com.mct.app.helper.admob.ads.natives.NativeTemplateView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class NativeFullScreenAds extends BaseFullScreenAds<NativeAd> {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({GRAVITY_TOP_START, GRAVITY_TOP_END, GRAVITY_RANDOM})
    public @interface DismissButtonGravity {
        int GRAVITY_TOP_START = Gravity.TOP | Gravity.START;
        int GRAVITY_TOP_END = Gravity.TOP | Gravity.END;
        int GRAVITY_RANDOM = -1;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MEDIA_RATIO_UNKNOWN, MEDIA_RATIO_ANY, MEDIA_RATIO_LANDSCAPE, MEDIA_RATIO_PORTRAIT, MEDIA_RATIO_SQUARE})
    public @interface MediaRatioOptions {
        int MEDIA_RATIO_UNKNOWN = NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_UNKNOWN;
        int MEDIA_RATIO_ANY = NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_ANY;
        int MEDIA_RATIO_LANDSCAPE = NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE;
        int MEDIA_RATIO_PORTRAIT = NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT;
        int MEDIA_RATIO_SQUARE = NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_SQUARE;
    }

    private final int layoutRes;
    private NativeTemplateStyle templateStyle;

    private int dismissButtonGravity = GRAVITY_TOP_END;
    private long showDismissButtonCountdown = 3000;
    private boolean cancelable = false;
    private boolean startMuted = true;
    private int mediaRatioOptions = MEDIA_RATIO_ANY;

    private FullScreenDialog fullScreenDialog;

    public NativeFullScreenAds(String adsUnitId, long adsInterval, @LayoutRes int layoutRes) {
        super(adsUnitId, adsInterval);
        this.layoutRes = layoutRes;
    }

    public void setTemplateStyle(NativeTemplateStyle templateStyle) {
        this.templateStyle = templateStyle;
    }

    public void setDismissButtonGravity(@DismissButtonGravity int gravity) {
        this.dismissButtonGravity = gravity;
    }

    public void setShowDismissButtonCountdown(long countdown) {
        this.showDismissButtonCountdown = countdown;
    }

    public void setCancelable(boolean cancelable) {
        this.cancelable = cancelable;
    }

    public void setStartMuted(boolean startMuted) {
        this.startMuted = startMuted;
    }

    public void setMediaRatioOptions(@MediaRatioOptions int mediaRatioOptions) {
        this.mediaRatioOptions = mediaRatioOptions;
    }

    public final void hide() {
        if (fullScreenDialog != null && fullScreenDialog.isShowing()) {
            fullScreenDialog.dismiss();
            fullScreenDialog = null;
        }
    }

    @Override
    protected void onLoadAds(@NonNull Context context, @NonNull AdsLoadCallback<NativeAd> callback) {
        new AdLoader.Builder(context, getLoadAdsUnitId())
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .setVideoOptions(new VideoOptions.Builder().setStartMuted(startMuted).build())
                        .setMediaAspectRatio(mediaRatioOptions)
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
        fullScreenDialog = new FullScreenDialog(activity, templateView, dismissButtonGravity, showDismissButtonCountdown) {
            @Override
            public void onShow(DialogInterface dialog) {
                super.onShow(dialog);
                callback.onAdShowedFullScreenContent();
            }

            @Override
            public void onDismiss(DialogInterface dialog) {
                super.onDismiss(dialog);
                templateView.destroyNativeAd();
                callback.onAdDismissedFullScreenContent();
                fullScreenDialog = null;
            }
        };
        fullScreenDialog.setCancelable(cancelable);
        fullScreenDialog.setCanceledOnTouchOutside(cancelable);
        fullScreenDialog.show();
    }

    @Override
    protected boolean allowAdsInterval() {
        return true;
    }

    private static class FullScreenDialog extends Dialog implements DialogInterface.OnShowListener, DialogInterface.OnDismissListener {

        private ValueAnimator dismissAnimator;

        public FullScreenDialog(@NonNull Context context,
                                @NonNull NativeTemplateView templateView,
                                int dismissButtonGravity,
                                long showDismissButtonCountdown) {
            super(context, R.style.Gnt_AlertDialog_FullScreen);
            initWindow(getWindow());

            //gnt_ic_close
            super.setOnShowListener(this);
            super.setOnDismissListener(this);

            templateView.addView(initDismissView(context, dismissButtonGravity, showDismissButtonCountdown));
            setContentView(templateView, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }

        private void initWindow(Window window) {
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

        @SuppressLint("InflateParams")
        @NonNull
        private View initDismissView(Context context, int dismissButtonGravity, long showDismissButtonCountdown) {

            // Inflate the dismiss view layout
            View view = LayoutInflater.from(context).inflate(R.layout.gnt_item_loading_dismiss, null);

            // Initialize components
            ProgressBar progressBar = view.findViewById(R.id.gnt_progress_bar);
            TextView progressText = view.findViewById(R.id.gnt_progress_text);
            ImageView dismissButton = view.findViewById(R.id.gnt_button_dismiss);
            dismissButton.setOnClickListener(v -> dismiss());

            // Set initial state
            animate(true, progressBar, progressText);
            animate(false, dismissButton);

            // Start countdown if the value is greater than 0
            if (showDismissButtonCountdown > 0) {
                // Update progress bar max and value
                int countdown = (int) showDismissButtonCountdown;
                progressBar.setMax(countdown);
                progressBar.setProgress(0);

                dismissAnimator = ValueAnimator.ofInt(0, countdown);
                dismissAnimator.setStartDelay(200);
                dismissAnimator.setDuration(countdown);
                dismissAnimator.setInterpolator(new LinearInterpolator());
                dismissAnimator.addUpdateListener(animation -> {
                    int progress = (int) animation.getAnimatedValue();
                    progressBar.setProgress(progress);
                    progressText.setText(String.valueOf((countdown - progress) / 1000 + 1));
                });
                dismissAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        progressText.setText(String.valueOf(0));
                        progressBar.setProgress(countdown);
                        progressBar.postDelayed(() -> {
                            animate(false, progressBar, progressText);
                            animate(true, dismissButton);
                        }, 200);
                    }
                });
            } else {
                animate(false, progressBar, progressText);
                animate(true, dismissButton);
            }

            int gravity = dismissButtonGravity == GRAVITY_RANDOM
                    ? Math.random() < 0.5 ? GRAVITY_TOP_START : GRAVITY_TOP_END
                    : dismissButtonGravity;

            view.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    gravity
            ));
            return view;
        }

        private void animate(boolean visible, @NonNull View... views) {
            for (View view : views) {
                view.animate().cancel();
                view.animate()
                        .setDuration(400)
                        .alpha(visible ? 1f : 0f)
                        .withStartAction(visible ? () -> view.setVisibility(View.VISIBLE) : null)
                        .withEndAction(visible ? null : () -> view.setVisibility(View.GONE))
                        .start();
            }
        }

        @Override
        public void setOnShowListener(@Nullable OnShowListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setOnDismissListener(@Nullable OnDismissListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onShow(DialogInterface dialog) {
            if (dismissAnimator != null) {
                dismissAnimator.start();
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            if (dismissAnimator != null) {
                dismissAnimator.cancel();
            }
        }
    }

}
