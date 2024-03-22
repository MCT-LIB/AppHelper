package com.mct.app.helper.admob.ads.natives;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
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
            initView(attributes.getResourceId(R.styleable.TemplateView_gnt_template, R.layout.gnt_large_template_view));
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
        secondaryView.setVisibility(VISIBLE);
        if (adHasOnlyStore(nativeAd)) {
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

    /**
     * To prevent memory leaks, make sure to destroy your ad when you don't need it anymore. This
     * method does not destroy the template view.
     * <a href="https://developers.google.com/admob/android/native-unified#destroy_ad">destroy_ad</a>
     */
    public void destroyNativeAd() {
        nativeAd.destroy();
    }

    private void applyStyles() {

        Drawable mainBackground = styles.getMainBackgroundColor();
        if (mainBackground != null) {
            background.setBackground(mainBackground);
            if (primaryView != null) {
                primaryView.setBackground(mainBackground);
            }
            if (secondaryView != null) {
                secondaryView.setBackground(mainBackground);
            }
            if (tertiaryView != null) {
                tertiaryView.setBackground(mainBackground);
            }
        }

        Typeface primary = styles.getPrimaryTextTypeface();
        if (primary != null && primaryView != null) {
            primaryView.setTypeface(primary);
        }

        Typeface secondary = styles.getSecondaryTextTypeface();
        if (secondary != null && secondaryView != null) {
            secondaryView.setTypeface(secondary);
        }

        Typeface tertiary = styles.getTertiaryTextTypeface();
        if (tertiary != null && tertiaryView != null) {
            tertiaryView.setTypeface(tertiary);
        }

        Typeface ctaTypeface = styles.getCallToActionTextTypeface();
        if (ctaTypeface != null && callToActionView != null) {
            callToActionView.setTypeface(ctaTypeface);
        }

        if (styles.getPrimaryTextTypefaceColor() != null && primaryView != null) {
            primaryView.setTextColor(styles.getPrimaryTextTypefaceColor());
        }

        if (styles.getSecondaryTextTypefaceColor() != null && secondaryView != null) {
            secondaryView.setTextColor(styles.getSecondaryTextTypefaceColor());
        }

        if (styles.getTertiaryTextTypefaceColor() != null && tertiaryView != null) {
            tertiaryView.setTextColor(styles.getTertiaryTextTypefaceColor());
        }

        if (styles.getCallToActionTypefaceColor() != null && callToActionView != null) {
            callToActionView.setTextColor(styles.getCallToActionTypefaceColor());
        }

        float ctaTextSize = styles.getCallToActionTextSize();
        if (ctaTextSize > 0 && callToActionView != null) {
            callToActionView.setTextSize(ctaTextSize);
        }

        float primaryTextSize = styles.getPrimaryTextSize();
        if (primaryTextSize > 0 && primaryView != null) {
            primaryView.setTextSize(primaryTextSize);
        }

        float secondaryTextSize = styles.getSecondaryTextSize();
        if (secondaryTextSize > 0 && secondaryView != null) {
            secondaryView.setTextSize(secondaryTextSize);
        }

        float tertiaryTextSize = styles.getTertiaryTextSize();
        if (tertiaryTextSize > 0 && tertiaryView != null) {
            tertiaryView.setTextSize(tertiaryTextSize);
        }

        Drawable ctaBackground = styles.getCallToActionBackgroundColor();
        if (ctaBackground != null && callToActionView != null) {
            callToActionView.setBackground(ctaBackground);
        }

        Drawable primaryBackground = styles.getPrimaryTextBackgroundColor();
        if (primaryBackground != null && primaryView != null) {
            primaryView.setBackground(primaryBackground);
        }

        Drawable secondaryBackground = styles.getSecondaryTextBackgroundColor();
        if (secondaryBackground != null && secondaryView != null) {
            secondaryView.setBackground(secondaryBackground);
        }

        Drawable tertiaryBackground = styles.getTertiaryTextBackgroundColor();
        if (tertiaryBackground != null && tertiaryView != null) {
            tertiaryView.setBackground(tertiaryBackground);
        }

        invalidate();
        requestLayout();
    }

    private boolean adHasOnlyStore(NativeAd nativeAd) {
        String store = nativeAd.getStore();
        String advertiser = nativeAd.getAdvertiser();
        return !TextUtils.isEmpty(store) && TextUtils.isEmpty(advertiser);
    }

}