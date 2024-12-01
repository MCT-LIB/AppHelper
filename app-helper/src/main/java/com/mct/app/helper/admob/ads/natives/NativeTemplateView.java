package com.mct.app.helper.admob.ads.natives;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.mct.app.helper.R;

/**
 * Base class for a template view. *
 */
public class NativeTemplateView extends FrameLayout {

    private NativeTemplateStyle styles;
    private NativeAd nativeAd;
    private NativeAdView nativeAdView;

    private TextView primaryView;
    private TextView secondaryView;
    private RatingBar ratingBar;
    private TextView tertiaryView;
    private ImageView iconView;
    private MediaView mediaView;
    private Button callToActionView;
    private ViewGroup background;
    private TextView adIndicator;

    public NativeTemplateView(Context context, @LayoutRes int template) {
        super(context);
        initView(template);
    }

    public NativeTemplateView(@NonNull Context context) {
        this(context, null);
    }

    public NativeTemplateView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NativeTemplateView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TemplateView, 0, 0);
        try {
            initView(attributes.getResourceId(R.styleable.TemplateView_gnt_template, R.layout.gnt_template_view_medium_1));
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                attributes.close();
            } else {
                attributes.recycle();
            }
        }
    }

    private void initView(int template) {
        LayoutInflater.from(getContext()).inflate(template, this);
        nativeAdView = findViewById(R.id.native_ad_view);
        primaryView = findViewById(R.id.primary);
        secondaryView = findViewById(R.id.secondary);
        tertiaryView = findViewById(R.id.body);
        ratingBar = findViewById(R.id.rating_bar);
        callToActionView = findViewById(R.id.cta);
        iconView = findViewById(R.id.icon);
        mediaView = findViewById(R.id.media_view);
        background = findViewById(R.id.background);
        adIndicator = findViewById(R.id.gnt_indicator_view);
    }

    public void setStyles(NativeTemplateStyle styles) {
        this.styles = styles;
        if (styles != null) {
            applyStyles();
        }
    }

    public NativeAdView getNativeAdView() {
        return nativeAdView;
    }

    public NativeAd getNativeAd() {
        return nativeAd;
    }

    /**
     * To prevent memory leaks, make sure to destroy your ad when you don't need it anymore. This
     * method does not destroy the template view.
     * <a href="https://developers.google.com/admob/android/native-unified#destroy_ad">destroy_ad</a>
     */
    public void destroyNativeAd() {
        if (nativeAd == null) {
            return;
        }
        nativeAd.destroy();
        nativeAd = null;
    }

    public void setNativeAd(@NonNull NativeAd nativeAd) {
        this.nativeAd = nativeAd;

        String store = nativeAd.getStore();
        String advertiser = nativeAd.getAdvertiser();
        String headline = nativeAd.getHeadline();
        String body = nativeAd.getBody();
        String cta = nativeAd.getCallToAction();
        Double starRating = nativeAd.getStarRating();
        NativeAd.Image icon = nativeAd.getIcon();

        String secondaryText;

        nativeAdView.setCallToActionView(callToActionView);
        nativeAdView.setHeadlineView(primaryView);
        nativeAdView.setMediaView(mediaView);
        if (!TextUtils.isEmpty(store) && TextUtils.isEmpty(advertiser)) {
            nativeAdView.setStoreView(secondaryView);
            secondaryText = store;
        } else if (!TextUtils.isEmpty(advertiser)) {
            nativeAdView.setAdvertiserView(secondaryView);
            secondaryText = advertiser;
        } else {
            secondaryText = "";
        }

        primaryView.setText(headline);
        callToActionView.setText(cta);

        //  Set the secondary view to be the star rating if available.
        if (starRating != null && starRating > 0) {
            secondaryView.setVisibility(GONE);
            ratingBar.setVisibility(VISIBLE);
            ratingBar.setRating(starRating.floatValue());

            nativeAdView.setStarRatingView(ratingBar);
        } else {
            secondaryView.setText(secondaryText);
            secondaryView.setVisibility(VISIBLE);
            ratingBar.setVisibility(GONE);
        }

        if (icon != null) {
            iconView.setVisibility(VISIBLE);
            iconView.setImageDrawable(icon.getDrawable());
        } else {
            iconView.setVisibility(GONE);
        }

        if (tertiaryView != null) {
            tertiaryView.setText(body);
            nativeAdView.setBodyView(tertiaryView);
        }

        nativeAdView.setNativeAd(nativeAd);
    }

    private void applyStyles() {

        Integer mainBackground = styles.getMainBackgroundColor();
        if (mainBackground != null) {
            background.setBackground(new ColorDrawable(mainBackground));
            if (primaryView != null) {
                primaryView.setBackground(new ColorDrawable(mainBackground));
            }
            if (secondaryView != null) {
                secondaryView.setBackground(new ColorDrawable(mainBackground));
            }
            if (tertiaryView != null) {
                tertiaryView.setBackground(new ColorDrawable(mainBackground));
            }
        }

        Integer ratingBarTint = styles.getRatingBarTint();
        if (ratingBar != null && ratingBarTint != null) {
            ratingBar.setProgressTintList(ColorStateList.valueOf(ratingBarTint));
            ratingBar.setProgressBackgroundTintList(ColorStateList.valueOf(ratingBarTint));
            ratingBar.setSecondaryProgressTintList(ColorStateList.valueOf(ratingBarTint));
        }

        Typeface adIndicatorTextTypeface = styles.getAdIndicatorTextTypeface();
        if (adIndicator != null && adIndicatorTextTypeface != null) {
            adIndicator.setTypeface(adIndicatorTextTypeface);
        }

        Typeface primary = styles.getPrimaryTextTypeface();
        if (primaryView != null && primary != null) {
            primaryView.setTypeface(primary);
        }

        Typeface secondary = styles.getSecondaryTextTypeface();
        if (secondaryView != null && secondary != null) {
            secondaryView.setTypeface(secondary);
        }

        Typeface tertiary = styles.getTertiaryTextTypeface();
        if (tertiaryView != null && tertiary != null) {
            tertiaryView.setTypeface(tertiary);
        }

        Typeface ctaTypeface = styles.getCallToActionTextTypeface();
        if (callToActionView != null && ctaTypeface != null) {
            callToActionView.setTypeface(ctaTypeface);
        }

        if (adIndicator != null && styles.getAdIndicatorTextColor() != null) {
            adIndicator.setTextColor(styles.getAdIndicatorTextColor());
        }

        if (primaryView != null && styles.getPrimaryTextColor() != null) {
            primaryView.setTextColor(styles.getPrimaryTextColor());
        }

        if (secondaryView != null && styles.getSecondaryTextColor() != null) {
            secondaryView.setTextColor(styles.getSecondaryTextColor());
        }

        if (tertiaryView != null && styles.getTertiaryTextColor() != null) {
            tertiaryView.setTextColor(styles.getTertiaryTextColor());
        }

        if (callToActionView != null && styles.getCallToActionColor() != null) {
            callToActionView.setTextColor(styles.getCallToActionColor());
        }

        float ctaTextSize = styles.getCallToActionTextSize();
        if (callToActionView != null && ctaTextSize > 0) {
            callToActionView.setTextSize(ctaTextSize);
        }

        float primaryTextSize = styles.getPrimaryTextSize();
        if (primaryView != null && primaryTextSize > 0) {
            primaryView.setTextSize(primaryTextSize);
        }

        float secondaryTextSize = styles.getSecondaryTextSize();
        if (secondaryView != null && secondaryTextSize > 0) {
            secondaryView.setTextSize(secondaryTextSize);
        }

        float tertiaryTextSize = styles.getTertiaryTextSize();
        if (tertiaryView != null && tertiaryTextSize > 0) {
            tertiaryView.setTextSize(tertiaryTextSize);
        }

        Integer ctaBackgroundColor = styles.getCallToActionBackgroundColor();
        if (callToActionView != null && ctaBackgroundColor != null) {
            Integer ctaRippleColor = styles.getCallToActionRippleColor();
            Integer ctaCornerRadius = styles.getCallToActionCornerRadius();
            if (ctaRippleColor == null) {
                ctaRippleColor = Color.parseColor("#80FFFFFF");
            }
            if (ctaCornerRadius == null) {
                ctaCornerRadius = 0;
            }
            callToActionView.setBackground(getRippleDrawable(ctaBackgroundColor, ctaRippleColor, ctaCornerRadius));
        }

        Integer ctaBackgroundDrawable = styles.getCallToActionBackgroundDrawable();
        if (callToActionView != null && ctaBackgroundDrawable != null) {
            callToActionView.setBackgroundResource(ctaBackgroundDrawable);
        }

        Integer adIndicatorBackgroundDrawable = styles.getAdIndicatorBackgroundDrawable();
        if (adIndicator != null && adIndicatorBackgroundDrawable != null) {
            adIndicator.setBackgroundResource(adIndicatorBackgroundDrawable);
        }

        Integer primaryBackground = styles.getPrimaryTextBackgroundColor();
        if (primaryBackground != null && primaryView != null) {
            primaryView.setBackground(new ColorDrawable(primaryBackground));
        }

        Integer secondaryBackground = styles.getSecondaryTextBackgroundColor();
        if (secondaryBackground != null && secondaryView != null) {
            secondaryView.setBackground(new ColorDrawable(secondaryBackground));
        }

        Integer tertiaryBackground = styles.getTertiaryTextBackgroundColor();
        if (tertiaryBackground != null && tertiaryView != null) {
            tertiaryView.setBackground(new ColorDrawable(tertiaryBackground));
        }

        requestLayout();
    }

    @NonNull
    private static RippleDrawable getRippleDrawable(int normalColor, int pressedColor, int cornerRadius) {
        return new RippleDrawable(
                getPressedColorSelector(normalColor, pressedColor),
                getDrawableFromColorAndCornerRadius(normalColor, cornerRadius), null);
    }

    @NonNull
    private static ColorStateList getPressedColorSelector(int normalColor, int pressedColor) {
        return new ColorStateList(new int[][]{
                new int[]{android.R.attr.state_pressed},
                new int[]{android.R.attr.state_focused},
                new int[]{android.R.attr.state_activated},
                new int[]{}
        }, new int[]{
                pressedColor,
                pressedColor,
                pressedColor,
                normalColor
        });
    }

    @NonNull
    private static Drawable getDrawableFromColorAndCornerRadius(int color, int cornerRadius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(cornerRadius);
        return drawable;
    }

}