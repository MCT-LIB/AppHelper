package com.mct.app.helper.iap.billing.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mct.app.helper.iap.billing.enums.ReplacementMode;

/**
 * Optional parameters for {@code BillingConnector.purchase/subscribe(activity, productId, options)}.
 * <pre>
 * PurchaseOptions.builder()
 *     .basePlan("annual", null)
 *     .replaceSubscription(current.getPurchaseToken(), ReplacementMode.WITH_TIME_PRORATION)
 *     .obfuscatedAccountId(hashedUserId)
 *     .build();
 * </pre>
 */
public final class PurchaseOptions {

    @Nullable
    private final String planId;
    @Nullable
    private final String offerId;
    @Nullable
    private final String oldPurchaseToken;
    @NonNull
    private final ReplacementMode replacementMode;
    @Nullable
    private final String obfuscatedAccountId;
    @Nullable
    private final String obfuscatedProfileId;

    private PurchaseOptions(Builder b) {
        this.planId = b.planId;
        this.offerId = b.offerId;
        this.oldPurchaseToken = b.oldPurchaseToken;
        this.replacementMode = b.replacementMode;
        this.obfuscatedAccountId = b.obfuscatedAccountId;
        this.obfuscatedProfileId = b.obfuscatedProfileId;
    }

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    @Nullable
    public String getPlanId() {
        return planId;
    }

    @Nullable
    public String getOfferId() {
        return offerId;
    }

    @Nullable
    public String getOldPurchaseToken() {
        return oldPurchaseToken;
    }

    @NonNull
    public ReplacementMode getReplacementMode() {
        return replacementMode;
    }

    @Nullable
    public String getObfuscatedAccountId() {
        return obfuscatedAccountId;
    }

    @Nullable
    public String getObfuscatedProfileId() {
        return obfuscatedProfileId;
    }

    public static final class Builder {
        private String planId;
        private String offerId;
        private String oldPurchaseToken;
        private ReplacementMode replacementMode = ReplacementMode.WITH_TIME_PRORATION;
        private String obfuscatedAccountId;
        private String obfuscatedProfileId;

        private Builder() {
        }

        /** Subscription base plan / offer. A null plan picks the first offer (old behaviour). */
        @NonNull
        public Builder basePlan(@Nullable String planId, @Nullable String offerId) {
            this.planId = planId;
            this.offerId = offerId;
            return this;
        }

        /**
         * Replace the subscription identified by {@code oldPurchaseToken} (upgrade / downgrade)
         * instead of selling a second concurrent subscription. Ignored for in-app products and when
         * the token is null or empty.
         */
        @NonNull
        public Builder replaceSubscription(@Nullable String oldPurchaseToken, @NonNull ReplacementMode mode) {
            this.oldPurchaseToken = oldPurchaseToken;
            this.replacementMode = mode;
            return this;
        }

        /** Hashed, non-PII user id (max 64 chars). Helps Play fraud detection. */
        @NonNull
        public Builder obfuscatedAccountId(@Nullable String id) {
            this.obfuscatedAccountId = id;
            return this;
        }

        @NonNull
        public Builder obfuscatedProfileId(@Nullable String id) {
            this.obfuscatedProfileId = id;
            return this;
        }

        @NonNull
        public PurchaseOptions build() {
            return new PurchaseOptions(this);
        }
    }
}
