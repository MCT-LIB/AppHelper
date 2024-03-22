package com.mct.app.helper.iap.banner;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.core.util.Supplier;

import com.mct.app.helper.iap.banner.component.BaseComponent;
import com.mct.app.helper.iap.banner.component.BaseComponentAdapter;
import com.mct.app.helper.iap.banner.component.billing.BillingComponent;
import com.mct.app.helper.iap.banner.component.billing.ProductConfiguration;
import com.mct.app.helper.iap.banner.component.normal.ClaimComponent;
import com.mct.app.helper.iap.banner.component.normal.Component;
import com.mct.app.helper.iap.banner.component.normal.CompositeComponent;
import com.mct.app.helper.iap.banner.component.normal.DismissComponent;
import com.mct.app.helper.iap.banner.component.normal.LazyTextComponent;
import com.mct.app.helper.iap.banner.component.normal.TextComponent;
import com.mct.app.helper.iap.banner.component.normal.TimeComponent;
import com.mct.app.helper.iap.banner.component.system.NavigationBarComponent;
import com.mct.app.helper.iap.banner.component.system.StatusBarComponent;
import com.mct.app.helper.iap.banner.countdown.Countdown;
import com.mct.app.helper.iap.banner.listener.OnBannerDismissListener;
import com.mct.app.helper.iap.banner.listener.OnBannerShowListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Class builder for {@link IapBanner}<br/>
 * This class provides a convenient way to create and configure {@link IapBanner} instances
 * with different components such as TextComponent, TimeComponent, DismissComponent, ClaimComponent,
 * StatusBarComponent, and NavigationBarComponent.
 * <p>
 * Usage example:
 * <code>
 * <pre>
 * ProductConfiguration monthConfiguration = ProductConfiguration.of(SUB_MONTH).build();
 * ProductConfiguration yearConfiguration = ProductConfiguration.of(SUB_YEAR)
 *         .withDiscountPercent(80)
 *         .withOfferIndex(0)
 *         .build();
 *
 * new IapBannerBuilder(context, R.layout.your_layout)
 *         .bindView(R.id.view).and()
 *         .bindView(R.id.view1, v -> v.setClickListener(listener))
 *         .bindViewGroup(R.id.view_group, vg -> {
 *             vg.bindView(id);
 *             vg.bindText(id);
 *         })
 *         .bindText(R.id.text, t -> t.text("").highlightText("").bold().strikeThru().underline().highlight(color))
 *         .bindTime(R.id.hour, t -> t.hour().bold().underline())
 *         .bindTime(R.id.minute, t -> t.minute())
 *         .bindTime(R.id.second, t -> t.second())
 *         .bindTime(R.id.millisecond, t -> t.millisecond())
 *         .bindDismiss(R.id.close_btn).and()
 *         .bindBilling(activity, billing -> billing
 *                 .addConsumable(consumableConfigurations)
 *                 .addNonConsumable(nonConsumableConfigurations)
 *                 .addSubscription(subscriptionConfigurations)
 *                 .addSubscription(monthConfiguration)
 *                 .addSubscription(yearConfiguration)
 *                 .autoConsume()
 *                 .autoAcknowledge()
 *                 .addBillingEventListener(new BillingEventListeners() {
 *                     // override func here
 *                 }))
 *         .bindClaim(R.id.claim_month_btn, monthConfiguration).and()
 *         .bindClaim(R.id.claim_year_btn, yearConfiguration).and()
 *         .bindLazyText(R.id.text_lazy, monthConfiguration, lzt -> lzt
 *                 .bold()
 *                 .strikeThru()
 *                 .lazyText((productInfo, productPriceInfo) -> {
 *                     String realPrice = productPriceInfo.getRealPrice();
 *                     String fakePrice = productPriceInfo.getFakePrice();
 *                     Pair&lt;BillingPeriod, String&gt; averagePrice = productPriceInfo.getAveragePrice();
 *                     return realPrice;
 *                 })
 *                 .lazyHighlightText((productInfo, productPriceInfo) -> {
 *                     String realPrice = productPriceInfo.getRealPrice();
 *                     String fakePrice = productPriceInfo.getFakePrice();
 *                     Pair&lt;BillingPeriod, String&gt; averagePrice = productPriceInfo.getAveragePrice();
 *                     return realPrice;
 *                 }))
 *         .bindStatusBar(status -> status
 *                 .background(Color.TRANSPARENT)
 *                 .lightAppearance()
 *                 .show())
 *         .bindNavigationBar(nav -> nav
 *                 .background(Color.TRANSPARENT)
 *                 .darkAppearance())
 *         .show(true);
 * </pre>
 * </code>
 *
 * @see IapBanner
 */
