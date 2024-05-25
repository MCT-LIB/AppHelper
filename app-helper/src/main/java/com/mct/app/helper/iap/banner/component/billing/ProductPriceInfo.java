package com.mct.app.helper.iap.banner.component.billing;

import android.content.Context;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.android.billingclient.api.ProductDetails;
import com.mct.app.helper.iap.banner.utils.FormatUtils;
import com.mct.app.helper.iap.billing.enums.SkuProductType;
import com.mct.app.helper.iap.billing.models.ProductInfo;
import com.mct.app.helper.iap.billing.models.SubscriptionOfferDetails;

import java.util.Comparator;
import java.util.function.Predicate;

/**
 * ProductPriceInfo is a utility class for managing and formatting pricing information for a product.
 * It provides methods to calculate real and fake prices, get average prices, and format prices for display.
 */
public class ProductPriceInfo {

    /**
     * Create a ProductPriceInfo instance based on the given ProductConfiguration and ProductInfo.
     *
     * @param productConfiguration The product configuration containing discount information.
     * @param productInfo          The product information.
     * @return A ProductPriceInfo instance.
     */
    @NonNull
    public static ProductPriceInfo fromProductInfo(@NonNull ProductConfiguration productConfiguration, @NonNull ProductInfo productInfo) {
        float discountPercent = productConfiguration.getDiscountPercent();
        if (productInfo.getSkuProductType() != SkuProductType.SUBSCRIPTION) {
            return fromPriceAmount(discountPercent,
                    productInfo.getOneTimePurchaseOfferPriceAmountMicros() / 1000000f,
                    productInfo.getOneTimePurchaseOfferPriceCurrencyCode(),
                    productInfo.getOneTimePurchaseOfferFormattedPrice()
            );
        }
        try {
            String planId = productConfiguration.getPlanId();
            String offerId = productConfiguration.getOfferId();
            SubscriptionOfferDetails offerDetails = null;

            if (planId != null) {
                Predicate<SubscriptionOfferDetails> filter = offerId != null
                        ? (it -> planId.equals(it.getBasePlanId()) && offerId.equals(it.getOfferId()))
                        : (it -> planId.equals(it.getBasePlanId()));
                offerDetails = productInfo.getSubscriptionOfferDetails().stream()
                        .filter(filter)
                        .findFirst()
                        .orElse(null);
            }

            if (offerDetails == null) {
                offerDetails = productInfo.getSubscriptionOfferDetails().get(0);
            }

            return fromPricingPhase(discountPercent, offerDetails.getPricingPhases().stream()
                    .max(Comparator.comparingLong(SubscriptionOfferDetails.PricingPhases::getPriceAmountMicros))
                    .orElseThrow(() -> new RuntimeException("Failed to get pricing phases")));

        } catch (Exception e) {
            return fromPriceAmount(discountPercent, 0f, null);
        }
    }

    /**
     * Create a ProductPriceInfo instance based on discount percentage and pricing phase.
     *
     * @param discountPercent The discount percentage.
     * @param pricingPhase    The pricing phase information.
     * @return A ProductPriceInfo instance.
     */
    @NonNull
    public static ProductPriceInfo fromPricingPhase(float discountPercent, @NonNull ProductDetails.PricingPhase pricingPhase) {
        return fromPriceAmount(discountPercent,
                pricingPhase.getPriceAmountMicros() / 1000000f,
                pricingPhase.getPriceCurrencyCode(),
                pricingPhase.getBillingPeriod());
    }

    /**
     * Create a ProductPriceInfo instance based on discount percentage and subscription pricing phase.
     *
     * @param discountPercent The discount percentage.
     * @param pricingPhase    The subscription pricing phase information.
     * @return A ProductPriceInfo instance.
     */
    @NonNull
    public static ProductPriceInfo fromPricingPhase(float discountPercent, @NonNull SubscriptionOfferDetails.PricingPhases pricingPhase) {
        return fromPriceAmount(discountPercent,
                pricingPhase.getPriceAmountMicros() / 1000000f,
                pricingPhase.getPriceCurrencyCode(),
                pricingPhase.getBillingPeriod());
    }

    /**
     * Create a ProductPriceInfo instance based on price amount and currency code.
     *
     * @param discountPercent   The discount percentage.
     * @param priceAmount       The price amount.
     * @param priceCurrencyCode The currency code.
     * @return A ProductPriceInfo instance.
     */
    @NonNull
    public static ProductPriceInfo fromPriceAmount(float discountPercent, float priceAmount, String priceCurrencyCode) {
        return fromPriceAmount(discountPercent, priceAmount, priceCurrencyCode, "");
    }

