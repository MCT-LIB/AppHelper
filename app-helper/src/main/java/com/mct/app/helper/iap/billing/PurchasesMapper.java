package com.mct.app.helper.iap.billing;

import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;
import static com.android.billingclient.api.BillingClient.ProductType.INAPP;
import static com.android.billingclient.api.BillingClient.ProductType.SUBS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.Purchase;
import com.mct.app.helper.iap.billing.enums.AcknowledgeOutcome;
import com.mct.app.helper.iap.billing.models.OwnedPurchase;
import com.mct.app.helper.iap.billing.models.PurchasesResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure mapping from raw Play query results to {@link PurchasesResult}. No BillingClient, so it is
 * unit-testable with fakes.
 */
final class PurchasesMapper {

    interface SignatureVerifier {
        boolean isValid(@NonNull Purchase purchase);
    }

    /** One raw {@code queryPurchasesAsync} answer. */
    static final class TypedQuery {
        final int responseCode;
        @Nullable
        final String debugMessage;
        @NonNull
        final List<Purchase> purchases;

        TypedQuery(int responseCode, @Nullable String debugMessage, @Nullable List<Purchase> purchases) {
            this.responseCode = responseCode;
            this.debugMessage = debugMessage;
            this.purchases = purchases == null ? Collections.emptyList() : purchases;
        }
    }

    /**
     * @param verifier null when no licence key is configured: every purchase is accepted and the
     *                 result reports {@code isSignatureVerified() == false}
     */
    @NonNull
    static PurchasesResult map(@NonNull TypedQuery subs, @NonNull TypedQuery inApp, @Nullable SignatureVerifier verifier) {
        List<OwnedPurchase> accepted = new ArrayList<>();
        List<OwnedPurchase> rejected = new ArrayList<>();
        collect(subs, SUBS, verifier, accepted, rejected);
        collect(inApp, INAPP, verifier, accepted, rejected);
        return new PurchasesResult(subs.responseCode, subs.debugMessage,
                inApp.responseCode, inApp.debugMessage, accepted, rejected, verifier != null);
    }

    private static void collect(TypedQuery q, String type, @Nullable SignatureVerifier verifier,
                                List<OwnedPurchase> accepted, List<OwnedPurchase> rejected) {
        if (q.responseCode != OK) {
            // a failed query never contributes purchases: "unknown", not "none"
            return;
        }
        for (Purchase p : q.purchases) {
            if (verifier == null) {
                accepted.add(OwnedPurchase.from(p, type, false));
            } else if (verifier.isValid(p)) {
                accepted.add(OwnedPurchase.from(p, type, true));
            } else {
                rejected.add(OwnedPurchase.from(p, type, false));
            }
        }
    }

    /**
     * What a refresh should do about acknowledging {@code p}, or {@code null} if it must call
     * acknowledgePurchase.
     *
     * @param acknowledgeable true for subscriptions and for in-app ids registered as non-consumable
     */
    @Nullable
    static AcknowledgeOutcome acknowledgeDecision(@NonNull OwnedPurchase p, boolean acknowledgeable) {
        if (p.isPending()) {
            return AcknowledgeOutcome.PENDING;
        }
        if (!acknowledgeable || !p.isPurchased()) {
            return AcknowledgeOutcome.NOT_REQUIRED;
        }
        if (p.isAcknowledged()) {
            return AcknowledgeOutcome.ALREADY_ACKNOWLEDGED;
        }
        return null;
    }

    private PurchasesMapper() {
    }
}
