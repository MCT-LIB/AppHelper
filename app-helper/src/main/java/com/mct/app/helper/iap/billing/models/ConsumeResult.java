package com.mct.app.helper.iap.billing.models;

import static com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_NOT_OWNED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * What happened when one purchase token was consumed.
 */
public final class ConsumeResult {

    private final String purchaseToken;
    private final int responseCode;
    @Nullable
    private final String debugMessage;
    private final boolean retryScheduled;

    public ConsumeResult(@NonNull String purchaseToken, int responseCode, @Nullable String debugMessage,
                         boolean retryScheduled) {
        this.purchaseToken = purchaseToken;
        this.responseCode = responseCode;
        this.debugMessage = debugMessage;
        this.retryScheduled = retryScheduled;
    }

    @NonNull
    public String getPurchaseToken() {
        return purchaseToken;
    }

    /** Play response code (OK also when the token was already consumed earlier in this session). */
    public int getResponseCode() {
        return responseCode;
    }

    @Nullable
    public String getDebugMessage() {
        return debugMessage;
    }

    /** The token is consumed: the product can be bought again. */
    public boolean isSuccess() {
        return responseCode == OK;
    }

    /**
     * Play no longer owns this token (already consumed elsewhere, refunded or never owned).
     * Nothing is left to consume, and the product can be bought again.
     */
    public boolean isNotOwned() {
        return responseCode == ITEM_NOT_OWNED;
    }

    /** The call failed and the library will try again after the next refresh. */
    public boolean isRetryScheduled() {
        return retryScheduled;
    }

    @NonNull
    @Override
    public String toString() {
        return "ConsumeResult{token=" + purchaseToken + ", code=" + responseCode
                + (debugMessage != null ? ", message=" + debugMessage : "")
                + (retryScheduled ? ", retryScheduled" : "") + '}';
    }
}
