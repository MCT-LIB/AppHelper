package com.mct.app.helper.admob.ads.natives;

import android.graphics.Typeface;

/**
 * A class containing the optional styling options for the Native NativeTemplate.
 */
public class NativeTemplateStyle {

    private Typeface callToActionTextTypeface;
    private float callToActionTextSize;
    private Integer callToActionColor;
    private Integer callToActionBackgroundColor;
    private Integer callToActionRippleColor;
    private Integer callToActionCornerRadius;
    private Integer callToActionBackgroundDrawable;

    private Typeface primaryTextTypeface;
    private float primaryTextSize;
    private Integer primaryTextColor;
    private Integer primaryTextBackgroundColor;

    private Typeface secondaryTextTypeface;
    private float secondaryTextSize;
    private Integer secondaryTextColor;
    private Integer secondaryTextBackgroundColor;

    private Typeface tertiaryTextTypeface;
    private float tertiaryTextSize;
    private Integer tertiaryTextColor;
    private Integer tertiaryTextBackgroundColor;

    private Typeface adIndicatorTextTypeface;
    private Integer adIndicatorTextColor;
    private Integer adIndicatorBackgroundDrawable;

    // The background color of the ad.
    private Integer mainBackgroundColor;
    private Integer ratingBarTint;

    private NativeTemplateStyle() {
    }

    public Typeface getCallToActionTextTypeface() {
        return callToActionTextTypeface;
    }

    public float getCallToActionTextSize() {
        return callToActionTextSize;
    }

    public Integer getCallToActionColor() {
        return callToActionColor;
    }

    public Integer getCallToActionBackgroundColor() {
        return callToActionBackgroundColor;
    }

    public Integer getCallToActionRippleColor() {
        return callToActionRippleColor;
    }

    public Integer getCallToActionCornerRadius() {
        return callToActionCornerRadius;
    }

    public Integer getCallToActionBackgroundDrawable() {
        return callToActionBackgroundDrawable;
    }

    public Typeface getPrimaryTextTypeface() {
        return primaryTextTypeface;
    }

    public float getPrimaryTextSize() {
        return primaryTextSize;
    }

    public Integer getPrimaryTextColor() {
        return primaryTextColor;
    }

    public Integer getPrimaryTextBackgroundColor() {
        return primaryTextBackgroundColor;
    }

    public Typeface getSecondaryTextTypeface() {
        return secondaryTextTypeface;
    }

    public float getSecondaryTextSize() {
        return secondaryTextSize;
    }

    public Integer getSecondaryTextColor() {
        return secondaryTextColor;
    }

    public Integer getSecondaryTextBackgroundColor() {
        return secondaryTextBackgroundColor;
    }

    public Typeface getTertiaryTextTypeface() {
        return tertiaryTextTypeface;
    }

    public float getTertiaryTextSize() {
        return tertiaryTextSize;
    }

    public Integer getTertiaryTextColor() {
        return tertiaryTextColor;
    }

    public Integer getTertiaryTextBackgroundColor() {
        return tertiaryTextBackgroundColor;
    }

    public Typeface getAdIndicatorTextTypeface() {
        return adIndicatorTextTypeface;
    }

    public Integer getAdIndicatorTextColor() {
        return adIndicatorTextColor;
    }

    public Integer getAdIndicatorBackgroundDrawable() {
        return adIndicatorBackgroundDrawable;
    }

    public Integer getMainBackgroundColor() {
        return mainBackgroundColor;
    }

    public Integer getRatingBarTint() {
        return ratingBarTint;
    }

    /**
     * A class that provides helper methods to build a style object.
     */
    public static class Builder {

        private final NativeTemplateStyle styles;

        public Builder() {
            this.styles = new NativeTemplateStyle();
        }

        public Builder withCallToActionTextTypeface(Typeface callToActionTextTypeface) {
            this.styles.callToActionTextTypeface = callToActionTextTypeface;
            return this;
        }

