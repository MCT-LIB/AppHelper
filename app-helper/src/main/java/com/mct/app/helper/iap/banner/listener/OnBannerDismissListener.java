package com.mct.app.helper.iap.banner.listener;

import android.content.DialogInterface;

import com.mct.app.helper.iap.banner.IapBanner;

/**
 * OnBannerDismissListener - Interface for handling banner dismissal events.
 * <p>
 * This interface defines a method that should be implemented to handle the dismissal
 * of an {@link IapBanner}. It provides a callback mechanism to perform actions when
 * the banner is dismissed by the user or programmatically.
 */
public interface OnBannerDismissListener {

    /**
     * Called when the IapBanner is dismissed.
     *
     * @param banner The IapBanner instance that was dismissed.
     * @param dialog The DialogInterface representing the banner dialog.
     */
    void onDismiss(IapBanner banner, DialogInterface dialog);
}
