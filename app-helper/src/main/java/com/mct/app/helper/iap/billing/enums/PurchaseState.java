package com.mct.app.helper.iap.billing.enums;

import com.android.billingclient.api.Purchase;

/**
 * Play purchase state. Only {@link #PURCHASED} may grant an entitlement; {@link #PENDING} means the
 * payment has not completed yet (cash / slow payment methods) and must not unlock anything.
 */
public enum PurchaseState {
    PURCHASED,
    PENDING,
    UNSPECIFIED;

    public static PurchaseState fromPlay(int purchaseState) {
        switch (purchaseState) {
            case Purchase.PurchaseState.PURCHASED:
                return PURCHASED;
            case Purchase.PurchaseState.PENDING:
                return PENDING;
            default:
                return UNSPECIFIED;
        }
    }
}
