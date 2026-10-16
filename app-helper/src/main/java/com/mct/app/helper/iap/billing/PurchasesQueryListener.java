package com.mct.app.helper.iap.billing;

import androidx.annotation.NonNull;

import com.mct.app.helper.iap.billing.models.PurchasesResult;

/**
 * Receives the result of {@link BillingConnector#queryPurchases(PurchasesQueryListener)} on the main thread.
 */
public interface PurchasesQueryListener {
    void onPurchasesQueried(@NonNull PurchasesResult result);
}
