package com.mct.app.helper.iap.banner.component.system;

import android.graphics.Color;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.mct.app.helper.R;

/**
 * NavigationBarComponent - A component for customizing the navigation bar of an IapBanner.
 * <p>
 * The NavigationBarComponent class allows you to create a component within an IapBanner that can
 * customize the appearance and behavior of the navigation bar.
 * You can set the background color, appearance (light or dark), and visibility of the navigation bar
 * using this component.
 * <p>
 * Usage example:
 * <code>
 * <pre>
 * // Create a NavigationBarComponent and apply it to a Window
 * NavigationBarComponent navigationBarComponent = new NavigationBarComponent()
 *         // Set the navigation bar background color
 *         .background(Color.BLACK)
 *         // Set the navigation bar appearance to light
 *         .lightAppearance()
 *         // Show the navigation bar
 *         .show();
 * // Apply the component to a Window
 * navigationBarComponent.applyToWindow(window);
 * </pre>
 * </code>
 */
public class NavigationBarComponent extends SystemBarComponent<NavigationBarComponent> {

    /**
     * The ID for the navigation bar component.
     */
    public static final int ID = R.id.iap_component_sys_bar_nav;

    public NavigationBarComponent() {
    }

    /**
     * Apply the component's settings to a Window.
     *
     * @param window The Window object to which the component's settings should be applied.
     */
    @Override
    public void applyToWindow(@NonNull Window window) {
        // Background
        window.setNavigationBarColor(background == Color.TRANSPARENT ? TRANSPARENT : background);
        // Create controller
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        // Appearance
        controller.setAppearanceLightNavigationBars(isLight);
        // Visible
        if (isShow) {
            controller.show(WindowInsetsCompat.Type.navigationBars());
        } else {
            controller.hide(WindowInsetsCompat.Type.navigationBars());
        }
    }
}
