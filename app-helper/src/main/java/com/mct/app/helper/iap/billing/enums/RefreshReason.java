package com.mct.app.helper.iap.billing.enums;

/**
 * Why a purchases refresh ran.
 */
public enum RefreshReason {
    /** {@code BillingConnector.refreshPurchases} was called (app start, onResume, Restore button). */
    MANUAL,
    /** The billing service (re)connected; only when a refresh listener is registered. */
    CONNECTED,
    /**
     * Play delivered an OK purchase update (new purchase, or a PENDING one completing); only when a
     * refresh listener is registered. Also reported when a refresh re-queried because a purchase
     * arrived while it was running.
     */
    PURCHASE_UPDATED,
    /** Play answered ITEM_ALREADY_OWNED to a purchase; the library re-queried to restore it. */
    ITEM_ALREADY_OWNED
}
