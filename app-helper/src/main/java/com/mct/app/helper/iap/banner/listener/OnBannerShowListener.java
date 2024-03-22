package com.mct.app.helper.iap.banner.listener;

import android.content.DialogInterface;

import com.mct.app.helper.iap.banner.IapBanner;

/**
 * OnBannerShowListener - Interface for handling banner show events.
 * <p>
 * This interface defines a method that should be implemented to handle the display
 * of an {@link IapBanner}. It provides a callback mechanism to perform actions when
 * the banner is shown to the user.
 */
public interface OnBannerShowListener {

    /**
     * Called when the IapBanner is displayed.
     *
     * @param banner The IapBanner instance that is being shown.
     * @param dialog The DialogInterface representing the banner dialog.
     */
    void onShow(IapBanner banner, DialogInterface dialog);
}
