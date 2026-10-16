package com.mct.app.helper.iap.billing.enums;

/**
 * What a refresh did about acknowledging one purchase.
 */
public enum AcknowledgeOutcome {
    /** Consumable, unknown in-app id, or unspecified state: the app handles it (consume etc.). */
    NOT_REQUIRED,
    /** Payment not completed yet; it cannot be acknowledged and must not grant anything. */
    PENDING,
    /** Was already acknowledged before this refresh. */
    ALREADY_ACKNOWLEDGED,
    /** Acknowledged by this refresh. */
    ACKNOWLEDGED,
    /** Acknowledge call failed; see {@code PurchaseOutcome.getResponseCode()}. Retried on next refresh. */
    FAILED
}
