package com.mct.app.helper.admob.listener;

import com.mct.app.helper.admob.ads.BaseAds;

/**
 * Result of a {@code load(...)} call. All methods are optional (default no-op) so
 * callers override only what they need.
 */
public interface LoadCallback {

    /** The ad finished loading and is ready to show. */
    default void onAdLoaded(BaseAds<?> ad) {
    }

    /** The ad failed to load. {@code error} carries AdMob's code / message / domain. */
    default void onAdFailedToLoad(BaseAds<?> ad, LoadError error) {
    }
}
