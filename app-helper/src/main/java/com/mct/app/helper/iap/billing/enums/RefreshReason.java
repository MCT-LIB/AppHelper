package com.mct.app.helper.iap.billing.enums;

/**
 * Why a purchases refresh ran.
 */
public enum RefreshReason {
    /** {@code BillingConnector.refreshPurchases} was called (app start, onResume, Restore button). */
    MANUAL,
    /** The billing service (re)connected; only when a refresh listener is registered. */
    CONNECTED,
    /** Play answered ITEM_ALREADY_OWNED to a purchase; the library re-queried to restore it. */
    ITEM_ALREADY_OWNED
}
