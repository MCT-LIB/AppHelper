package com.mct.app.helper.iap.billing.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mct.app.helper.iap.billing.enums.RefreshReason;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of {@code BillingConnector.refreshPurchases}: the query result plus what was done with each
 * purchase (acknowledged, pending, ...).
 */
public final class RefreshResult {

    private final PurchasesResult purchases;
    private final List<PurchaseOutcome> outcomes;
    private final RefreshReason reason;
    @Nullable
    private final String triggerProductId;

    public RefreshResult(@NonNull PurchasesResult purchases, @NonNull List<PurchaseOutcome> outcomes,
                         @NonNull RefreshReason reason, @Nullable String triggerProductId) {
        this.purchases = purchases;
        this.outcomes = Collections.unmodifiableList(new ArrayList<>(outcomes));
        this.reason = reason;
        this.triggerProductId = triggerProductId;
    }

    @NonNull
    public PurchasesResult getPurchases() {
        return purchases;
    }

    /** Shortcut for {@code getPurchases().isSuccess()}. */
    public boolean isSuccess() {
        return purchases.isSuccess();
    }

    @NonNull
    public List<PurchaseOutcome> getOutcomes() {
        return outcomes;
    }

    @Nullable
    public PurchaseOutcome getOutcome(@NonNull String purchaseToken) {
        for (PurchaseOutcome o : outcomes) {
            if (o.getPurchase().getPurchaseToken().equals(purchaseToken)) return o;
        }
        return null;
    }

    @NonNull
    public RefreshReason getReason() {
        return reason;
    }

    /** For {@link RefreshReason#ITEM_ALREADY_OWNED}: the product the user tried to buy, if known. */
    @Nullable
    public String getTriggerProductId() {
        return triggerProductId;
    }

    @NonNull
    @Override
    public String toString() {
        return "RefreshResult{" + reason + " " + purchases + " outcomes=" + outcomes + "}";
    }
}
