package com.mct.app.helper.iap.banner.component.normal;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mct.app.helper.iap.banner.Customizer;
import com.mct.app.helper.iap.banner.IapBanner;
import com.mct.app.helper.iap.banner.component.BaseComponent;
import com.mct.app.helper.iap.banner.component.BaseComponentAdapter;
import com.mct.app.helper.iap.banner.component.billing.ProductConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * CompositeComponent - A component that can contain multiple sub-components within an IapBanner.
 * <p>
 * This class extends the Component class and allows the creation of composite components that can
 * contain multiple sub-components, such as buttons, text, or other custom components. Composite
 * components facilitate the organization and customization of multiple components as a single unit
 * within an IapBanner.
 */
public class CompositeComponent<C extends CompositeComponent<C>> extends Component<C> {

    // Map to store sub-components associated with this composite component
    private final Map<Integer, BaseComponent> components = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    public CompositeComponent(int id) {
        super(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(@NonNull IapBanner banner, View root) {
        super.init(banner, root);
        if (view != null) {
            for (Map.Entry<Integer, BaseComponent> component : components.entrySet()) {
                component.getValue().init(banner, view);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release(@NonNull IapBanner banner, View root) {
        super.release(banner, root);
        if (view != null) {
            for (Map.Entry<Integer, BaseComponent> component : components.entrySet()) {
                component.getValue().release(banner, view);
            }
        }
    }

    /**
     * Get a reference to the sub-component's resource ID.
     *
     * @param id  The resource ID of the sub-component's view.
     * @param <T> The type of the sub-component's view.
     * @return The sub-component's view or null if not found.
     */
    @Nullable
    public <T extends View> T findViewById(@IdRes int id) {
        if (view == null) {
            return null;
        }
        return view.findViewById(id);
    }

    /**
     * Get a reference to a sub-component by its resource ID.
     *
     * @param id  The resource ID of the sub-component.
     * @param <T> The type of the sub-component.
     * @return The sub-component instance or null if not found.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends BaseComponent> T findComponentById(@IdRes int id) {
        return (T) components.get(id);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Component with Customizer
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Bind and customize a sub-component using a customizer.
     *
     * @param id         The resource ID of the sub-component's view.
     * @param customizer The customizer for the sub-component.
     * @return The composite component instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C bindView(@IdRes int id, @NonNull Customizer<Component<?>> customizer) {
        customizer.customize(bindView(id));
        return (C) this;
    }

    /**
     * Bind and customize a TextComponent sub-component using a customizer.
     *
     * @param id         The resource ID of the sub-component's view.
     * @param customizer The customizer for the sub-component.
     * @return The composite component instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C bindText(@IdRes int id, @NonNull Customizer<TextComponent<?>> customizer) {
        customizer.customize(bindText(id));
        return (C) this;
    }

    /**
     * Bind and customize a TimeComponent sub-component using a customizer.
     *
     * @param id         The resource ID of the sub-component's view.
     * @param customizer The customizer for the sub-component.
     * @return The composite component instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C bindTime(@IdRes int id, @NonNull Customizer<TimeComponent<?>> customizer) {
        customizer.customize(bindTime(id));
        return (C) this;
    }

    /**
     * Bind and customize a DismissComponent sub-component using a customizer.
     *
     * @param id         The resource ID of the sub-component's view.
     * @param customizer The customizer for the sub-component.
     * @return The composite component instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C bindDismiss(@IdRes int id, @NonNull Customizer<DismissComponent<?>> customizer) {
        customizer.customize(bindDismiss(id));
        return (C) this;
    }

    /**
     * Bind and customize a ClaimComponent sub-component using a customizer.
     *
     * @param id            The resource ID of the sub-component's view.
     * @param configuration The configuration.
     * @param customizer    The customizer for the sub-component.
     * @return The composite component instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C bindClaim(@IdRes int id, ProductConfiguration configuration, @NonNull Customizer<ClaimComponent<?>> customizer) {
        customizer.customize(bindClaim(id, configuration));
        return (C) this;
    }

    /**
     * Bind and customize a LazyTextComponent sub-component using a customizer.
     *
     * @param id            The resource ID of the TextView.
     * @param configuration The configuration.
     * @param customizer    The Customizer for configuring the component.
     * @return The composite component instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C bindLazyText(@IdRes int id, ProductConfiguration configuration, @NonNull Customizer<LazyTextComponent<?>> customizer) {
        customizer.customize(bindLazyText(id, configuration));
        return (C) this;
    }

    /**
     * Binds a custom subcomponent of type {@code SubComponent} to a specific view in the layout identified by its resource ID
     * while applying customizations using the provided customizer.
     *
     * @param id             The resource ID of the view to which the custom subcomponent will be bound.
     * @param c              The custom subcomponent instance to be bound.
     * @param customizer     A {@link Customizer} that customizes the provided subcomponent.
     * @param <SubComponent> The type of the custom subcomponent.
     * @return The current composite component instance with the custom subcomponent bound and customized.
     */
    @SuppressWarnings("unchecked")
    public <SubComponent extends BaseComponentAdapter> C bindComponent(
            @IdRes int id,
            @NonNull SubComponent c,
            @NonNull Customizer<SubComponent> customizer) {
        customizer.customize(bindComponent(id, c));
        return (C) this;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Component without Customizer
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Bind a sub-component using its resource ID.
     *
     * @param id The resource ID of the sub-component's view.
     * @return A {@link Component} instance bound to the specified view.
     */
    public Component<?> bindView(@IdRes int id) {
        return bindComponent(id, new Component<>(id));
    }

    /**
     * Bind a TextComponent sub-component using its resource ID.
     *
     * @param id The resource ID of the sub-component's view.
     * @return A {@link TextComponent} instance bound to the specified view.
     */
    public TextComponent<?> bindText(@IdRes int id) {
        return bindComponent(id, new TextComponent<>(id));
    }

    /**
     * Bind a TimeComponent sub-component using its resource ID.
     *
     * @param id The resource ID of the sub-component's view.
     * @return A {@link TimeComponent} instance bound to the specified view.
     */
    public TimeComponent<?> bindTime(@IdRes int id) {
        return bindComponent(id, new TimeComponent<>(id));
    }

    /**
     * Bind a DismissComponent sub-component using its resource ID.
     *
     * @param id The resource ID of the sub-component's view.
     * @return A {@link DismissComponent} instance bound to the specified view.
     */
    public DismissComponent<?> bindDismiss(@IdRes int id) {
        return bindComponent(id, new DismissComponent<>(id));
    }

    /**
     * Bind a ClaimComponent sub-component using its resource ID.
     *
     * @param id            The resource ID of the sub-component's view.
     * @param configuration The configuration.
     * @return A {@link ClaimComponent} instance bound to the specified view.
     */
    public ClaimComponent<?> bindClaim(@IdRes int id, ProductConfiguration configuration) {
        return bindComponent(id, new ClaimComponent<>(id)).setProductConfiguration(configuration);
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
     * Binds a custom subcomponent of type {@code SubComponent} to a specific view in the layout identified by its resource ID.
     *
     * @param id             The resource ID of the view to which the custom subcomponent will be bound.
     * @param c              The custom subcomponent instance to be bound.
     * @param <SubComponent> The type of the custom subcomponent.
     * @return The custom subcomponent instance after being bound to the view.
     */
    public <SubComponent extends BaseComponentAdapter> SubComponent bindComponent(
            @IdRes int id,
            @NonNull SubComponent c) {
        return getOrCreate(id, c);
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
        component.setBuilder(getBuilder());
        components.put(id, component);
        return (O) component;
    }
}
