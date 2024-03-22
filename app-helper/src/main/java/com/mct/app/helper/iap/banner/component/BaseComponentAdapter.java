package com.mct.app.helper.iap.banner.component;

import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import com.mct.app.helper.iap.banner.IapBanner;
import com.mct.app.helper.iap.banner.IapBannerBuilder;

/**
 * BaseComponentAdapter - Abstract class for adapting components to be added to an IapBanner.
 * <p>
 * This abstract class serves as a base for creating component adapters that can be added
 * to an {@link IapBanner} using the builder pattern. It provides methods for initialization,
 * resource release, and access to the parent {@link IapBannerBuilder} instance to facilitate
 * component customization and configuration.
 */
public abstract class BaseComponentAdapter implements BaseComponent {

    private IapBannerBuilder bannerBuilder;

    /**
     * {@inheritDoc}
     */
    @CallSuper
    @Override
    public void init(@NonNull IapBanner banner, View root) {
    }

    /**
     * {@inheritDoc}
     */
    @CallSuper
    @Override
    public void release(@NonNull IapBanner banner, View root) {
    }

    /**
     * Get the parent IapBannerBuilder instance associated with the component adapter.
     *
     * @return The parent IapBannerBuilder.
     */
    protected IapBannerBuilder getBuilder() {
        return bannerBuilder;
    }

    /**
     * Set the parent IapBannerBuilder instance for the component adapter.
     *
     * @param builder The parent IapBannerBuilder instance.
     */
    public void setBuilder(IapBannerBuilder builder) {
        this.bannerBuilder = builder;
    }

    /**
     * Chain the parent IapBannerBuilder instance to continue building the banner.
     *
     * @return The parent IapBannerBuilder instance.
     */
    public IapBannerBuilder and() {
        return getBuilder();
    }

    /**
     * Build and return the IapBanner instance associated with the parent builder.
     *
     * @return The IapBanner instance.
     */
    public IapBanner build() {
        return getBuilder().build();
    }

    /**
     * Show the banner with the option to make it full-screen using the parent builder.
     *
     * @param fullScreen Set to true to display the banner as full-screen.
     */
    public void show(boolean fullScreen) {
        build().show(fullScreen);
    }

}
