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
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
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
import java.util.Optional;

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
    private long clickableDismissButtonCountdown = 0;  // 0 means enable immediately
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

    public void setClickableDismissButtonCountdown(long countdown) {
        this.clickableDismissButtonCountdown = countdown;
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
                    public void onAdClicked() {
                        NativeFullScreenAds.this.onAdClicked();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        callback.onAdsFailedToLoad(loadAdError);
                    }
                })
                .forNativeAd(nativeAd -> {
                    nativeAd.setOnPaidEventListener(NativeFullScreenAds.this::onPaidEvent);
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
        fullScreenDialog = new FullScreenDialog(activity, templateView, dismissButtonGravity, showDismissButtonCountdown, clickableDismissButtonCountdown) {
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
    protected void onClear() {
        super.onClear();
        Optional.ofNullable(getAds()).ifPresent(NativeAd::destroy);
    }

    @Override
    protected void onAdClicked() {
        super.onAdClicked();
        if (fullScreenDialog != null) {
            fullScreenDialog.onAdClicked();
        }
    }

    @Override
    protected boolean allowAdsInterval() {
        return true;
    }

    private static class FullScreenDialog extends Dialog implements DialogInterface.OnShowListener, DialogInterface.OnDismissListener {

        private View dismissView;
        private ValueAnimator dismissAnimator;

        public FullScreenDialog(@NonNull Context context,
                                @NonNull NativeTemplateView templateView,
                                int dismissButtonGravity,
                                long showDismissButtonCountdown,
                                long clickableDismissButtonCountdown) {
            super(context, R.style.Gnt_AlertDialog_FullScreen);
            super.setOnShowListener(this);
            super.setOnDismissListener(this);

            initWindow(getWindow());
            initDismissView(
                    templateView,
                    dismissButtonGravity,
                    showDismissButtonCountdown,
                    clickableDismissButtonCountdown
            );
            setContentView(templateView, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }

        public void onAdClicked() {
            initDismissViewClickListener();
        }

        private void initWindow(Window window) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                View decorView = window.getDecorView();
                ViewCompat.setOnApplyWindowInsetsListener(decorView, (v, insets) -> {
                    Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    decorView.setPadding(inset.left, inset.top, inset.right, inset.bottom);
                    return WindowInsetsCompat.CONSUMED;
                });
            } else {
                // deprecated on API 35
                WindowCompat.setDecorFitsSystemWindows(window, true);
                WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
                controller.setAppearanceLightStatusBars(false);
                controller.setAppearanceLightNavigationBars(false);
                window.setStatusBarColor(Color.BLACK);
                window.setNavigationBarColor(Color.BLACK);
            }
        }

        @SuppressLint("InflateParams")
        private void initDismissView(@NonNull NativeTemplateView templateView,
                                     int dismissButtonGravity,
                                     long showDismissButtonCountdown,
                                     long clickableDismissButtonCountdown) {

            // Initialize components
            View loadingDismiss = templateView.findViewById(R.id.loading);
            ProgressBar progressBar = loadingDismiss.findViewById(R.id.gnt_progress_bar);
            TextView progressText = loadingDismiss.findViewById(R.id.gnt_progress_text);
            ImageView dismissButton = loadingDismiss.findViewById(R.id.gnt_button_dismiss);

            // Set initial state
            animate(true, progressBar, progressText);
            animate(false, dismissButton);

            this.dismissView = dismissButton;

            // Start countdown if the value is greater than 0
            if (showDismissButtonCountdown > 0) {
                // Update progress bar max and value
                int countdown = (int) showDismissButtonCountdown;
                progressBar.setMax(countdown);
                progressBar.setProgress(0);

                dismissAnimator = ValueAnimator.ofInt(0, countdown);
                dismissAnimator.setStartDelay(400);
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
                            dismissButton.postDelayed(FullScreenDialog.this::initDismissViewClickListener, clickableDismissButtonCountdown);
                        }, 400);
                    }
                });
            } else {
                animate(false, progressBar, progressText);
                animate(true, dismissButton);
                dismissButton.postDelayed(this::initDismissViewClickListener, clickableDismissButtonCountdown);
            }

            // init dismiss button gravity
            ViewGroup.LayoutParams lp = loadingDismiss.getLayoutParams();
            if (lp instanceof RelativeLayout.LayoutParams) {
                int gravity;
                if (dismissButtonGravity == GRAVITY_RANDOM) {
                    gravity = Math.random() < 0.5 ? GRAVITY_TOP_START : GRAVITY_TOP_END;
                } else {
                    gravity = dismissButtonGravity;
                }
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) lp;
                params.removeRule(RelativeLayout.ALIGN_START);
                params.removeRule(RelativeLayout.ALIGN_END);
                if (gravity == GRAVITY_TOP_START) {
                    params.addRule(RelativeLayout.ALIGN_TOP, R.id.media_view);
                    params.addRule(RelativeLayout.ALIGN_START, R.id.media_view);
                } else if (gravity == GRAVITY_TOP_END) {
                    params.addRule(RelativeLayout.ALIGN_TOP, R.id.media_view);
                    params.addRule(RelativeLayout.ALIGN_END, R.id.media_view);
                }
            }
        }

        private void initDismissViewClickListener() {
            if (dismissView != null) {
                dismissView.setOnClickListener(v -> dismiss());
            }
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
            dismissView = null;
        }
    }

}
