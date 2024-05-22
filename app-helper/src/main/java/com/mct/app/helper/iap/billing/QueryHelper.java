package com.mct.app.helper.iap.billing;

import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_DISCONNECTED;
import static com.android.billingclient.api.BillingClient.FeatureType.SUBSCRIPTIONS;
import static com.android.billingclient.api.BillingClient.ProductType.INAPP;
import static com.android.billingclient.api.BillingClient.ProductType.SUBS;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.mct.app.helper.iap.billing.enums.SupportState;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.BiConsumer;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.schedulers.Schedulers;

class QueryHelper {

    static SupportState isSubscriptionSupported(@NonNull BillingClient billingClient) {
        switch (billingClient.isFeatureSupported(SUBSCRIPTIONS).getResponseCode()) {
            // @formatter:off
            case OK:                    return SupportState.SUPPORTED;
            case SERVICE_DISCONNECTED:  return SupportState.DISCONNECTED;
            default:                    return SupportState.NOT_SUPPORTED;
            // @formatter:on
        }
    }

    @NonNull
    static Disposable queryProductDetails(BillingClient client,
                                          List<QueryProductDetailsParams.Product> products,
                                          Consumer<List<ProductDetails>> listener) {
        return Single.zip(
                        queryProductDetails(client, products.stream().filter(p -> p.zzb().equals(INAPP)).collect(Collectors.toList())),
                        queryProductDetails(client, products.stream().filter(p -> p.zzb().equals(SUBS)).collect(Collectors.toList())),
                        getMapper())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getConsumer(listener));
    }

    @NonNull
    static Disposable queryPurchases(BillingClient client, Consumer<List<Purchase>> listener) {
        return Single.zip(
                        queryPurchases(client, INAPP),
                        queryPurchases(client, SUBS),
                        getMapper())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getConsumer(listener));
    }

    @NonNull
    private static Single<List<ProductDetails>> queryProductDetails(BillingClient client, List<QueryProductDetailsParams.Product> products) {
        return Single.create(emitter -> {
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
        });
    }

    @NonNull
    private static Single<List<Purchase>> queryPurchases(BillingClient client, @BillingClient.ProductType String productType) {
        return Single.create(emitter -> {
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
        });
    }

    @NonNull
    private static <T> BiFunction<List<T>, List<T>, List<T>> getMapper() {
        return (r1, r2) -> Stream.of(r1, r2).flatMap(List::stream).collect(Collectors.toList());
    }

    @NonNull
    private static <T> BiConsumer<List<T>, Throwable> getConsumer(Consumer<List<T>> listener) {
        return (result, throwable) -> {
            if (listener == null) {
                return;
            }
            listener.accept(Optional.ofNullable(result).orElseGet(Collections::emptyList));
        };
    }

    private QueryHelper() {
        //no instance
    }
}
