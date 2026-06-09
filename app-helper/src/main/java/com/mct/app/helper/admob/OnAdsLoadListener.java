package com.mct.app.helper.admob;

import com.mct.app.helper.admob.ads.BaseAds;
import com.mct.app.helper.admob.listener.LoadError;

/**
 * Global load observer wired at config time. Both methods are optional
 * (default no-op) so implementors override only what they need.
 */
public interface OnAdsLoadListener {

    default void onAdsLoaded(BaseAds<?> ads) {
    }

    default void onAdsFailedToLoad(BaseAds<?> ads, LoadError error) {
    }
}
