package com.mct.app.helper.admob;

import com.google.android.gms.ads.OnPaidEventListener;

public interface OnPaidEventListeners {

    void onPaidEvent(AdsValue value);

    default OnPaidEventListener toGms() {
        return adValue -> onPaidEvent(AdsValue.of(adValue));
    }
}
