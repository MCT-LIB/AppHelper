package com.mct.app.helper.iap.banner.component.system;

import android.graphics.Color;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.mct.app.helper.R;

/**
 * StatusBarComponent - A component for customizing the status bar of an IapBanner.
 * <p>
 * The StatusBarComponent class allows you to create a component within an IapBanner that can
 * customize the appearance and behavior of the status bar.
 * You can set the background color, appearance (light or dark), and visibility of the status bar
 * using this component.
 * <p>
 * Usage example:
 * <code>
 * <pre>
 * // Create a StatusBarComponent and apply it to a Window
 * StatusBarComponent statusBarComponent = new StatusBarComponent()
 *         // Set the status bar background color
 *         .background(Color.WHITE)
 *         // Set the status bar appearance to dark
 *         .darkAppearance()
 *         // Show the status bar
 *         .show();
 * // Apply the component to a Window
 * statusBarComponent.applyToWindow(window);
 * </pre>
 * </code>
 */
public class StatusBarComponent extends SystemBarComponent<StatusBarComponent> {

    /**
     * The ID for the status bar component.
     */
    public static final int ID = R.id.iap_component_sys_bar_status;

    public StatusBarComponent() {
    }

    /**
     * Apply the component's settings to a Window.
     *
     * @param window The Window object to which the component's settings should be applied.
     */
    @Override
    public void applyToWindow(@NonNull Window window) {
        // Background
        window.setStatusBarColor(background == Color.TRANSPARENT ? TRANSPARENT : background);
        // Create controller
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        // Appearance
        controller.setAppearanceLightStatusBars(isLight);
        // Visible
        if (isShow) {
            controller.show(WindowInsetsCompat.Type.statusBars());
        } else {
            controller.hide(WindowInsetsCompat.Type.statusBars());
        }
    }
}
