package com.mct.app.helper.iap.banner.component;

import android.view.View;

import androidx.annotation.NonNull;

import com.mct.app.helper.iap.banner.IapBanner;

/**
 * BaseComponent - Interface for components that can be added to an IapBanner.
 * <p>
 * This interface defines methods for initializing and releasing components that can be
 * added to an {@link IapBanner}. Components can include various visual elements or
 * functionalities such as text, buttons, countdown timers, etc.
 * Classes implementing this interface should provide implementations for
 * initialization and resource release.
 */
public interface BaseComponent {

    /**
     * Initialize the component within the IapBanner.
     *
     * @param banner The IapBanner instance to which the component belongs.
     * @param root   The root View of the Component.
     */
    void init(@NonNull IapBanner banner, View root);

    /**
     * Release resources associated with the component when it's no longer needed.
     *
     * @param banner The IapBanner instance from which the component is released.
     * @param root   The root View of the Component.
     */
    void release(@NonNull IapBanner banner, View root);
}
