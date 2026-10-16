package com.mct.app.helper.iap.billing;

import androidx.annotation.NonNull;

import com.mct.app.helper.iap.billing.models.ConsumeResult;

/**
 * Receives the result of consuming one purchase token, on the main thread.
 * <p>
 * Pass one to {@link BillingConnector#consume(String, ConsumeListener)} for a single call (called
 * exactly once per call), or register one with {@link BillingConnector#setConsumeListener} to also
 * receive the retries the library makes for {@link BillingConnector#consumeWithRetry}.
 */
public interface ConsumeListener {
    void onConsumeResult(@NonNull ConsumeResult result);
}
