package com.mct.app.helper.iap.billing.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.mct.app.helper.iap.billing.enums.PurchaseState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One purchase as Play reports it, independent of whether product details were loaded.
 * Unlike {@link PurchaseInfo} it never drops a purchase because its product is unknown.
 */
public final class OwnedPurchase {

    private final String productType;
    private final List<String> products;
    private final String purchaseToken;
    @Nullable
    private final String orderId;
    private final PurchaseState state;
    private final boolean acknowledged;
    private final boolean autoRenewing;
    private final boolean suspended;
    private final int quantity;
    private final long purchaseTime;
    private final boolean signatureVerified;
    @Nullable
    private final Purchase purchase;

    /**
     * Public so apps can build fakes in their own tests.
     *
     * @param productType       {@code BillingClient.ProductType.SUBS} ("subs") or {@code INAPP} ("inapp")
     * @param signatureVerified true only if the signature was checked against a licence key and matched
     */
    public OwnedPurchase(@NonNull String productType,
                         @NonNull List<String> products,
                         @NonNull String purchaseToken,
                         @Nullable String orderId,
                         @NonNull PurchaseState state,
                         boolean acknowledged,
                         boolean autoRenewing,
                         boolean suspended,
                         int quantity,
                         long purchaseTime,
                         boolean signatureVerified,
                         @Nullable Purchase purchase) {
        this.productType = productType;
        this.products = Collections.unmodifiableList(new ArrayList<>(products));
        this.purchaseToken = purchaseToken;
        this.orderId = orderId;
        this.state = state;
        this.acknowledged = acknowledged;
        this.autoRenewing = autoRenewing;
        this.suspended = suspended;
        this.quantity = quantity;
        this.purchaseTime = purchaseTime;
        this.signatureVerified = signatureVerified;
        this.purchase = purchase;
    }

    @NonNull
    public static OwnedPurchase from(@NonNull Purchase p, @NonNull String productType, boolean signatureVerified) {
        String orderId = p.getOrderId();
        return new OwnedPurchase(productType, p.getProducts(), p.getPurchaseToken(),
                orderId == null || orderId.isEmpty() ? null : orderId,
                PurchaseState.fromPlay(p.getPurchaseState()), p.isAcknowledged(), p.isAutoRenewing(),
                p.isSuspended(), p.getQuantity(), p.getPurchaseTime(), signatureVerified, p);
    }

    /** "subs" or "inapp". */
    @NonNull
    public String getProductType() {
        return productType;
    }

    public boolean isSubscription() {
        return BillingClient.ProductType.SUBS.equals(productType);
    }

    @NonNull
    public List<String> getProducts() {
        return products;
    }

    public boolean hasProduct(@NonNull String productId) {
        return products.contains(productId);
    }

    @NonNull
    public String getPurchaseToken() {
        return purchaseToken;
    }

    /** Null while the purchase is PENDING (Play assigns the order id once paid). */
    @Nullable
    public String getOrderId() {
        return orderId;
    }

    @NonNull
    public PurchaseState getState() {
        return state;
    }

    public boolean isPurchased() {
        return state == PurchaseState.PURCHASED;
    }

    public boolean isPending() {
        return state == PurchaseState.PENDING;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public boolean isAutoRenewing() {
        return autoRenewing;
    }

    /** Subscription on hold / paused for a payment problem: owned but must not grant access. */
    public boolean isSuspended() {
        return suspended;
    }

    /** PURCHASED and not suspended: the only state that should grant an entitlement. */
    public boolean isEntitled() {
        return isPurchased() && !suspended;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getPurchaseTime() {
        return purchaseTime;
    }

    /**
     * True only if the signature was verified against the licence key. False when the connector has
     * no key (verification skipped), see {@link PurchasesResult#isSignatureVerified()}.
     */
    public boolean isSignatureVerified() {
        return signatureVerified;
    }

    /** Raw Play object; null for fakes built in tests. */
    @Nullable
    public Purchase getPurchase() {
        return purchase;
    }

    @NonNull
    @Override
    public String toString() {
        return "OwnedPurchase{" + productType + " " + products + " state=" + state
                + " ack=" + acknowledged + " suspended=" + suspended + " order=" + orderId + "}";
    }
}
