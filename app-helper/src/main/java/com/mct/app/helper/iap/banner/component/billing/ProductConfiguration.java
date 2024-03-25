package com.mct.app.helper.iap.banner.component.billing;

import androidx.annotation.NonNull;

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
    private final float discountPercent;
    private final int selectedOfferIndex;

    private ProductConfiguration(String productId, float discountPercent, int selectedOfferIndex) {
        this.productId = productId;
        this.discountPercent = discountPercent;
        this.selectedOfferIndex = selectedOfferIndex;
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
     * Gets the discount percentage configured in this instance.
     *
     * @return The discount percentage.
     */
    public float getDiscountPercent() {
        return discountPercent;
    }

    /**
     * Gets the selected offer index configured in this instance.
     *
     * @return The selected offer index (for products with multiple offers).
     */
    public int getSelectedOfferIndex() {
        return selectedOfferIndex;
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
        private float discountPercent;
        private int selectedOfferIndex;

        /**
         * @param productId - The product ID to configure.
         */
        public Builder(String productId) {
            this.productId = productId;
            this.discountPercent = 0;
            this.selectedOfferIndex = 0;
        }

        /**
         * Sets the discount percentage for the product.
         *
         * @param discountPercent - The discount percentage to apply.
         * @return The {@link Builder} instance for method chaining.
         */
        public Builder withDiscountPercent(float discountPercent) {
            this.discountPercent = discountPercent;
            return this;
        }

        /**
         * Sets the selected offer index for the product (for products with multiple offers).
         *
         * @param selectedOfferIndex - The selected offer index to set.
         * @return The {@link Builder} instance for method chaining.
         */
        public Builder withOfferIndex(int selectedOfferIndex) {
            this.selectedOfferIndex = selectedOfferIndex;
            return this;
        }

        /**
         * Builds a {@link ProductConfiguration} instance with the configured settings.
         *
         * @return A {@link ProductConfiguration} instance with the specified configurations.
         */
        public ProductConfiguration build() {
            return new ProductConfiguration(productId, discountPercent, selectedOfferIndex);
        }
    }
}
