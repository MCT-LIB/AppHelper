package com.mct.app.helper.admob;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdValue;

public class AdsValue {

    private final String alias;
    private final int precisionType;
    private final String currencyCode;
    private final long valueMicros;

    public String getAlias() {
        return alias;
    }

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

    private AdsValue(String alias, int precisionType, String currencyCode, long valueMicros) {
        this.alias = alias;
        this.precisionType = precisionType;
        this.currencyCode = currencyCode;
        this.valueMicros = valueMicros;
    }

    @NonNull
    public static AdsValue of(String alias, @NonNull AdValue adValue) {
        return new AdsValue(
                alias,
                adValue.getPrecisionType(),
                adValue.getCurrencyCode(),
                adValue.getValueMicros()
        );
    }

}
