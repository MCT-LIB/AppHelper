package com.mct.app.helper.admob.ads.natives;

import android.graphics.Typeface;

/**
 * A class containing the optional styling options for the Native NativeTemplate.
 */
public class NativeTemplateStyle {

    // Call to action typeface.
    private Typeface callToActionTextTypeface;

    // Size of call to action text.
    private float callToActionTextSize;

    // Call to action typeface color in the form 0xAARRGGBB.
    private Integer callToActionTypefaceColor;

    // Call to action background color.
    private Integer callToActionBackgroundColor;
    // Call to action ripple color.
    private Integer callToActionRippleColor;
    // Call to action corner radius.
    private Integer callToActionCornerRadius;

    // All templates have a primary text area which is populated by the native ad's headline.

    // Primary text typeface.
    private Typeface primaryTextTypeface;

    // Size of primary text.
    private float primaryTextSize;

    // Primary text typeface color in the form 0xAARRGGBB.
    private Integer primaryTextTypefaceColor;

    // Primary text background color.
    private Integer primaryTextBackgroundColor;

    // The typeface, typeface color, and background color for the second row of text in the template.
    // All templates have a secondary text area which is populated either by the body of the ad or
    // by the rating of the app.

    // Secondary text typeface.
    private Typeface secondaryTextTypeface;

    // Size of secondary text.
    private float secondaryTextSize;

    // Secondary text typeface color in the form 0xAARRGGBB.
    private Integer secondaryTextTypefaceColor;

    // Secondary text background color.
    private Integer secondaryTextBackgroundColor;

    // The typeface, typeface color, and background color for the third row of text in the template.
    // The third row is used to display store name or the default tertiary text.

    // Tertiary text typeface.
    private Typeface tertiaryTextTypeface;

    // Size of tertiary text.
    private float tertiaryTextSize;

    // Tertiary text typeface color in the form 0xAARRGGBB.
    private Integer tertiaryTextTypefaceColor;

    // Tertiary text background color.
    private Integer tertiaryTextBackgroundColor;

    // The background color for the bulk of the ad.
    private Integer mainBackgroundColor;

    private NativeTemplateStyle() {
    }

    public Typeface getCallToActionTextTypeface() {
        return callToActionTextTypeface;
    }

    public float getCallToActionTextSize() {
        return callToActionTextSize;
    }

    public Integer getCallToActionTypefaceColor() {
        return callToActionTypefaceColor;
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

    public Typeface getPrimaryTextTypeface() {
        return primaryTextTypeface;
    }

    public float getPrimaryTextSize() {
        return primaryTextSize;
    }

    public Integer getPrimaryTextTypefaceColor() {
        return primaryTextTypefaceColor;
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

    public Integer getSecondaryTextTypefaceColor() {
        return secondaryTextTypefaceColor;
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

    public Integer getTertiaryTextTypefaceColor() {
        return tertiaryTextTypefaceColor;
    }

    public Integer getTertiaryTextBackgroundColor() {
        return tertiaryTextBackgroundColor;
    }

    public Integer getMainBackgroundColor() {
        return mainBackgroundColor;
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

        public Builder withCallToActionTypefaceColor(int callToActionTypefaceColor) {
            this.styles.callToActionTypefaceColor = callToActionTypefaceColor;
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

        public Builder withPrimaryTextTypeface(Typeface primaryTextTypeface) {
            this.styles.primaryTextTypeface = primaryTextTypeface;
            return this;
        }

        public Builder withPrimaryTextSize(float primaryTextSize) {
            this.styles.primaryTextSize = primaryTextSize;
            return this;
        }

        public Builder withPrimaryTextTypefaceColor(int primaryTextTypefaceColor) {
            this.styles.primaryTextTypefaceColor = primaryTextTypefaceColor;
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

        public Builder withSecondaryTextTypefaceColor(int secondaryTextTypefaceColor) {
            this.styles.secondaryTextTypefaceColor = secondaryTextTypefaceColor;
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

        public Builder withTertiaryTextTypefaceColor(int tertiaryTextTypefaceColor) {
            this.styles.tertiaryTextTypefaceColor = tertiaryTextTypefaceColor;
            return this;
        }

        public Builder withTertiaryTextBackgroundColor(int tertiaryTextBackgroundColor) {
            this.styles.tertiaryTextBackgroundColor = tertiaryTextBackgroundColor;
            return this;
        }

        public Builder withMainBackgroundColor(int mainBackgroundColor) {
            this.styles.mainBackgroundColor = mainBackgroundColor;
            return this;
        }

        public NativeTemplateStyle build() {
            return styles;
        }
    }
}