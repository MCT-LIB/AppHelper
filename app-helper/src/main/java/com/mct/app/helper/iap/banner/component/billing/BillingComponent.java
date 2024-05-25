package com.mct.app.helper.iap.banner.component.billing;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;

import com.mct.app.helper.R;
import com.mct.app.helper.iap.banner.IapBanner;
import com.mct.app.helper.iap.banner.component.BaseComponentAdapter;
import com.mct.app.helper.iap.billing.BillingConnector;
import com.mct.app.helper.iap.billing.BillingEventListener;
import com.mct.app.helper.iap.billing.enums.PurchasedResult;
import com.mct.app.helper.iap.billing.models.BillingResponse;
import com.mct.app.helper.iap.billing.models.ProductInfo;
import com.mct.app.helper.iap.billing.models.PurchaseInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BillingComponent manages in-app purchase functionality for fetching product
 * listings and purchase information. It handles connectivity to the billing
 * library and notifies listeners of purchase events.
 * <p>
 * The BillingComponent is designed to facilitate in-app purchases within your Android
 * application. It connects to the billing library, fetches product details, handles
 * purchases, and provides callbacks for purchase-related events.
 * <p>
 * Usage:
 * To use the BillingComponent, create an instance of it with an Activity and an optional
 * base64 key. You can add consumable, non-consumable, and subscription product IDs to
 * fetch listings for. You can also configure auto-acknowledgment and auto-consumption
 * of purchases, enable logging, and add listeners for purchase events.
 * <p>
 * Example Usage:
 * <code>
 * <pre>
 * BillingComponent billingComponent = new BillingComponent(activity, "YOUR_BASE64_KEY")
 *     .addConsumableIds("consumable_product_id")
 *     .addNonConsumableIds("non_consumable_product_id")
 *     .addSubscriptionIds("subscription_product_id")
 *     .autoAcknowledge()
 *     .autoConsume()
 *     .enableLogging()
 *     .addBillingEventListener(billingEventListener);
 * </pre>
 * </code>
 */
public class BillingComponent extends BaseComponentAdapter {

    /**
     * The ID for the billing component.
     */
    public static final int ID = R.id.iap_component_billing;

    private final Activity activity;
    private final BillingConnector connector;
    private final List<BillingEventListeners> listeners;

    private final Set<ProductConfiguration> consumableStrategies = new HashSet<>();
    private final Set<ProductConfiguration> nonConsumableStrategies = new HashSet<>();
    private final Set<ProductConfiguration> subscriptionStrategies = new HashSet<>();

    /**
     * BillingComponent public constructor
     *
     * @param activity - is the activity
     */
    public BillingComponent(Activity activity) {
        this(activity, null);
    }

