package com.mct.app.helper.iap.banner.component.normal;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.util.Supplier;

import com.mct.app.helper.iap.banner.IapBanner;
import com.mct.app.helper.iap.banner.component.billing.BillingComponent;
import com.mct.app.helper.iap.banner.component.billing.ProductConfiguration;

/**
 * ClaimComponent - A component for enabling users to claim an offer within an IapBanner.
 * <p>
 * The {@link ClaimComponent} class is designed to create a clickable component within an `IapBanner`
 * that allows users to claim offers associated with specific products. It provides a convenient
 * way to handle offer claiming interactions within your app's user interface.
 *
 * @param <C> - The type of the {@link ClaimComponent} for method chaining.
 */
public class ClaimComponent<C extends ClaimComponent<C>> extends Component<C> {

    private ProductConfiguration productConfiguration;
    private Supplier<ProductConfiguration> productConfigurationLazy;

    /**
     * {@inheritDoc}
     */
    public ClaimComponent(int id) {
        super(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(@NonNull IapBanner banner, View root) {
        super.init(banner, root);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release(@NonNull IapBanner banner, View root) {
        super.release(banner, root);
    }


    /**
     * Sets up an `OnClickListener` for the {@link ClaimComponent} to handle the claim action.
     *
     * @param banner - The parent `IapBanner` to which the component belongs.
     * @param root   - The root View of the layout.
     */
    @Override
    protected void setupOnClickListener(@NonNull IapBanner banner, View root) {
        if (view != null) {
            view.setOnClickListener(v -> {
                ProductConfiguration configuration = productConfiguration;
                if (configuration == null && productConfigurationLazy != null) {
                    configuration = productConfigurationLazy.get();
                }
                BillingComponent component = banner.findComponentById(BillingComponent.ID);
                if (component != null && configuration != null) {
                    component.subscribe(configuration);
                }
                if (clickListener != null) {
                    clickListener.onClick(v);
                }
            });
        }
    }

    /**
     * Sets the product ID for which the offer will be claimed.
     *
     * @param productConfiguration - The product configuration to set.
     * @return The {@link ClaimComponent} instance for method chaining.
     */
    @SuppressWarnings("unchecked")
    public C setProductConfiguration(ProductConfiguration productConfiguration) {
        this.productConfiguration = productConfiguration;
        return (C) this;
    }

    /**
     * Sets the product ID for which the offer will be claimed.
     *
     * @param productConfigurationLazy - The product configuration lazy to set.
     * @return The {@link ClaimComponent} instance for method chaining.
     */
    @SuppressWarnings("unchecked")
    public C setProductConfigurationLazy(Supplier<ProductConfiguration> productConfigurationLazy) {
        this.productConfigurationLazy = productConfigurationLazy;
        return (C) this;
    }
}
