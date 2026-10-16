package com.mct.app.helper.iap.billing.enums;

/**
 * Subscription standing from one subscriptions query, see
 * {@code PurchasesResult.getSubscriptionStatus()}. Checked in this order.
 */
public enum SubscriptionStatus {
    /** An accepted subscription is PURCHASED and not on hold: grant access. */
    ACTIVE,
    /**
     * No active subscription, but one is on hold (account hold: the renewal payment failed). Do not
     * grant access and do not sell another plan; ask the user to fix the payment method in Play.
     */
    SUSPENDED,
    /** No active or held subscription, but one is waiting for payment. Do not grant or sell. */
    PENDING,
    /** The query succeeded and the user owns no subscription. Safe to revoke and to sell. */
    NONE,
    /**
     * Play failed, or signature verification rejected a subscription. Keep the cached entitlement,
     * do not revoke and do not sell.
     */
    UNKNOWN
}