    /**
     * BillingComponent public constructor
     *
     * @param activity  - is the activity
     * @param base64Key - The public developer key from Play Console (optional).
     */
    public BillingComponent(Activity activity, String base64Key) {
        this.activity = activity;
        this.connector = new BillingConnector(activity, base64Key);
        this.listeners = new ArrayList<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(@NonNull IapBanner banner, View root) {
        super.init(banner, root);
        connector.setConsumableIds(consumableStrategies.stream().map(ProductConfiguration::getProductId).collect(Collectors.toList()));
        connector.setNonConsumableIds(nonConsumableStrategies.stream().map(ProductConfiguration::getProductId).collect(Collectors.toList()));
        connector.setSubscriptionIds(subscriptionStrategies.stream().map(ProductConfiguration::getProductId).collect(Collectors.toList()));
        connector.setBillingEventListener(new BillingEvent(banner, listeners));
        connector.connect();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release(@NonNull IapBanner banner, View root) {
        super.release(banner, root);
        connector.release();
        listeners.clear();
    }

    /**
     * Adds a consumable product ID to fetch listings for.
     *
     * @param consumable - The consumable products ID to be added.
     * @return The {@link BillingComponent} instance for method chaining.
     */
    public BillingComponent addConsumable(ProductConfiguration... consumable) {
        add(this.consumableStrategies, consumable);
        return this;
    }

    /**
     * Adds a non-consumable product ID to fetch listings for.
     *
     * @param nonConsumable - The non-consumable product ID to be added.
     * @return The {@link BillingComponent} instance for method chaining.
     */
    public BillingComponent addNonConsumable(ProductConfiguration... nonConsumable) {
        add(this.nonConsumableStrategies, nonConsumable);
        return this;
    }

    /**
     * Adds a subscription product ID to fetch listings for.
     *
     * @param subscription - The subscription product ID to be added.
     * @return The {@link BillingComponent} instance for method chaining.
     */
    public BillingComponent addSubscription(ProductConfiguration... subscription) {
        add(this.subscriptionStrategies, subscription);
        return this;
    }

    /**
     * Configures auto-acknowledgment of purchases. When enabled, purchases are automatically acknowledged.
     *
     * @return The {@link BillingComponent} instance for method chaining.
     */
    public BillingComponent autoAcknowledge() {
        connector.autoAcknowledge();
        return this;
    }

    /**
     * Configures auto-consumption of consumable purchases. When enabled, consumable purchases are automatically consumed.
     *
     * @return The {@link BillingComponent} instance for method chaining.
     */
    public BillingComponent autoConsume() {
        connector.autoConsume();
        return this;
    }

    /**
     * Enables logging from the billing library.
     *
     * @return The {@link BillingComponent} instance for method chaining.
     */
    public BillingComponent enableLogging() {
        connector.enableLogging();
        return this;
    }

    /**
     * Adds a listener for purchase events.
     *
     * @param listener - The BillingEventListener to be added.
     * @return The {@link BillingComponent} instance for method chaining.
     */
    public BillingComponent addBillingEventListener(BillingEventListeners listener) {
        if (listener == null) {
            return this;
        }
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
        return this;
    }

    /**
     * Removes a purchase event listener.
     *
     * @param listener - The BillingEventListener to be removed.
     * @return The {@link BillingComponent} instance for method chaining.
     */
    public BillingComponent removeBillingEventListener(BillingEventListeners listener) {
        if (listener == null) {
            return this;
        }
        listeners.remove(listener);
        return this;
    }

    /**
     * Subscribes to a subscription product.
     *
     * @param product - The product to subscribe.
     */
    public final void subscribe(@NonNull ProductConfiguration product) {
        connector.subscribe(activity, product.getProductId(), product.getPlanId(), product.getOfferId());
    }

    /**
     * Consumes a purchased product.
     *
     * @param purchaseInfo - The PurchaseInfo object representing the purchased product to be consumed.
     */
    public final void consumePurchase(PurchaseInfo purchaseInfo) {
        connector.consumePurchase(purchaseInfo);
    }

    /**
     * Acknowledges a purchased product.
     *
     * @param purchaseInfo - The PurchaseInfo object representing the purchased product to be acknowledged.
     */
    public final void acknowledgePurchase(PurchaseInfo purchaseInfo) {
        connector.acknowledgePurchase(purchaseInfo);
    }

    /**
     * Unsubscribes from a subscription product.
     *
     * @param activity    - The activity to use for subscribing.
     * @param packageName - The package name of the application.
     * @param productId   - The ID of the subscription product to unsubscribe from.
     */
    public final void unsubscribe(Activity activity, String packageName, String productId) {
        connector.unsubscribe(activity, packageName, productId);
    }

    /**
     * Checks if a product has been purchased.
     *
     * @param productInfo - The ProductInfo object representing the product to check.
     * @return A {@link PurchasedResult} indicating whether the product has been purchased.
     */
    public final PurchasedResult isPurchased(@NonNull ProductInfo productInfo) {
        return connector.isPurchased(productInfo);
    }

    public List<ProductConfiguration> getConsumableStrategies() {
        return new ArrayList<>(consumableStrategies);
    }

    public List<ProductConfiguration> getNonConsumableStrategies() {
        return new ArrayList<>(nonConsumableStrategies);
    }

    public List<ProductConfiguration> getSubscriptionStrategies() {
        return new ArrayList<>(subscriptionStrategies);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helper class & methods
    ///////////////////////////////////////////////////////////////////////////

    private static class BillingEvent implements BillingEventListener {

        private final IapBanner banner;
        private final List<BillingEventListeners> listeners;

        private BillingEvent(IapBanner banner, List<BillingEventListeners> listeners) {
            this.banner = banner;
            this.listeners = listeners;
        }

        @Override
        public void onProductsFetched(@NonNull List<ProductInfo> productDetails) {
            for (BillingEventListeners listener : listeners) {
                listener.onProductsFetched(banner, productDetails);
            }
        }

        @Override
        public void onPurchasedProductsFetched(@NonNull List<PurchaseInfo> purchases) {
            for (BillingEventListeners listener : listeners) {
                listener.onPurchasedProductsFetched(banner, purchases);
            }
        }

        @Override
        public void onProductsPurchased(@NonNull List<PurchaseInfo> purchases) {
            for (BillingEventListeners listener : listeners) {
                listener.onProductsPurchased(banner, purchases);
            }
        }

        @Override
        public void onPurchaseAcknowledged(@NonNull PurchaseInfo purchase) {
            for (BillingEventListeners listener : listeners) {
                listener.onPurchaseAcknowledged(banner, purchase);
            }
        }

        @Override
        public void onPurchaseConsumed(@NonNull PurchaseInfo purchase) {
            for (BillingEventListeners listener : listeners) {
                listener.onPurchaseConsumed(banner, purchase);
            }
        }

        @Override
        public void onBillingError(@NonNull BillingConnector billingConnector, @NonNull BillingResponse response) {
            for (BillingEventListeners listener : listeners) {
                listener.onBillingError(banner, billingConnector, response);
            }
        }
    }

    /**
     * Adds a String ids to a List.
     *
     * @param list - The List to which the ids is added.
     * @param ids  - The String ids to be added.
     */
    private void add(@NonNull Set<ProductConfiguration> list, ProductConfiguration... ids) {
        if (ids != null) {
            list.addAll(Arrays.asList(ids));
        }
    }

}
