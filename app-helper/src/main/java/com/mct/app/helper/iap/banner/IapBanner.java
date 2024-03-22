package com.mct.app.helper.iap.banner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mct.app.helper.iap.banner.component.BaseComponent;
import com.mct.app.helper.iap.banner.countdown.Countdown;
import com.mct.app.helper.iap.banner.listener.OnBannerDismissListener;
import com.mct.app.helper.iap.banner.listener.OnBannerShowListener;

import java.util.Map;

/**
 * IapBanner - Class for managing and displaying custom banners.
 * <p>
 * This class is responsible for initializing, displaying, and handling custom banners
 * that can contain various components like text, countdown timers, system bar and more.
 */
public class IapBanner implements View.OnAttachStateChangeListener {

    private final Context context;
    private final int layout;
    private final Map<Integer, BaseComponent> components;
    private final Countdown countdown;
    private final OnBannerShowListener bannerShowListener;
    private final OnBannerDismissListener bannerDismissListener;

    private View root;
    private IapBannerDialog dialog;

    /**
     * Constructor for IapBanner.
     *
     * @param builder The builder used to configure the banner.
     */
    public IapBanner(@NonNull IapBannerBuilder builder) {
        this.context = builder.context;
        this.layout = builder.layout;
        this.components = builder.components;
        this.countdown = builder.countDown;
        this.bannerShowListener = builder.onBannerShowListener;
        this.bannerDismissListener = builder.onBannerDismissListener;
    }

    @Override
    public void onViewAttachedToWindow(@NonNull View v) {
        countdown.startCountDown();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull View v) {
        countdown.stopCountDown();
    }

    /**
     * Initialize the banner. This method should be called before displaying the banner.
     * It inflates the banner layout, initializes components, and sets up countdown timers.
     */
    @CallSuper
    public void init() {
        if (root != null) {
            return;
        }
        root = LayoutInflater.from(context).inflate(layout, null);
        root.removeOnAttachStateChangeListener(this);
        root.addOnAttachStateChangeListener(this);

        countdown.release();
        countdown.init();
        for (Map.Entry<Integer, BaseComponent> component : components.entrySet()) {
            component.getValue().init(this, root);
        }
    }

    /**
     * Release resources associated with the banner. This method should be called when
     * the banner is no longer needed to release resources and listeners.
     */
    @CallSuper
    public void release() {
        if (root == null) {
            return;
        }
        for (Map.Entry<Integer, BaseComponent> component : components.entrySet()) {
            component.getValue().release(this, root);
        }
        countdown.release();
        root.removeOnAttachStateChangeListener(this);
        root = null;
    }

    /**
     * Get the context associated with the banner.
     *
     * @return The Android application context.
     */
    public Context getContext() {
        return context;
    }


    /**
     * Get the countdown timer associated with the banner.
     *
     * @return The Countdown instance for managing the countdown.
     */
    public Countdown getCountdown() {
        return countdown;
    }

    /**
     * Get the root view of the banner.
     *
     * @return The root View of the banner layout.
     */
    public View getRoot() {
        return root;
    }

    /**
     * Find a child view within the banner layout by its resource ID.
     *
     * @param id The resource ID of the view to find.
     * @return The found View or null if not found.
     */
    @Nullable
    public <T extends View> T findViewById(@IdRes int id) {
        if (root == null) {
            return null;
        }
        return root.findViewById(id);
    }

    /**
     * Find a component within the banner by its resource ID.
     *
     * @param id The resource ID of the component to find.
     * @return The found BaseComponent or null if not found.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends BaseComponent> T findComponentById(@IdRes int id) {
        return (T) components.get(id);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Area Dialog
    ///////////////////////////////////////////////////////////////////////////

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    /**
     * Show the banner with the option to make it full-screen.
     *
     * @param fullScreen Set to true to display the banner as full-screen.
     */
    public void show(boolean fullScreen) {
        if (isShowing()) {
            return;
        }
        init();
        if (dialog == null) {
            dialog = new IapBannerDialog(this, fullScreen);
            dialog.setOnShowListener(dialog -> {
                if (bannerShowListener != null) {
                    bannerShowListener.onShow(this, dialog);
                }
            });
            dialog.setOnDismissListener(dialog -> {
                dismiss();
                if (bannerDismissListener != null) {
                    bannerDismissListener.onDismiss(this, dialog);
                }
            });
        }
        dialog.show();
    }

    /**
     * Dismiss the banner and release associated resources.
     */
    public void dismiss() {
        if (!isShowing()) {
            return;
        }
        release();
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

}
