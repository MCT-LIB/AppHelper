package com.mct.app.helper.iap.banner;

import android.app.Dialog;
import android.os.Build;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;

import com.mct.app.helper.R;
import com.mct.app.helper.iap.banner.component.system.NavigationBarComponent;
import com.mct.app.helper.iap.banner.component.system.StatusBarComponent;
import com.mct.app.helper.iap.banner.component.system.SystemBarComponent;

/**
 * IapBannerDialog - A custom dialog used for displaying the IapBanner.
 * <p>
 * This class extends the Android `Dialog` class to provide a custom dialog for showing the
 * IapBanner with optional full-screen support. It also handles system bar components such as
 * status bar and navigation bar customization based on the banner's configuration.
 */
class IapBannerDialog extends Dialog {

    // Constants for dialog styles
    private static final int STYLE_NORMAL = R.style.Iap_AlertDialog;
    private static final int STYLE_FULL_SCREEN = R.style.Iap_AlertDialog_FullScreen;

    /**
     * Constructor for IapBannerDialog.
     *
     * @param banner     The IapBanner instance to be displayed in the dialog.
     * @param fullScreen Set to true to display the dialog in full-screen mode.
     */
    IapBannerDialog(@NonNull IapBanner banner, boolean fullScreen) {
        super(banner.getContext(), getStyle(fullScreen));
        initWindow(getWindow(), fullScreen);
        initSystemBar(getWindow(), banner);
        setContentView(banner.getRoot());
    }

    /**
     * Get the appropriate dialog style based on whether it should be full-screen or not.
     *
     * @param fullScreen Set to true for full-screen style, false for normal style.
     * @return The dialog style resource ID.
     */
    private static int getStyle(boolean fullScreen) {
        return fullScreen ? STYLE_FULL_SCREEN : STYLE_NORMAL;
    }

    /**
     * Initialize the window settings for the dialog, including system bar handling.
     *
     * @param window     The dialog's Window object.
     * @param fullScreen Set to true for full-screen mode.
     */
    private static void initWindow(Window window, boolean fullScreen) {
        if (window == null) {
            return;
        }
        // set fit decor
        WindowCompat.setDecorFitsSystemWindows(window, !fullScreen);
        ViewGroup decor = (ViewGroup) window.getDecorView();
        if (decor.getChildCount() > 0) {
            decor.getChildAt(0).setFitsSystemWindows(!fullScreen);
        }
        if (fullScreen) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Adjust window attributes for devices with display cutouts.
                WindowManager.LayoutParams layoutParams = window.getAttributes();
                layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                window.setAttributes(layoutParams);
            }
        }
    }

    /**
     * Initialize the system bar components such as status bar and navigation bar
     * based on the banner's configuration.
     *
     * @param window The dialog's Window object.
     * @param banner The associated IapBanner instance.
     */
    private static void initSystemBar(Window window, IapBanner banner) {
        if (window == null) {
            return;
        }
        SystemBarComponent<?> statusBarComponent = banner.findComponentById(StatusBarComponent.ID);
        SystemBarComponent<?> navigationBarComponent = banner.findComponentById(NavigationBarComponent.ID);

        if (statusBarComponent != null) {
            statusBarComponent.applyToWindow(window);
        }
        if (navigationBarComponent != null) {
            navigationBarComponent.applyToWindow(window);
        }
    }
}
