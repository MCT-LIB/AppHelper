package com.mct.app.helper.iap.billing;

import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_DISCONNECTED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_TIMEOUT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.mct.app.helper.iap.billing.enums.AcknowledgeOutcome;
import com.mct.app.helper.iap.billing.enums.RefreshReason;
import com.mct.app.helper.iap.billing.models.OwnedPurchase;
import com.mct.app.helper.iap.billing.models.PurchaseOutcome;
import com.mct.app.helper.iap.billing.models.PurchasesResult;
import com.mct.app.helper.iap.billing.models.RefreshResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * The refresh state machine behind {@code BillingConnector.refreshPurchases}, kept free of
 * BillingClient so it can be unit-tested with fakes.
 * <ul>
 * <li>One refresh at a time. Requests made while one runs join it and share its result.</li>
 * <li>A result is never delivered if a purchase update arrived after its query was issued
 * (it could say "not subscribed" for a user who just paid); the query is re-issued instead.</li>
 * <li>A refresh that does not finish within {@link #TIMEOUT_MS} is delivered as SERVICE_TIMEOUT.</li>
 * <li>{@link #release()} fails the per-call callbacks so no spinner waits forever; late callbacks
 * from the released work are ignored.</li>
 * </ul>
 * Main thread only (everything enters through {@link Scheduler#post}).
 */
final class PurchasesRefresher {

    static final long TIMEOUT_MS = 30_000L;
    static final long ALREADY_OWNED_DEDUPE_MS = 2_000L;
    static final int MAX_STALE_REISSUES = 3;

    interface Cancellable {
        void cancel();
    }

    interface Scheduler {
        void post(@NonNull Runnable r);

        @NonNull
        Cancellable postDelayed(@NonNull Runnable r, long delayMs);

        long now();
    }

    interface Backend {
        /** Runs before each query (e.g. to retry product details). */
        void beforeQuery(@NonNull RefreshReason reason);

        /** Query both types; deliver on the main thread. */
        void queryPurchases(@NonNull Consumer<PurchasesResult> callback);

        /** Acknowledge one token (through the shared {@link AcknowledgeTracker}); main-thread result. */
        void acknowledge(@NonNull String purchaseToken, @NonNull AcknowledgeTracker.AckResult callback);

        boolean isAcknowledgeable(@NonNull OwnedPurchase purchase);

        boolean isSignatureVerificationEnabled();

        /** Called with every delivered (non-stale) result before listeners run. */
        void onDelivered(@NonNull PurchasesResult result);

        @Nullable
        PurchasesRefreshListener globalListener();

        void log(@NonNull String message);
    }

    private final Backend backend;
    private final Scheduler scheduler;
    private final AtomicInteger purchaseSeq = new AtomicInteger();

    private final List<PurchasesRefreshListener> pending = new ArrayList<>();
    private boolean inFlight;
    private int generation;
    private int seqAtQuery;
    private int staleReissues;
    private RefreshReason reason;
    @Nullable
    private String trigger;
    @Nullable
    private Cancellable timeout;
    @Nullable
    private String lastAlreadyOwnedTrigger;
    private long lastAlreadyOwnedAt = Long.MIN_VALUE / 2;

    PurchasesRefresher(@NonNull Backend backend, @NonNull Scheduler scheduler) {
        this.backend = backend;
        this.scheduler = scheduler;
    }

    /** Request a refresh. {@code callback} may be null. */
    void request(@NonNull RefreshReason reason, @Nullable String trigger, @Nullable PurchasesRefreshListener callback) {
        scheduler.post(() -> {
            if (reason == RefreshReason.ITEM_ALREADY_OWNED && callback == null
                    && isRecentAlreadyOwned(trigger)) {
                // Play reported the same ITEM_ALREADY_OWNED through both the launch result and
                // the purchases listener: one refresh is enough
                backend.log("Refresh: duplicate ITEM_ALREADY_OWNED ignored");
                return;
            }
            if (reason == RefreshReason.ITEM_ALREADY_OWNED) {
                lastAlreadyOwnedTrigger = trigger;
                lastAlreadyOwnedAt = scheduler.now();
            }
            if (callback != null) {
                pending.add(callback);
            }
            if (inFlight) {
                merge(reason, trigger);
                return;
            }
            start(reason, trigger);
        });
    }

    /**
     * Must be called synchronously (main thread) for every OK purchase update, before anything else
     * runs, so a refresh whose query predates it is never delivered.
     *
     * @param autoRefresh also request a refresh (acknowledges new purchases, reports PENDING → PURCHASED)
     */
    void onPurchaseUpdated(boolean autoRefresh) {
        purchaseSeq.incrementAndGet();
        if (autoRefresh) {
            request(RefreshReason.PURCHASE_UPDATED, null, null);
        }
    }

    /** Fails per-call callbacks and ignores any late result of the current work. */
    void release() {
        generation++;
        inFlight = false;
        cancelTimeout();
        List<PurchasesRefreshListener> callbacks = drainPending();
        if (!callbacks.isEmpty()) {
            RefreshResult failed = failure(SERVICE_DISCONNECTED, "Billing connector released");
            for (PurchasesRefreshListener cb : callbacks) {
                cb.onPurchasesRefreshed(failed);
            }
        }
    }

    @VisibleForTesting
    boolean isInFlight() {
        return inFlight;
    }

    private boolean isRecentAlreadyOwned(@Nullable String trigger) {
        boolean sameTrigger = trigger == null ? lastAlreadyOwnedTrigger == null : trigger.equals(lastAlreadyOwnedTrigger);
        return sameTrigger && scheduler.now() - lastAlreadyOwnedAt < ALREADY_OWNED_DEDUPE_MS;
    }

    private static int priority(RefreshReason r) {
        switch (r) {
            case ITEM_ALREADY_OWNED:
                return 3;
            case PURCHASE_UPDATED:
                return 2;
            case CONNECTED:
                return 1;
            default:
                return 0;
        }
    }

    private void merge(@NonNull RefreshReason newReason, @Nullable String newTrigger) {
        if (priority(newReason) > priority(reason)) {
            reason = newReason;
            trigger = newTrigger;
        }
    }

    private void start(@NonNull RefreshReason reason, @Nullable String trigger) {
        inFlight = true;
        this.reason = reason;
        this.trigger = trigger;
        staleReissues = 0;
        issueQuery();
    }

    private void issueQuery() {
        int gen = ++generation;
        seqAtQuery = purchaseSeq.get();
        cancelTimeout();
        timeout = scheduler.postDelayed(() -> onTimeout(gen), TIMEOUT_MS);
        backend.beforeQuery(reason);
        backend.queryPurchases(result -> {
            if (gen != generation) {
                return;
            }
            acknowledgeAll(gen, result);
        });
    }

    private void acknowledgeAll(int gen, @NonNull PurchasesResult result) {
        List<OwnedPurchase> purchases = result.getPurchases();
        PurchaseOutcome[] outcomes = new PurchaseOutcome[purchases.size()];
        int[] remaining = {1}; // released after the loop
        Runnable finishOne = () -> {
            if (--remaining[0] == 0 && gen == generation) {
                finish(result, Arrays.asList(outcomes));
            }
        };
        for (int i = 0; i < purchases.size(); i++) {
            OwnedPurchase p = purchases.get(i);
            AcknowledgeOutcome decision = PurchasesMapper.acknowledgeDecision(p, backend.isAcknowledgeable(p));
            if (decision != null) {
                outcomes[i] = new PurchaseOutcome(p, decision, OK, null);
                continue;
            }
            final int index = i;
            remaining[0]++;
            backend.acknowledge(p.getPurchaseToken(), (code, message) -> {
                backend.log("Refresh acknowledge " + p.getProducts() + ": " + code);
                outcomes[index] = new PurchaseOutcome(p,
                        code == OK ? AcknowledgeOutcome.ACKNOWLEDGED : AcknowledgeOutcome.FAILED, code, message);
                finishOne.run();
            });
        }
        finishOne.run();
    }

    private void finish(@NonNull PurchasesResult result, @NonNull List<PurchaseOutcome> outcomes) {
        if (purchaseSeq.get() != seqAtQuery && staleReissues < MAX_STALE_REISSUES) {
            // a purchase arrived after this query was issued: the result may not contain it
            staleReissues++;
            merge(RefreshReason.PURCHASE_UPDATED, null);
            backend.log("Refresh: purchase arrived during refresh, re-querying");
            issueQuery();
            return;
        }
        if (purchaseSeq.get() != seqAtQuery) {
            // purchases keep arriving: report the state as unknown rather than possibly stale
            deliver(failure(SERVICE_TIMEOUT, "Purchases kept changing during refresh"));
            return;
        }
        RefreshResult refresh = new RefreshResult(result, outcomes, reason, trigger);
        backend.onDelivered(result);
        deliver(refresh);
    }

    private void onTimeout(int gen) {
        if (gen != generation) {
            return;
        }
        generation++; // ignore the late answer
        backend.log("Refresh timed out");
        deliver(failure(SERVICE_TIMEOUT, "Refresh timed out"));
    }

    private void deliver(@NonNull RefreshResult result) {
        inFlight = false;
        cancelTimeout();
        backend.log("Refresh finished: " + result);
        List<PurchasesRefreshListener> callbacks = drainPending();
        for (PurchasesRefreshListener cb : callbacks) {
            cb.onPurchasesRefreshed(result);
        }
        PurchasesRefreshListener global = backend.globalListener();
        if (global != null && !callbacks.contains(global)) {
            global.onPurchasesRefreshed(result);
        }
    }

    @NonNull
    private RefreshResult failure(int code, @NonNull String message) {
        PurchasesResult r = new PurchasesResult(code, message, code, message,
                Collections.emptyList(), Collections.emptyList(), backend.isSignatureVerificationEnabled());
        return new RefreshResult(r, Collections.emptyList(),
                reason != null ? reason : RefreshReason.MANUAL, trigger);
    }

    private List<PurchasesRefreshListener> drainPending() {
        List<PurchasesRefreshListener> copy = new ArrayList<>(pending);
        pending.clear();
        return copy;
    }

    private void cancelTimeout() {
        if (timeout != null) {
            timeout.cancel();
            timeout = null;
        }
    }

    /**
     * Legacy {@code isPurchased()} cache may only be rebuilt when product details are loaded
     * (entries are built from them) and at least one query succeeded.
     */
    static boolean canUpdateLegacyCache(boolean productDetailsLoaded, @NonNull PurchasesResult r) {
        return productDetailsLoaded && (r.isSubscriptionsQueryOk() || r.isInAppQueryOk());
    }

    /** The legacy cache counts as "fetched" only with product details and both queries OK. */
    static boolean canMarkLegacyCacheFetched(boolean productDetailsLoaded, @NonNull PurchasesResult r) {
        return productDetailsLoaded && r.isSuccess();
    }
}
