package com.mct.app.helper.iap.billing;

import static com.android.billingclient.api.BillingClient.BillingResponseCode.DEVELOPER_ERROR;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_NOT_OWNED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_DISCONNECTED;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.mct.app.helper.iap.billing.models.ConsumeResult;
import com.mct.app.helper.iap.billing.models.OwnedPurchase;
import com.mct.app.helper.iap.billing.models.PurchasesResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Consume-by-token behind {@code BillingConnector.consume}, kept free of BillingClient so it can be
 * unit-tested with fakes.
 * <ul>
 * <li>Each call reports exactly once. A call for a token that is already being consumed joins that
 * call; a token consumed earlier in the session answers OK without calling Play.</li>
 * <li>Tokens given with {@code retryOnFailure} that fail (anything but OK / ITEM_NOT_OWNED), or are
 * still PENDING, are kept. They are consumed again after each delivered refresh that shows them
 * PURCHASED. A token that a fully successful refresh no longer lists is dropped and reported as
 * ITEM_NOT_OWNED. Retry results go to the global {@link ConsumeListener}.</li>
 * <li>{@link #release()} answers in-flight calls with SERVICE_DISCONNECTED and forgets the retries.</li>
 * </ul>
 * Main thread only (public entry points go through {@link PurchasesRefresher.Scheduler#post}).
 */
final class PurchaseConsumer {

    interface Backend {
        /** Raw Play consume of one token; result delivered on the main thread. */
        void consume(@NonNull String purchaseToken, @NonNull AcknowledgeTracker.AckResult result);

        @Nullable
        ConsumeListener globalListener();

        void log(@NonNull String message);
    }

    private final Backend backend;
    private final PurchasesRefresher.Scheduler scheduler;
    private final AcknowledgeTracker tracker = new AcknowledgeTracker();
    private final Set<String> retry = new LinkedHashSet<>();
    private int generation;

    PurchaseConsumer(@NonNull Backend backend, @NonNull PurchasesRefresher.Scheduler scheduler) {
        this.backend = backend;
        this.scheduler = scheduler;
    }

    void consume(@NonNull String token, boolean retryOnFailure, @Nullable ConsumeListener callback) {
        scheduler.post(() -> run(token, retryOnFailure, callback, false));
    }

    /**
     * Checks the purchase first: a subscription is refused with DEVELOPER_ERROR, and so is a
     * PENDING purchase (kept for retry when {@code retryOnFailure}).
     */
    void consume(@NonNull OwnedPurchase purchase, boolean retryOnFailure, @Nullable ConsumeListener callback) {
        String token = purchase.getPurchaseToken();
        String refusal = refusal(purchase);
        if (refusal == null) {
            consume(token, retryOnFailure, callback);
            return;
        }
        boolean keep = retryOnFailure && !purchase.isSubscription();
        scheduler.post(() -> {
            if (keep) {
                retry.add(token); // consumed once a refresh shows it PURCHASED
            }
            report(callback, new ConsumeResult(token, DEVELOPER_ERROR, refusal, keep));
        });
    }

    /** Retries the kept tokens against a delivered refresh. */
    void onRefreshDelivered(@NonNull PurchasesResult result) {
        if (retry.isEmpty()) {
            return;
        }
        for (String token : new ArrayList<>(retry)) {
            if (tracker.isInFlight(token)) {
                continue;
            }
            OwnedPurchase p = findToken(result, token);
            if (p == null) {
                if (result.isSuccess()) {
                    // Play lists every owned purchase and this one is gone: consumed or refunded
                    retry.remove(token);
                    report(backend.globalListener(), new ConsumeResult(token, ITEM_NOT_OWNED, "No longer owned", false));
                }
                continue;
            }
            if (p.isSubscription()) {
                retry.remove(token);
                report(backend.globalListener(), new ConsumeResult(token, DEVELOPER_ERROR, refusal(p), false));
                continue;
            }
            if (!p.isPurchased()) {
                continue; // still PENDING
            }
            backend.log("Consume retry: " + p.getProducts());
            run(token, true, null, true);
        }
    }

    void release() {
        generation++;
        retry.clear();
        tracker.failInFlight(SERVICE_DISCONNECTED, "Billing connector released");
    }

    @NonNull
    Set<String> getRetryTokens() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(retry));
    }

    @VisibleForTesting
    boolean isConsumed(@NonNull String token) {
        return tracker.isDone(token);
    }

    private void run(@NonNull String token, boolean retryOnFailure, @Nullable ConsumeListener callback, boolean toGlobal) {
        int gen = generation;
        tracker.acknowledge(token, backend::consume, (code, message) -> {
            boolean scheduled = false;
            if (gen == generation) {
                if (code == OK || code == ITEM_NOT_OWNED) {
                    retry.remove(token);
                } else if (retryOnFailure || retry.contains(token)) {
                    retry.add(token);
                    scheduled = true;
                }
            }
            backend.log("Consume " + token + ": " + code + (scheduled ? " (retry scheduled)" : ""));
            report(toGlobal ? backend.globalListener() : callback, new ConsumeResult(token, code, message, scheduled));
        });
    }

    private static void report(@Nullable ConsumeListener listener, @NonNull ConsumeResult result) {
        if (listener != null) {
            listener.onConsumeResult(result);
        }
    }

    @Nullable
    private static String refusal(@NonNull OwnedPurchase p) {
        if (p.isSubscription()) {
            return "Subscriptions cannot be consumed";
        }
        if (!p.isPurchased()) {
            return "Only a PURCHASED purchase can be consumed (state " + p.getState() + ")";
        }
        return null;
    }

    @Nullable
    private static OwnedPurchase findToken(@NonNull PurchasesResult result, @NonNull String token) {
        for (OwnedPurchase p : result.getPurchases()) {
            if (token.equals(p.getPurchaseToken())) {
                return p;
            }
        }
        return null;
    }
}
