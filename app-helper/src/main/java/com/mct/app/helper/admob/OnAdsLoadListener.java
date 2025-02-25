package com.mct.app.helper.admob;

import com.mct.app.helper.admob.ads.BaseAds;

public interface OnAdsLoadListener {

    void onAdsFailedToLoad(BaseAds<?> ads, int code, String message, String domain);

    void onAdsLoaded(BaseAds<?> ads);
}
