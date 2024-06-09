package com.mct.app.helper.admob;

import androidx.annotation.Nullable;

import com.google.android.gms.ads.OnPaidEventListener;

public interface OnPaidEventListeners {

    void onPaidEvent(AdsValue value);

    default OnPaidEventListener toGms(@Nullable String alias) {
        return adValue -> onPaidEvent(AdsValue.of(alias, adValue));
    }
}
