package com.mct.app.helper.iap.billing.enums;

import com.android.billingclient.api.BillingFlowParams;

/**
 * How Play replaces an existing subscription when the user switches plan
 * (mirrors {@code BillingFlowParams.SubscriptionUpdateParams.ReplacementMode}).
 */
public enum ReplacementMode {
    /** New plan starts now; remaining time of the old plan is credited (prorated). Upgrade default. */
    WITH_TIME_PRORATION(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION),
    /** New plan starts now, billing cycle kept; the user pays the prorated price difference now. */
    CHARGE_PRORATED_PRICE(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_PRORATED_PRICE),
    /** New plan starts now, charged at the next renewal date. */
    WITHOUT_PRORATION(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITHOUT_PRORATION),
    /** New plan starts now and is charged in full now; remaining time is credited. */
    CHARGE_FULL_PRICE(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE),
    /** New plan starts when the old one renews. Usual choice for a downgrade. */
    DEFERRED(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.DEFERRED);

    private final int playValue;

    ReplacementMode(int playValue) {
        this.playValue = playValue;
    }

    /** The {@code SubscriptionUpdateParams.ReplacementMode} constant. */
    public int getPlayValue() {
        return playValue;
    }
}