        public Builder withCallToActionTextSize(float callToActionTextSize) {
            this.styles.callToActionTextSize = callToActionTextSize;
            return this;
        }

        public Builder withCallToActionColor(int callToActionColor) {
            this.styles.callToActionColor = callToActionColor;
            return this;
        }

        public Builder withCallToActionBackgroundColor(int callToActionBackgroundColor) {
            this.styles.callToActionBackgroundColor = callToActionBackgroundColor;
            return this;
        }

        public Builder withCallToActionRippleColor(int callToActionRippleColor) {
            this.styles.callToActionRippleColor = callToActionRippleColor;
            return this;
        }

        public Builder withCallToActionCornerRadius(int callToActionCornerRadius) {
            this.styles.callToActionCornerRadius = callToActionCornerRadius;
            return this;
        }

        public Builder withCallToActionBackgroundDrawable(int callToActionBackgroundDrawable) {
            this.styles.callToActionBackgroundDrawable = callToActionBackgroundDrawable;
            return this;
        }

        public Builder withPrimaryTextTypeface(Typeface primaryTextTypeface) {
            this.styles.primaryTextTypeface = primaryTextTypeface;
            return this;
        }

        public Builder withPrimaryTextSize(float primaryTextSize) {
            this.styles.primaryTextSize = primaryTextSize;
            return this;
        }

        public Builder withPrimaryTextColor(int primaryTextColor) {
            this.styles.primaryTextColor = primaryTextColor;
            return this;
        }

        public Builder withPrimaryTextBackgroundColor(int primaryTextBackgroundColor) {
            this.styles.primaryTextBackgroundColor = primaryTextBackgroundColor;
            return this;
        }

        public Builder withSecondaryTextTypeface(Typeface secondaryTextTypeface) {
            this.styles.secondaryTextTypeface = secondaryTextTypeface;
            return this;
        }

        public Builder withSecondaryTextSize(float secondaryTextSize) {
            this.styles.secondaryTextSize = secondaryTextSize;
            return this;
        }

        public Builder withSecondaryTextColor(int secondaryTextColor) {
            this.styles.secondaryTextColor = secondaryTextColor;
            return this;
        }

        public Builder withSecondaryTextBackgroundColor(int secondaryTextBackgroundColor) {
            this.styles.secondaryTextBackgroundColor = secondaryTextBackgroundColor;
            return this;
        }

        public Builder withTertiaryTextTypeface(Typeface tertiaryTextTypeface) {
            this.styles.tertiaryTextTypeface = tertiaryTextTypeface;
            return this;
        }

        public Builder withTertiaryTextSize(float tertiaryTextSize) {
            this.styles.tertiaryTextSize = tertiaryTextSize;
            return this;
        }

        public Builder withTertiaryTextColor(int tertiaryTextColor) {
            this.styles.tertiaryTextColor = tertiaryTextColor;
            return this;
        }

        public Builder withTertiaryTextBackgroundColor(int tertiaryTextBackgroundColor) {
            this.styles.tertiaryTextBackgroundColor = tertiaryTextBackgroundColor;
            return this;
        }

        public Builder withAdIndicatorTextTypeface(Typeface adIndicatorTextTypeface) {
            this.styles.adIndicatorTextTypeface = adIndicatorTextTypeface;
            return this;
        }

        public Builder withAdIndicatorTextColor(int adIndicatorTextColor) {
            this.styles.adIndicatorTextColor = adIndicatorTextColor;
            return this;
        }

        public Builder withAdIndicatorBackgroundDrawable(int adIndicatorBackgroundDrawable) {
            this.styles.adIndicatorBackgroundDrawable = adIndicatorBackgroundDrawable;
            return this;
        }

        public Builder withMainBackgroundColor(int mainBackgroundColor) {
            this.styles.mainBackgroundColor = mainBackgroundColor;
            return this;
        }

        public Builder withRatingBarTint(int ratingBarTint) {
            this.styles.ratingBarTint = ratingBarTint;
            return this;
        }

        public NativeTemplateStyle build() {
            return styles;
        }
    }
}