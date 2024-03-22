package com.mct.app.helper.iap.banner.component.billing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mct.app.helper.R;

public enum BillingPeriod {
    // @formatter:off
    DAY      ("P1D", 1  , R.string.iap_period_day),
    WEEK     ("P1W", 7  , R.string.iap_period_week),
    MONTH    ("P1M", 30 , R.string.iap_period_month),
    QUARTER  ("P3M", 90 , R.string.iap_period_quarter),
    HALF_YEAR("P6M", 180, R.string.iap_period_half_year),
    YEAR     ("P1Y", 365, R.string.iap_period_year),
    LIFE_TIME("",    -1 , R.string.iap_period_life_time);
    // @formatter:on

    private final String billingPeriod;
    private final int totalDay;
    private final int titleRes;

    BillingPeriod(String billingPeriod, int totalDay, int titleRes) {
        this.billingPeriod = billingPeriod;
        this.totalDay = totalDay;
        this.titleRes = titleRes;
    }

    public int getTotalDay() {
        return totalDay;
    }

    public int getTitleRes() {
        return titleRes;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }

    public boolean atLeast(@NonNull BillingPeriod period) {
        return ordinal() >= period.ordinal();
    }

    public static BillingPeriod findPeriod(@Nullable String billingPeriod) {
        BillingPeriod result = LIFE_TIME; // Default value if not found

        for (BillingPeriod period : BillingPeriod.values()) {
            if (period.billingPeriod.equalsIgnoreCase(billingPeriod)) {
                result = period;
                break;
            }
        }
        return result;
    }
}
