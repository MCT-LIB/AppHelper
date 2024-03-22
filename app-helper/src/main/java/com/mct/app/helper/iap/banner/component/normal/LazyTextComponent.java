package com.mct.app.helper.iap.banner.component.normal;

import android.view.View;

import androidx.annotation.NonNull;

import com.mct.app.helper.iap.banner.IapBanner;
import com.mct.app.helper.iap.banner.component.billing.BillingComponent;
import com.mct.app.helper.iap.banner.component.billing.BillingEventListeners;
import com.mct.app.helper.iap.banner.component.billing.ProductConfiguration;
import com.mct.app.helper.iap.banner.component.billing.ProductPriceInfo;
import com.mct.app.helper.iap.billing.models.ProductInfo;

import java.util.List;
import java.util.Objects;

/**
 * {@link LazyTextComponent} is a component used in an {@link IapBanner} to display text that
 * can be loaded lazily based on product information. It extends the {@link TextComponent} class.
 * <p>
 * This component allows you to specify a product ID and provide callbacks to load text and
 * highlight text dynamically when product information is fetched from a billing component.
 * This can be useful when you want to display product-related information that depends on
 * the availability of specific products.
 * <p>
 * Usage:
 * To use the {@link LazyTextComponent}, create an instance of it with a unique ID and set the
 * product ID, lazy text loader, and lazy highlight text loader using the provided methods.
 * When product information is fetched, the {@link LazyTextComponent} will update its text and
 * highlight text based on the loaded data.
 * <p>
 * Example Usage:
 * <code>
 * <pre>
 * LazyTextComponent lazyTextComponent =
 *      new LazyTextComponent(R.id.lazy_text_component)
 *     .setProductId("your_product_id")
 *     .lazyText(productInfo -> {})
 *     .lazyHighlightText(productInfo -> {});
 * </pre>
 * </code>
 *
 * @param <C> - The type of the {@link LazyTextComponent} for method chaining.
 */
public class LazyTextComponent<C extends LazyTextComponent<C>> extends TextComponent<C> {

    private ProductConfiguration productConfiguration;
    private LazyLoadText lazyLoadText;
    private LazyLoadText lazyLoadHighlightText;

    private final BillingEventListeners listenerAdapter = new BillingEventListeners() {
        @Override
        public void onProductsFetched(@NonNull IapBanner banner, @NonNull List<ProductInfo> productInfos) {
            // product is null -> ignore
            if (productConfiguration == null) {
                return;
            }
            // Callback triggered when product information is fetched
            // Load text and highlight text based on productInfo
            // Update the component's text if it has changed
            ProductInfo productInfo = productInfos.stream()
                    .filter(p -> Objects.equals(p.getProduct(), productConfiguration.getProductId()))
                    .findFirst()
                    .orElse(null);
            if (productInfo != null) {
                String text = null;
                String highlightText = null;
                ProductPriceInfo productPriceInfo = ProductPriceInfo.fromProductInfo(productConfiguration, productInfo);
                if (lazyLoadText != null) {
                    text = lazyLoadText.lazy(productInfo, productPriceInfo);
                }
                if (lazyLoadHighlightText != null) {
                    highlightText = lazyLoadHighlightText.lazy(productInfo, productPriceInfo);
                }
                if (!Objects.equals(getText(), text) || !Objects.equals(getHighlightText(), highlightText)) {
                    text(text).highlightText(highlightText).setText();
                }
            }
        }
    };

    /**
     * {@inheritDoc}
     */
    public LazyTextComponent(int id) {
        super(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(@NonNull IapBanner banner, View root) {
        super.init(banner, root);
        BillingComponent component = banner.findComponentById(BillingComponent.ID);
        if (component != null) {
            component.addBillingEventListener(listenerAdapter);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release(@NonNull IapBanner banner, View root) {
        super.release(banner, root);
        BillingComponent component = banner.findComponentById(BillingComponent.ID);
        if (component != null) {
            component.removeBillingEventListener(listenerAdapter);
        }
    }

    /**
     * Sets the product ID for which the text will be loaded lazily.
     *
     * @param productConfiguration - The product configuration to set.
     * @return The {@link LazyTextComponent} instance for method chaining.
     */
    @SuppressWarnings("unchecked")
    public C setProductConfiguration(ProductConfiguration productConfiguration) {
        this.productConfiguration = productConfiguration;
        return (C) this;
    }

    /**
     * Sets a lazy text loader to load text based on product information.
     *
     * @param lazyLoadText - The {@link LazyLoadText} instance to load text.
     * @return The {@link LazyTextComponent} instance for method chaining.
     */
    @SuppressWarnings("unchecked")
    public C lazyText(LazyLoadText lazyLoadText) {
        this.lazyLoadText = lazyLoadText;
        return (C) this;
    }

    /**
     * Sets a lazy highlight text loader to load highlight text based on product information.
     *
     * @param lazyLoadHighlightText - The {@link LazyLoadText} instance to load highlight text.
     * @return The {@link LazyTextComponent} instance for method chaining.
     */
    @SuppressWarnings("unchecked")
    public C lazyHighlightText(LazyLoadText lazyLoadHighlightText) {
        this.lazyLoadHighlightText = lazyLoadHighlightText;
        return (C) this;
    }

    /**
     * A callback interface to load text based on product information.
     */
    public interface LazyLoadText {
        String lazy(ProductInfo productInfo, ProductPriceInfo productPriceInfo);
    }
}
