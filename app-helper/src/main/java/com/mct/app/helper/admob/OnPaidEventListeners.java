package com.mct.app.helper.admob;

import androidx.core.util.Supplier;

import com.google.android.gms.ads.OnPaidEventListener;

import java.util.Optional;

public interface OnPaidEventListeners {

    void onPaidEvent(AdsValue value);

    default OnPaidEventListener toGms(Supplier<String> alias) {
        return adValue -> {
            String aliasString = Optional.ofNullable(alias).map(Supplier::get).orElse(null);
            onPaidEvent(AdsValue.of(aliasString, adValue));
        };
    }
}
