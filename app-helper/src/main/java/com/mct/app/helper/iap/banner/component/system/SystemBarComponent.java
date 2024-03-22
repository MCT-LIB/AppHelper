package com.mct.app.helper.iap.banner.component.system;

import android.graphics.Color;
import android.view.View;
import android.view.Window;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.mct.app.helper.iap.banner.IapBanner;
import com.mct.app.helper.iap.banner.component.BaseComponentAdapter;

/**
 * SystemBarComponent - A component for customizing the system bars of an IapBanner.
 * <p>
 * The SystemBarComponent class allows you to create a component within an IapBanner that can
 * customize the appearance and behavior of system bars (status bar and navigation bar).
 * You can set the background color, appearance (light or dark), and visibility of the system bars
 * using this component.
 */
public abstract class SystemBarComponent<C extends SystemBarComponent<C>> extends BaseComponentAdapter {

    /**
     * Use this instead of {@link Color#TRANSPARENT}<br/>
     * Fix: some samsung device when set system bar color is {@link Color#TRANSPARENT}
     * and then the system bar will take semi-transparent color(#80000000)
     */
    protected static final int TRANSPARENT = Color.parseColor("#01000000");

    @ColorInt
    protected int background;
    protected boolean isLight;
    protected boolean isShow;

    /**
     * Constructor for the SystemBarComponent class.
     * Initializes the component with default settings.
     */
    public SystemBarComponent() {
        this.backgroundTransparent().lightAppearance().show();
    }

    /**
     * Apply the component's settings to a Window.
     *
     * @param window The Window object to which the component's settings should be applied.
     */
    public abstract void applyToWindow(@NonNull Window window);

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
     * Set the background color of the system bars to transparent.
     *
     * @return The SystemBarComponent instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C backgroundTransparent() {
        this.background = TRANSPARENT;
        return (C) this;
    }

    /**
     * Set the background color of the system bars.
     *
     * @param background The background color to set for the system bars.
     * @return The SystemBarComponent instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C background(@ColorInt int background) {
        this.background = background;
        return (C) this;
    }

    /**
     * Set the system bar appearance to light, which changes text and icons on it to white.
     *
     * @return The SystemBarComponent instance to enable method chaining.
     */
    public C lightAppearance() {
        return appearance(true);
    }

    /**
     * Set the system bar appearance to dark, which changes text and icons on it to black.
     *
     * @return The SystemBarComponent instance to enable method chaining.
     */
    public C darkAppearance() {
        return appearance(false);
    }

    /**
     * Set the system bar appearance
     *
     * @param isLight - true: light appearance, false: dark appearance
     * @return The SystemBarComponent instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C appearance(boolean isLight) {
        this.isLight = !isLight;
        return (C) this;
    }

    /**
     * Show the system bars.
     *
     * @return The SystemBarComponent instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C show() {
        isShow = true;
        return (C) this;
    }

    /**
     * Hide the system bars.
     *
     * @return The SystemBarComponent instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C hide() {
        isShow = false;
        return (C) this;
    }

}