public class IapBannerBuilder {

    final Context context;
    final int layout;
    final Map<Integer, BaseComponent> components;
    final Countdown countDown = Countdown.newInstance();
    OnBannerShowListener onBannerShowListener;
    OnBannerDismissListener onBannerDismissListener;

    /**
     * Constructor for IapBannerBuilder.
     *
     * @param context The Android application context.
     * @param layout  The layout resource ID for the banner.
     */
    public IapBannerBuilder(@NonNull Context context, @LayoutRes int layout) {
        this.context = context;
        this.layout = layout;
        this.components = new HashMap<>();
        this.bindStatusBar();
        this.bindNavigationBar();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Component with Customizer
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Bind a Composite component and apply customizations using a Customizer.
     *
     * @param id         The resource ID of the ViewGroup.
     * @param customizer The Customizer for configuring the component.
     * @return The builder instance for method chaining.
     */
    public IapBannerBuilder bindViewGroup(@IdRes int id, @NonNull Customizer<CompositeComponent<?>> customizer) {
        customizer.customize(bindViewGroup(id));
        return this;
    }

    /**
     * Bind a View component and apply customizations using a Customizer.
     *
     * @param id         The resource ID of the View.
     * @param customizer The Customizer for configuring the component.
     * @return The builder instance for method chaining.
     */
    public IapBannerBuilder bindView(@IdRes int id, @NonNull Customizer<Component<?>> customizer) {
        customizer.customize(bindView(id));
        return this;
    }

    /**
     * Bind a Text component and apply customizations using a Customizer.
     *
     * @param id         The resource ID of the TextView.
     * @param customizer The Customizer for configuring the component.
     * @return The builder instance for method chaining.
     */
    public IapBannerBuilder bindText(@IdRes int id, @NonNull Customizer<TextComponent<?>> customizer) {
        customizer.customize(bindText(id));
        return this;
    }

    /**
     * Bind a Time component and apply customizations using a Customizer.
     *
     * @param id         The resource ID of the TextView.
     * @param customizer The Customizer for configuring the component.
     * @return The builder instance for method chaining.
     */
    public IapBannerBuilder bindTime(@IdRes int id, @NonNull Customizer<TimeComponent<?>> customizer) {
        customizer.customize(bindTime(id));
        return this;
    }

    /**
     * Bind a Dismiss component and apply customizations using a Customizer.
     *
     * @param id         The resource ID of the View.
     * @param customizer The Customizer for configuring the component.
     * @return The builder instance for method chaining.
     */
    public IapBannerBuilder bindDismiss(@IdRes int id, @NonNull Customizer<DismissComponent<?>> customizer) {
        customizer.customize(bindDismiss(id));
        return this;
    }

    /**
     * Bind a Claim component and apply customizations using a Customizer.
     *
     * @param id            The resource ID of the View.
     * @param configuration The configuration.
     * @param customizer    The Customizer for configuring the component.
     * @return The builder instance for method chaining.
     */
    public IapBannerBuilder bindClaim(@IdRes int id, ProductConfiguration configuration, @NonNull Customizer<ClaimComponent<?>> customizer) {
        customizer.customize(bindClaim(id, configuration));
        return this;
    }

    /**
     * Bind a LazyText component and apply customizations using a Customizer.
     *
     * @param id            The resource ID of the TextView.
     * @param configuration The configuration.
     * @param customizer    The Customizer for configuring the component.
     * @return The builder instance for method chaining.
     */
    public IapBannerBuilder bindLazyText(@IdRes int id, ProductConfiguration configuration, @NonNull Customizer<LazyTextComponent<?>> customizer) {
        customizer.customize(bindLazyText(id, configuration));
        return this;
    }

    /**
     * Bind a StatusBar component and apply customizations using a Customizer.
     *
     * @param customizer The Customizer for configuring the component.
     * @return The builder instance for method chaining.
     */
    public IapBannerBuilder bindStatusBar(@NonNull Customizer<StatusBarComponent> customizer) {
        customizer.customize(bindStatusBar());
        return this;
    }

    /**
     * Bind a NavigationBar component and apply customizations using a Customizer.
     *
     * @param customizer The Customizer for configuring the component.
     * @return The builder instance for method chaining.
     */
    public IapBannerBuilder bindNavigationBar(@NonNull Customizer<NavigationBarComponent> customizer) {
        customizer.customize(bindNavigationBar());
        return this;
    }

    /**
     * Bind a Billing component and apply customizations using a Customizer.
     *
     * @param activity   Activity to launchBillingFlow.
     * @param customizer The Customizer for configuring the component.
     * @return A BillingComponent instance to manager billing process.
     */
    public IapBannerBuilder bindBilling(@NonNull Activity activity, @NonNull Customizer<BillingComponent> customizer) {
        customizer.customize(bindBilling(activity));
        return this;
    }

    /**
     * Binds a custom component of type {@code C} to a specific view in the layout identified by its resource ID and applies customizations using a provided customizer.
     *
     * @param id         The resource ID of the view to which the custom component will be bound.
     * @param c          The custom component instance to be bound.
     * @param customizer The customizer used to apply specific customizations to the custom component.
     * @param <C>        The type of the custom component.
     * @return The builder instance for method chaining.
     */
    public <C extends BaseComponentAdapter> IapBannerBuilder bindComponent(
            @IdRes int id,
            @NonNull C c,
            @NonNull Customizer<C> customizer) {
        customizer.customize(bindComponent(id, c));
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Component without Customizer
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Bind a Composite component.
     *
     * @param id The resource ID of the ViewGroup.
     * @return A CompositeComponent instance bound to the specified view.
     */
    public CompositeComponent<?> bindViewGroup(@IdRes int id) {
        return bindComponent(id, new CompositeComponent<>(id));
    }

    /**
     * Bind a View component.
     *
     * @param id The resource ID of the View.
     * @return A Component instance bound to the specified view.
     */
    public Component<?> bindView(@IdRes int id) {
        return bindComponent(id, new Component<>(id));
    }

    /**
     * Bind a Text component.
     *
     * @param id The resource ID of the TextView.
     * @return A TextComponent instance bound to the specified view.
     */
    public TextComponent<?> bindText(@IdRes int id) {
        return bindComponent(id, new TextComponent<>(id));
    }

    /**
     * Bind a Time component.
     *
     * @param id The resource ID of the TextView.
     * @return A TimeComponent instance bound to the specified view.
     */
    public TimeComponent<?> bindTime(@IdRes int id) {
        return bindComponent(id, new TimeComponent<>(id));
    }

    /**
     * Bind a Dismiss component.
     *
     * @param id The resource ID of the View.
     * @return A DismissComponent instance bound to the specified view.
     */
    public DismissComponent<?> bindDismiss(@IdRes int id) {
        return bindComponent(id, new DismissComponent<>(id));
    }

    /**
     * Bind a Claim component.
     *
     * @param id            The resource ID of the View.
     * @param configuration The configuration.
     * @return A ClaimComponent instance bound to the specified view.
     */
    public ClaimComponent<?> bindClaim(@IdRes int id, ProductConfiguration configuration) {
        return bindComponent(id, new ClaimComponent<>(id)).setProductConfiguration(configuration);
    }

    /**
     * Bind a Claim component.
     *
     * @param id            The resource ID of the View.
     * @param configuration The configuration lazy.
     * @return A ClaimComponent instance bound to the specified view.
     */
    public ClaimComponent<?> bindClaim(@IdRes int id, Supplier<ProductConfiguration> configuration) {
        return bindComponent(id, new ClaimComponent<>(id)).setProductConfigurationLazy(configuration);
    }

    /**
     * Bind a LazyText component.
     *
     * @param id            The resource ID of the TextView.
     * @param configuration The configuration.
     * @return A LazyTextComponent instance bound to the specified view.
     */
    public LazyTextComponent<?> bindLazyText(@IdRes int id, ProductConfiguration configuration) {
        return bindComponent(id, new LazyTextComponent<>(id)).setProductConfiguration(configuration);
    }

    /**
     * Bind a StatusBar component.
     *
     * @return A StatusBarComponent instance bound to the specified view.
     */
    public StatusBarComponent bindStatusBar() {
        return bindComponent(StatusBarComponent.ID, new StatusBarComponent());
    }

    /**
     * Bind a NavigationBar component.
     *
     * @return A NavigationBarComponent instance bound to the specified view.
     */
    public NavigationBarComponent bindNavigationBar() {
        return bindComponent(NavigationBarComponent.ID, new NavigationBarComponent());
    }

    /**
     * Bind a Billing component.
     *
     * @param activity Activity to launchBillingFlow.
     * @return A BillingComponent instance to manager billing process.
     */
    public BillingComponent bindBilling(@NonNull Activity activity) {
        return bindComponent(BillingComponent.ID, new BillingComponent(activity));
    }

    /**
     * Binds a custom component of type {@code C} to a specific view in the layout identified by its resource ID.
     *
     * @param id  The resource ID of the view to which the custom component will be bound.
     * @param c   The custom component instance to be bound.
     * @param <C> The type of the custom component.
     * @return The bound custom component of type {@code C}.
     */
    public <C extends BaseComponentAdapter> C bindComponent(
            @IdRes int id,
            @NonNull C c) {
        return getOrCreate(id, c);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Other component customization methods...
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Set the countdown timer for the banner.
     *
     * @param time The countdown time in milliseconds.
     * @return The builder instance for method chaining.
     */
    public IapBannerBuilder setCountDownTime(long time) {
        countDown.setTime(time);
        return this;
    }

    /**
     * Set an OnBannerShowListener for the banner.
     *
     * @param listener The listener to be invoked when the banner is shown.
     * @return The builder instance for method chaining.
     */
    public IapBannerBuilder setOnBannerShowListener(OnBannerShowListener listener) {
        this.onBannerShowListener = listener;
        return this;
    }

    /**
     * Set an OnBannerDismissListener for the banner.
     *
     * @param listener The listener to be invoked when the banner is dismissed.
     * @return The builder instance for method chaining.
     */
    public IapBannerBuilder setOnBannerDismissListener(OnBannerDismissListener listener) {
        this.onBannerDismissListener = listener;
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Banner creation and display methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Build an instance of IapBanner with the configured components.
     *
     * @return An instance of IapBanner.
     */
    public IapBanner build() {
        return new IapBanner(this);
    }

    /**
     * Show the banner with the option to make it full-screen.
     *
     * @param fullScreen Set to true to display the banner as full-screen.
     */
    public void show(boolean fullScreen) {
        build().show(fullScreen);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private utility methods...
    ///////////////////////////////////////////////////////////////////////////

    @NonNull
    @SuppressWarnings("unchecked")
    private <O extends BaseComponent> O getOrCreate(int id, BaseComponentAdapter component) {
        if (components.containsKey(id)) {
            BaseComponent c = components.get(id);
            if (c != null && component.getClass().equals(c.getClass())) {
                return (O) c;
            }
        }
        component.setBuilder(this);
        components.put(id, component);
        return (O) component;
    }
}
