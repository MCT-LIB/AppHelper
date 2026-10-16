package com.mct.app.helper.iap.billing;

import androidx.annotation.NonNull;

import com.mct.app.helper.iap.billing.models.RefreshResult;

/**
 * Receives the result of a purchases refresh on the main thread.
 * <p>
 * Pass one to {@link BillingConnector#refreshPurchases(PurchasesRefreshListener)} for a single call,
 * or register one with {@link BillingConnector#setPurchasesRefreshListener(PurchasesRefreshListener)}
 * to also receive refreshes the library starts itself (ITEM_ALREADY_OWNED).
 */
public interface PurchasesRefreshListener {
    void onPurchasesRefreshed(@NonNull RefreshResult result);
}
