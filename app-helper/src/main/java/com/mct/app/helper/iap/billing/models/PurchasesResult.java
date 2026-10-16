package com.mct.app.helper.iap.billing.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of querying owned purchases (subscriptions and in-app) from Play.
 * <p>
 * The key rule: an empty purchase list means "owns nothing" only when the matching query succeeded.
 * Check {@link #isSubscriptionsQueryOk()} (or use {@link #isSubscribed()}) before revoking a
 * subscription entitlement, and {@link #isSuccess()} before treating the whole list as complete.
 */
public final class PurchasesResult {

    private final int subsResponseCode;
    @Nullable
    private final String subsDebugMessage;
    private final int inAppResponseCode;
    @Nullable
    private final String inAppDebugMessage;
    private final List<OwnedPurchase> purchases;
    private final List<OwnedPurchase> rejected;
    private final boolean signatureVerified;

    /**
     * Public so apps can build fakes in their own tests.
     *
     * @param subsResponseCode  {@code BillingResponseCode} of the subscriptions query
     * @param inAppResponseCode {@code BillingResponseCode} of the in-app query
     * @param purchases         purchases that passed signature verification (all of them when
     *                          verification is off)
     * @param rejected          purchases whose signature did not match the licence key
     * @param signatureVerified whether signatures were actually checked (licence key present)
     */
    public PurchasesResult(int subsResponseCode, @Nullable String subsDebugMessage,
                           int inAppResponseCode, @Nullable String inAppDebugMessage,
                           @NonNull List<OwnedPurchase> purchases,
                           @NonNull List<OwnedPurchase> rejected,
                           boolean signatureVerified) {
        this.subsResponseCode = subsResponseCode;
        this.subsDebugMessage = subsDebugMessage;
        this.inAppResponseCode = inAppResponseCode;
        this.inAppDebugMessage = inAppDebugMessage;
        this.purchases = Collections.unmodifiableList(new ArrayList<>(purchases));
        this.rejected = Collections.unmodifiableList(new ArrayList<>(rejected));
        this.signatureVerified = signatureVerified;
    }

    /** Both queries returned OK: the purchase list is complete. */
    public boolean isSuccess() {
        return isSubscriptionsQueryOk() && isInAppQueryOk();
    }

    /** The subscriptions query returned OK, so {@link #getSubscriptions()} is authoritative. */
    public boolean isSubscriptionsQueryOk() {
        return subsResponseCode == BillingClient.BillingResponseCode.OK;
    }

    /** The in-app query returned OK, so {@link #getInAppPurchases()} is authoritative. */
    public boolean isInAppQueryOk() {
        return inAppResponseCode == BillingClient.BillingResponseCode.OK;
    }

    /** OK if both succeeded, otherwise the first failing code (subscriptions first). */
    public int getResponseCode() {
        return !isSubscriptionsQueryOk() ? subsResponseCode : inAppResponseCode;
    }

    @Nullable
    public String getDebugMessage() {
        return !isSubscriptionsQueryOk() ? subsDebugMessage : inAppDebugMessage;
    }

    public int getSubscriptionsResponseCode() {
        return subsResponseCode;
    }

    public int getInAppResponseCode() {
        return inAppResponseCode;
    }

    /** All accepted purchases (subscriptions first, then in-app), in any state. */
    @NonNull
    public List<OwnedPurchase> getPurchases() {
        return purchases;
    }

    @NonNull
    public List<OwnedPurchase> getSubscriptions() {
        List<OwnedPurchase> out = new ArrayList<>();
        for (OwnedPurchase p : purchases) {
            if (p.isSubscription()) out.add(p);
        }
        return out;
    }

    @NonNull
    public List<OwnedPurchase> getInAppPurchases() {
        List<OwnedPurchase> out = new ArrayList<>();
        for (OwnedPurchase p : purchases) {
            if (!p.isSubscription()) out.add(p);
        }
        return out;
    }

    /** Subscriptions that are PURCHASED and not suspended. */
    @NonNull
    public List<OwnedPurchase> getActiveSubscriptions() {
        List<OwnedPurchase> out = new ArrayList<>();
        for (OwnedPurchase p : purchases) {
            if (p.isSubscription() && p.isEntitled()) out.add(p);
        }
        return out;
    }

    public boolean hasActiveSubscription() {
        return !getActiveSubscriptions().isEmpty();
    }

    /**
     * {@code TRUE}/{@code FALSE} when the subscriptions query succeeded; {@code null} when Play
     * failed and the answer is unknown (keep the cached entitlement, do not revoke).
     */
    @Nullable
    public Boolean isSubscribed() {
        return isSubscriptionsQueryOk() ? hasActiveSubscription() : null;
    }

    /** First accepted purchase containing {@code productId}, or null. */
    @Nullable
    public OwnedPurchase find(@NonNull String productId) {
        for (OwnedPurchase p : purchases) {
            if (p.hasProduct(productId)) return p;
        }
        return null;
    }

    /** Purchases dropped because their signature did not match the licence key. */
    @NonNull
    public List<OwnedPurchase> getRejected() {
        return rejected;
    }

    /**
     * Whether signatures were really checked. False means the connector has no licence key and every
     * purchase was accepted unverified (fine for debug, not for release).
     */
    public boolean isSignatureVerified() {
        return signatureVerified;
    }

    @NonNull
    @Override
    public String toString() {
        return "PurchasesResult{subs=" + subsResponseCode + ", inapp=" + inAppResponseCode
                + ", purchases=" + purchases + ", rejected=" + rejected.size()
                + ", verified=" + signatureVerified + "}";
    }
}
