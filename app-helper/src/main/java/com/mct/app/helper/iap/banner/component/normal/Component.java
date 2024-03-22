package com.mct.app.helper.iap.banner.component.normal;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mct.app.helper.iap.banner.IapBanner;
import com.mct.app.helper.iap.banner.component.BaseComponentAdapter;

/**
 * Component - A base class for creating components within an IapBanner.
 * <p>
 * This class extends the BaseComponentAdapter and serves as a base class for creating components
 * to be added to an IapBanner. Components can represent various UI elements
 * and functionalities such as buttons, text, or custom views that can be customized
 * and interacted with.
 * <code>
 * <pre>
 * Usage example:
 *
 * // Create a custom component extending the Component class
 * public class CustomComponent extends Component<CustomComponent> {
 *     public CustomComponent(@IdRes int id) {
 *         super(id);
 *     }
 *
 *     // Implement customizations and behavior for the component
 *     // ...
 * }
 *
 * // Add the custom component to an IapBannerBuilder
 * IapBannerBuilder builder = new IapBannerBuilder(context, R.layout.your_layout);
 * builder.bindView(R.id.custom_component, component -> component
 *         .setClickListener(view -> {
 *             // Handle click event on the custom component
 *         })
 * );
 * builder.show(true); // Display the banner with the custom component
 *
 * </pre>
 * </code>
 */
public class Component<C extends Component<C>> extends BaseComponentAdapter {

    @IdRes
    protected final int id; // The resource ID of the component's view
    @Nullable
    protected View.OnClickListener clickListener; // Optional click listener for the component

    @Nullable
    protected View view;    // Reference to the component's view

    /**
     * Constructor for the Component class.
     *
     * @param id The resource ID of the component's view.
     */
    public Component(int id) {
        this.id = id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(@NonNull IapBanner banner, View root) {
        super.init(banner, root);
        view = root.findViewById(id);
        setupOnClickListener(banner, root);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release(@NonNull IapBanner banner, View root) {
        super.release(banner, root);
        view = null;
    }

    /**
     * Get the component's associated view.
     *
     * @return The View object associated with the component.
     */
    @Nullable
    public View getView() {
        return view;
    }


    /**
     * Set a click listener for the component's view.
     *
     * @param clickListener The OnClickListener to be set for the component's view.
     * @return The component instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C setClickListener(@Nullable View.OnClickListener clickListener) {
        this.clickListener = clickListener;
        return (C) this;
    }

    /**
     * Setup the OnClickListener for the component's view if both view and clickListener are provided.
     *
     * @param banner The IapBanner instance to which the component belongs.
     * @param root   The root View of the IapBanner layout.
     */
    protected void setupOnClickListener(@NonNull IapBanner banner, View root) {
        if (view != null && clickListener != null) {
            view.setOnClickListener(clickListener);
        }
    }

}