    /**
     * Create a ProductPriceInfo instance based on price amount, currency code, and billing period.
     *
     * @param discountPercent   The discount percentage.
     * @param priceAmount       The price amount.
     * @param priceCurrencyCode The currency code.
     * @param billingPeriod     The billing period.
     * @return A ProductPriceInfo instance.
     */
    @NonNull
    public static ProductPriceInfo fromPriceAmount(float discountPercent, float priceAmount, String priceCurrencyCode, String billingPeriod) {
        return fromPriceAmount(discountPercent, priceAmount, priceCurrencyCode, BillingPeriod.findPeriod(billingPeriod));
    }

    /**
     * Create a ProductPriceInfo instance based on price amount, currency code, and billing period.
     *
     * @param discountPercent   The discount percentage.
     * @param priceAmount       The price amount.
     * @param priceCurrencyCode The currency code.
     * @param billingPeriod     The billing period.
     * @return A ProductPriceInfo instance.
     */
    @NonNull
    public static ProductPriceInfo fromPriceAmount(float discountPercent, float priceAmount, String priceCurrencyCode, BillingPeriod billingPeriod) {
        return new ProductPriceInfo(discountPercent, priceAmount, priceCurrencyCode, billingPeriod);
    }

    public final float discountPercent;
    public final float priceAmount;
    public final String priceCurrencyCode;
    public final BillingPeriod billingPeriod;

    ProductPriceInfo(float discountPercent, float priceAmount, String priceCurrencyCode, @NonNull BillingPeriod billingPeriod) {
        this.discountPercent = discountPercent;
        this.priceAmount = priceAmount;
        this.priceCurrencyCode = priceCurrencyCode;
        this.billingPeriod = billingPeriod;
    }

    /**
     * Get the formatted real price as a string.
     *
     * @return The formatted real price.
     */
    public String getRealPrice() {
        return getFormattedPrice(priceAmount);
    }

    /**
     * Get the formatted fake price as a string using the current price amount and discount percentage.
     *
     * @return The formatted fake price.
     */
    public String getFakePrice() {
        return getFakePrice(priceAmount, discountPercent);
    }

    /**
     * Get the formatted fake price as a string based on a given sale percentage.
     *
     * @param salePercent The sale percentage to apply to the price.
     * @return The formatted fake price.
     */
    public String getFakePrice(@FloatRange(from = 0f, to = 100f) float salePercent) {
        return getFakePrice(priceAmount, salePercent);
    }

    /**
     * Get the formatted fake price as a string based on a given price amount and sale percentage.
     *
     * @param priceAmount The price amount to apply the sale to.
     * @param salePercent The sale percentage to apply to the price.
     * @return The formatted fake price.
     */
    public String getFakePrice(float priceAmount, @FloatRange(from = 0f, to = 100f) float salePercent) {
        return getFormattedPrice(priceAmount / (100 - salePercent) * 100);
    }

    /**
     * Get avg period and avg price
     * <br/>return
     * <br/>- Pair.first = averagePeriod => {@link BillingPeriod#MONTH} || {@link BillingPeriod#DAY}
     * <br/>- Pair.second = averagePrice => price has been calculated
     */
    public Pair<BillingPeriod, String> getAveragePrice() {
        BillingPeriod averagePeriod;
        int averageTotalDay;
        if (billingPeriod.atLeast(BillingPeriod.QUARTER)) {
            averagePeriod = BillingPeriod.MONTH;
            averageTotalDay = billingPeriod.getTotalDay() / 30;
        } else {
            averagePeriod = BillingPeriod.DAY;
            averageTotalDay = billingPeriod.getTotalDay();
        }
        return new Pair<>(averagePeriod, getAveragePrice(averageTotalDay));
    }

    /**
     * Get the formatted average price as a string for a specified average.
     *
     * @param average The average value to use for calculating the price.
     * @return The formatted average price.
     */
    public String getAveragePrice(int average) {
        return getFormattedPrice(priceAmount / average);
    }

    /**
     * Get the formatted price as a string using the current price amount and currency code.
     *
     * @param price The price amount to format.
     * @return The formatted price as a string.
     */
    public String getFormattedPrice(float price) {
        return FormatUtils.formatMoney(price, priceCurrencyCode);
    }

    /**
     * Get the title of the billing period as a string using the provided context.
     *
     * @param context The Android context for resource retrieval.
     * @return The title of the billing period as a string.
     */
    public String getPeriodTitle(@NonNull Context context) {
        return getPeriodTitle(context, billingPeriod);
    }

    /**
     * Get the title of a billing period as a string using the provided context and billing period enum.
     *
     * @param context The Android context for resource retrieval.
     * @param period  The billing period enum.
     * @return The title of the billing period as a string.
     */
    public String getPeriodTitle(@NonNull Context context, @NonNull BillingPeriod period) {
        return context.getResources().getString(period.getTitleRes());
    }
}
