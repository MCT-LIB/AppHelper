package com.mct.app.helper.iap.billing;

import static com.android.billingclient.api.BillingClient.BillingResponseCode.DEVELOPER_ERROR;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ERROR;
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
import java.util.List;
import java.util.Set;

/**
 * Consume-by-token behind {@code BillingConnector.consume}, kept free of BillingClient so it can be
 * unit-tested with fakes.
 * <ul>
 * <li>Each call reports exactly once. A call for a token that is already being consumed joins that
 * call; a token consumed earlier in the session answers OK without calling Play.</li>
 * <li>Grant before consume: the library only consumes tokens the app passed in (which it must
 * have granted). A PENDING purchase is refused and never kept, so a purchase that clears later is
 * reported by the refresh, granted by the app and only then passed in again.</li>
 * <li>Tokens given with {@code retryOnFailure} that fail (anything but OK / ITEM_NOT_OWNED) are
 * kept. After each delivered refresh, a kept token is:
 * <ul>
 * <li>consumed again if the refresh shows it PURCHASED and verified;</li>
 * <li>kept and reported {@link ConsumeResult#isRejected() rejected} (not consumed) while signature
 * verification rejects it;</li>
 * <li>dropped and reported {@link ConsumeResult#isPending() pending} if the refresh shows it PENDING
 * (the app passed a token it should not have granted yet);</li>
 * <li>dropped and reported ITEM_NOT_OWNED if a fully successful refresh no longer lists it;</li>
 * <li>left alone if the refresh failed and does not list it.</li>
 * </ul>
 * Retry results go to the global {@link ConsumeListener}.</li>
 * <li>{@link #release()} answers in-flight calls, and calls posted but not started, with
 * SERVICE_DISCONNECTED and forgets the kept tokens.</li>
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
    /** Calls posted to the main thread but not started yet; any thread. */
    private final List<Queued> queued = new ArrayList<>();

    private static final class Queued {
        final String token;
        @Nullable
        final ConsumeListener callback;

        Queued(@NonNull String token, @Nullable ConsumeListener callback) {
            this.token = token;
            this.callback = callback;
        }
    }

    PurchaseConsumer(@NonNull Backend backend, @NonNull PurchasesRefresher.Scheduler scheduler) {
        this.backend = backend;
        this.scheduler = scheduler;
    }

    void consume(@NonNull String token, boolean retryOnFailure, @Nullable ConsumeListener callback) {
        enqueue(token, callback, () -> run(token, retryOnFailure, callback, false));
    }

    /**
     * Checks the purchase first: a subscription or a PENDING purchase is refused with
     * DEVELOPER_ERROR and never kept.
     */
    void consume(@NonNull OwnedPurchase purchase, boolean retryOnFailure, @Nullable ConsumeListener callback) {
        String token = purchase.getPurchaseToken();
        String refusal = refusal(purchase);
        if (refusal == null) {
            consume(token, retryOnFailure, callback);
            return;
        }
        boolean pending = purchase.isPending();
        enqueue(token, callback, () ->
                report(callback, new ConsumeResult(token, DEVELOPER_ERROR, refusal, false, pending, false)));
    }

    private void enqueue(@NonNull String token, @Nullable ConsumeListener callback, @NonNull Runnable work) {
        Queued q = new Queued(token, callback);
        synchronized (queued) {
            queued.add(q);
        }
        scheduler.post(() -> {
            synchronized (queued) {
                if (!queued.remove(q)) {
                    return; // release() already answered it
                }
            }
            work.run();
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
            OwnedPurchase p = findToken(result.getPurchases(), token);
            if (p == null) {
                OwnedPurchase rejected = findToken(result.getRejected(), token);
                if (rejected != null) {
                    // not "not owned": verification failed (wrong licence key?). Keep it and let the
                    // app decide; consuming an unverified purchase is not the library's call
                    report(backend.globalListener(), new ConsumeResult(token, ERROR,
                            "Signature verification rejected this purchase", true, false, true));
                } else if (result.isSuccess()) {
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
                // never granted: the app passes it again after granting it as PURCHASED
                retry.remove(token);
                report(backend.globalListener(), new ConsumeResult(token, DEVELOPER_ERROR, refusal(p), false, true, false));
                continue;
            }
            backend.log("Consume retry: " + p.getProducts());
            run(token, true, null, true);
        }
    }

    void release() {
        generation++;
        retry.clear();
        List<Queued> notStarted;
        synchronized (queued) {
            notStarted = new ArrayList<>(queued);
            queued.clear();
        }
        tracker.failInFlight(SERVICE_DISCONNECTED, "Billing connector released");
        for (Queued q : notStarted) {
            report(q.callback, new ConsumeResult(q.token, SERVICE_DISCONNECTED, "Billing connector released", false));
        }
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
    private static OwnedPurchase findToken(@NonNull List<OwnedPurchase> purchases, @NonNull String token) {
        for (OwnedPurchase p : purchases) {
            if (token.equals(p.getPurchaseToken())) {
                return p;
            }
        }
        return null;
    }
}
