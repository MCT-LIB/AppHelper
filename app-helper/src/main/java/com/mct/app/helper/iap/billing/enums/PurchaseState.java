package com.mct.app.helper.iap.billing.enums;

import com.android.billingclient.api.Purchase;

/**
 * Play purchase state. Only {@link #PURCHASED} may grant an entitlement; {@link #PENDING} means the
 * payment has not completed yet (cash / slow payment methods) and must not unlock anything.
 * {@link #SUSPENDED} is a paid subscription that Play has put on hold (account hold) because its
 * renewal payment failed: still owned, but it must not unlock anything until the user fixes the
 * payment method in Play. It is never acknowledged or consumed.
 */
public enum PurchaseState {
    PURCHASED,
    PENDING,
    UNSPECIFIED,
    /** Subscription on hold for a payment problem (Play {@code Purchase.isSuspended()}). */
    SUSPENDED;

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

    /** Like {@link #fromPlay(int)}, but a suspended PURCHASED subscription maps to {@link #SUSPENDED}. */
    public static PurchaseState fromPlay(int purchaseState, boolean suspended) {
        PurchaseState state = fromPlay(purchaseState);
        return suspended && state == PURCHASED ? SUSPENDED : state;
    }
}
