package com.mct.app.helper.iap.banner.component.billing;

import androidx.annotation.NonNull;

import com.mct.app.helper.iap.banner.IapBanner;
import com.mct.app.helper.iap.billing.BillingConnector;
import com.mct.app.helper.iap.billing.models.BillingResponse;
import com.mct.app.helper.iap.billing.models.ProductInfo;
import com.mct.app.helper.iap.billing.models.PurchaseInfo;

import java.util.List;

public abstract class BillingEventListeners {

    public void onProductsFetched(@NonNull IapBanner banner, @NonNull List<ProductInfo> productDetails) {
    }

    public void onPurchasedProductsFetched(@NonNull IapBanner banner, @NonNull List<PurchaseInfo> purchases) {
    }

    public void onProductsPurchased(@NonNull IapBanner banner, @NonNull List<PurchaseInfo> purchases) {
    }

    public void onPurchaseAcknowledged(@NonNull IapBanner banner, @NonNull PurchaseInfo purchase) {
    }

    public void onPurchaseConsumed(@NonNull IapBanner banner, @NonNull PurchaseInfo purchase) {
    }

    public void onBillingError(@NonNull IapBanner banner, @NonNull BillingConnector billingConnector, @NonNull BillingResponse response) {
    }
}
