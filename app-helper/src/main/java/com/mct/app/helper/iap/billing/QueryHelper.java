package com.mct.app.helper.iap.billing;

import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_DISCONNECTED;
import static com.android.billingclient.api.BillingClient.FeatureType.SUBSCRIPTIONS;
import static com.android.billingclient.api.BillingClient.ProductType.INAPP;
import static com.android.billingclient.api.BillingClient.ProductType.SUBS;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.mct.app.helper.iap.billing.enums.SupportState;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

class QueryHelper {

    @NonNull
    static Disposable queryProductDetails(BillingClient client,
                                          List<QueryProductDetailsParams.Product> products,
                                          Consumer<List<ProductDetails>> listener) {
        return queryProductDetailsSingle(client, products)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((productDetails, throwable) -> {
                    if (listener != null) {
                        if (productDetails != null) {
                            listener.accept(productDetails);
                        } else {
                            listener.accept(Collections.emptyList());
                        }
                    }
                });
    }

    @NonNull
    static Disposable queryPurchases(BillingClient client, Consumer<List<Purchase>> listener) {
        return queryPurchasesSingle(client)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((productDetails, throwable) -> {
                    if (listener != null) {
                        if (productDetails != null) {
                            listener.accept(productDetails);
                        } else {
                            listener.accept(Collections.emptyList());
                        }
                    }
                });
    }

    static SupportState isSubscriptionSupported(@NonNull BillingClient billingClient) {
        BillingResult response = billingClient.isFeatureSupported(SUBSCRIPTIONS);
        switch (response.getResponseCode()) {
            case OK:
                return SupportState.SUPPORTED;
            case SERVICE_DISCONNECTED:
                return SupportState.DISCONNECTED;
            default:
                return SupportState.NOT_SUPPORTED;
        }
    }

    private static Single<List<ProductDetails>> queryProductDetailsSingle(BillingClient client, List<QueryProductDetailsParams.Product> products) {
        return Single.create((SingleOnSubscribe<List<ProductDetails>>) emitter -> {
            List<QueryProductDetailsParams.Product> productInAppList = products.stream().filter(p -> p.zzb().equals(INAPP)).collect(Collectors.toList());
            List<QueryProductDetailsParams.Product> productSubsList = products.stream().filter(p -> p.zzb().equals(SUBS)).collect(Collectors.toList());
            if (emitter.isDisposed()) {
                return;
            }
            List<ProductDetails> result1 = queryProductDetailsSync(client, productInAppList);
            if (emitter.isDisposed()) {
                return;
            }
            List<ProductDetails> result2 = queryProductDetailsSync(client, productSubsList);
            if (emitter.isDisposed()) {
                return;
            }
            emitter.onSuccess(Stream.of(result1, result2).flatMap(List::stream).collect(Collectors.toList()));
        });
    }

    private static Single<List<Purchase>> queryPurchasesSingle(BillingClient client) {
        return Single.create((SingleOnSubscribe<List<Purchase>>) emitter -> {
            if (emitter.isDisposed()) {
                return;
            }
            List<Purchase> result1 = queryPurchasesSync(client, INAPP);
            if (emitter.isDisposed()) {
                return;
            }
            List<Purchase> result2 = queryPurchasesSync(client, SUBS);
            if (emitter.isDisposed()) {
                return;
            }
            emitter.onSuccess(Stream.of(result1, result2).flatMap(List::stream).collect(Collectors.toList()));
        });
    }

    @NonNull
    private static List<ProductDetails> queryProductDetailsSync(BillingClient client, List<QueryProductDetailsParams.Product> products) {
        return Single.create((SingleOnSubscribe<List<ProductDetails>>) emitter -> {
            if (emitter.isDisposed()) {
                return;
            }
            if (!client.isReady()) {
                emitter.onSuccess(Collections.emptyList());
                return;
            }
            if (products == null || products.isEmpty()) {
                emitter.onSuccess(Collections.emptyList());
                return;
            }
            QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder().setProductList(products).build();
            client.queryProductDetailsAsync(params, (billingResult, list) -> {
                if (emitter.isDisposed()) {
                    return;
                }
                emitter.onSuccess(list);
            });
        }).blockingGet();
    }

    private static List<Purchase> queryPurchasesSync(BillingClient client, @BillingClient.ProductType String productType) {
        return Single.create((SingleOnSubscribe<List<Purchase>>) emitter -> {
            if (emitter.isDisposed()) {
                return;
            }
            if (!client.isReady()) {
                emitter.onSuccess(Collections.emptyList());
                return;
            }
            if (Objects.equals(productType, SUBS) && isSubscriptionSupported(client) != SupportState.SUPPORTED) {
                emitter.onSuccess(Collections.emptyList());
                return;
            }
            QueryPurchasesParams params = QueryPurchasesParams.newBuilder().setProductType(productType).build();
            client.queryPurchasesAsync(params, (billingResult, purchases) -> {
                if (emitter.isDisposed()) {
                    return;
                }
                emitter.onSuccess(purchases);
            });
        }).blockingGet();
    }

    private QueryHelper() {
        //no instance
    }
}
