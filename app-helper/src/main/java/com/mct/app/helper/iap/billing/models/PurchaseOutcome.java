package com.mct.app.helper.iap.billing.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mct.app.helper.iap.billing.enums.AcknowledgeOutcome;

/**
 * What a refresh did with one purchase.
 */
public final class PurchaseOutcome {

    private final OwnedPurchase purchase;
    private final AcknowledgeOutcome acknowledgeOutcome;
    private final int responseCode;
    @Nullable
    private final String debugMessage;

    public PurchaseOutcome(@NonNull OwnedPurchase purchase, @NonNull AcknowledgeOutcome acknowledgeOutcome,
                           int responseCode, @Nullable String debugMessage) {
        this.purchase = purchase;
        this.acknowledgeOutcome = acknowledgeOutcome;
        this.responseCode = responseCode;
        this.debugMessage = debugMessage;
    }

    @NonNull
    public OwnedPurchase getPurchase() {
        return purchase;
    }

    @NonNull
    public AcknowledgeOutcome getAcknowledgeOutcome() {
        return acknowledgeOutcome;
    }

    /** Response code of the acknowledge call; OK (0) when no call was made. */
    public int getResponseCode() {
        return responseCode;
    }

    @Nullable
    public String getDebugMessage() {
        return debugMessage;
    }

    @NonNull
    @Override
    public String toString() {
        return "PurchaseOutcome{" + purchase.getProducts() + " " + acknowledgeOutcome + " code=" + responseCode + "}";
    }
}
