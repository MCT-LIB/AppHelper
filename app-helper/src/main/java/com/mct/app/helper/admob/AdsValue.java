package com.mct.app.helper.admob;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdValue;

public class AdsValue {

    private final int precisionType;
    private final String currencyCode;
    private final long valueMicros;

    public int getPrecisionType() {
        return this.precisionType;
    }

    public long getValueMicros() {
        return this.valueMicros;
    }

    @NonNull
    public String getCurrencyCode() {
        return this.currencyCode;
    }

    private AdsValue(int precisionType, String currencyCode, long valueMicros) {
        this.precisionType = precisionType;
        this.currencyCode = currencyCode;
        this.valueMicros = valueMicros;
    }

    @NonNull
    public static AdsValue of(@NonNull AdValue adValue) {
        return new AdsValue(
                adValue.getPrecisionType(),
                adValue.getCurrencyCode(),
                adValue.getValueMicros()
        );
    }

}
