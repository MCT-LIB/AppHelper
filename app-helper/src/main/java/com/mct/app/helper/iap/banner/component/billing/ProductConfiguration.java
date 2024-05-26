package com.mct.app.helper.iap.banner.component.billing;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ProductConfiguration - A class for configuring product-specific settings.
 * <p>
 * The {@link ProductConfiguration} class allows you to create and configure product-specific settings,
 * such as the product ID, discount percentage, and selected offer index. It provides convenient
 * factory methods for creating instances with different configurations.
 * <p>
 * Usage Example:
 * <code>
 * <pre>
 * 1. Create an instance of {@link ProductConfiguration} with the product ID:
 * ProductConfiguration config = ProductConfiguration.of("your_product_id");
 *
 * 2. Optionally, set a discount percentage:
 * ProductConfiguration configWithDiscount = ProductConfiguration.of("your_product_id", 10.0f);
 *
 * 3. Optionally, set a selected offer index (for products with multiple offers):
 * ProductConfiguration configWithOffer = ProductConfiguration.of("your_product_id", 10.0f, 1);
 * </pre>
 * </code>
 */
public class ProductConfiguration {

    private final String productId;
    private final String planId;
    private final String offerId;
    private final AtomicBoolean enableOfferId; // use for toggle free trial
    private final float discountPercent;

    private ProductConfiguration(String productId, String planId, String offerId, boolean enableOfferId, float discountPercent) {
        this.productId = productId;
        this.planId = planId;
        this.offerId = offerId;
        this.enableOfferId = new AtomicBoolean(enableOfferId);
        this.discountPercent = discountPercent;
    }

    /**
     * Gets the product ID configured in this instance.
     *
     * @return The product ID.
     */
    public String getProductId() {
        return productId;
    }

    /**
     * Gets the plan ID configured in this instance.
     *
     * @return The plan ID.
     */
    public String getPlanId() {
        return planId;
    }

    /**
     * Gets the offer ID configured in this instance.
     *
     * @return The offer ID.
     */
    public String getOfferId() {
        return isEnableOfferId() ? offerId : null;
    }

    /**
     * Gets the discount percentage configured in this instance.
     *
     * @return The discount percentage.
     */
    public float getDiscountPercent() {
        return discountPercent;
    }

    /**
     * Gets the enableOfferId configured in this instance.
     *
     * @return The enableOfferId.
     */
    public boolean isEnableOfferId() {
        return enableOfferId.get();
    }

    /**
     * Sets enable offer id
     *
     * @param enableOfferId - enable offer id
     */
    public void setEnableOfferId(boolean enableOfferId) {
        this.enableOfferId.set(enableOfferId);
    }

    /**
     * Creates a builder for configuring a {@link ProductConfiguration} instance.
     *
     * @param productId - The product ID to configure.
     * @return A {@link Builder} instance for configuring the product settings.
     */
    @NonNull
    public static Builder of(String productId) {
        return new Builder(productId);
    }

    /**
     * Builder pattern for configuring a {@link ProductConfiguration} instance.
     */
    public static class Builder {

        private final String productId;
        private String planId;
        private String offerId;
        private boolean enableOfferId;
        private float discountPercent;

        /**
         * @param productId - The product ID to configure.
         */
        public Builder(String productId) {
            this.productId = productId;
            this.planId = null;
            this.offerId = null;
            this.enableOfferId = true;
            this.discountPercent = 0;
        }

        /**
         * Sets the plan ID for the product.
         *
         * @param planId - The plan ID to set.
         * @return The {@link Builder} instance for method chaining.
         */
        public Builder setSubscription(String planId) {
            this.planId = planId;
            return this;
        }

        /**
         * Sets the plan ID and offer ID for the product.
         *
         * @param offerId - The offer ID to set.
         * @return The {@link Builder} instance for method chaining.
         */
        public Builder setSubscription(String planId, String offerId) {
            this.planId = planId;
            this.offerId = TextUtils.isEmpty(offerId) ? null : offerId;
            return this;
        }

        /**
         * Sets enable offer id
         *
         * @param enableOfferId - enable offer id
         * @return The {@link Builder} instance for method chaining.
         */
        public Builder setEnableOfferId(boolean enableOfferId) {
            this.enableOfferId = enableOfferId;
            return this;
        }

        /**
         * Sets the discount percentage for the product.
         *
         * @param discountPercent - The discount percentage to apply.
         * @return The {@link Builder} instance for method chaining.
         */
        public Builder setDiscountPercent(float discountPercent) {
            this.discountPercent = discountPercent;
            return this;
        }

        /**
         * Builds a {@link ProductConfiguration} instance with the configured settings.
         *
         * @return A {@link ProductConfiguration} instance with the specified configurations.
         */
        public ProductConfiguration build() {
            return new ProductConfiguration(productId, planId, offerId, enableOfferId, discountPercent);
        }
    }
}
