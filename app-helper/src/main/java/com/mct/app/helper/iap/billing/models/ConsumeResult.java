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
    private final boolean pending;
    private final boolean rejected;

    public ConsumeResult(@NonNull String purchaseToken, int responseCode, @Nullable String debugMessage,
                         boolean retryScheduled) {
        this(purchaseToken, responseCode, debugMessage, retryScheduled, false, false);
    }

    /**
     * @param pending  the purchase is PENDING, so nothing was sent to Play
     * @param rejected signature verification rejected the purchase, so nothing was sent to Play
     */
    public ConsumeResult(@NonNull String purchaseToken, int responseCode, @Nullable String debugMessage,
                         boolean retryScheduled, boolean pending, boolean rejected) {
        this.purchaseToken = purchaseToken;
        this.responseCode = responseCode;
        this.debugMessage = debugMessage;
        this.retryScheduled = retryScheduled;
        this.pending = pending;
        this.rejected = rejected;
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

    /**
     * The token is still kept: the library tries the consume again after the next refresh that
     * shows it PURCHASED and verified.
     */
    public boolean isRetryScheduled() {
        return retryScheduled;
    }

    /**
     * The purchase is PENDING, so it was not consumed and is not kept. Grant it once a refresh
     * reports it PURCHASED, then pass it to {@code consumeWithRetry} again.
     */
    public boolean isPending() {
        return pending;
    }

    /**
     * A refresh found this kept token among the purchases rejected by signature verification
     * ({@link PurchasesResult#getRejected()}). It was not consumed and stays kept (and is not
     * consumed while it is rejected). It is not "not owned": the app decides what to do (usually,
     * fix the licence key).
     */
    public boolean isRejected() {
        return rejected;
    }

    @NonNull
    @Override
    public String toString() {
        return "ConsumeResult{token=" + purchaseToken + ", code=" + responseCode
                + (debugMessage != null ? ", message=" + debugMessage : "")
                + (retryScheduled ? ", retryScheduled" : "")
                + (pending ? ", pending" : "")
                + (rejected ? ", rejected" : "") + '}';
    }
}
