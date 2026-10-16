package com.mct.app.helper.iap.billing;

import static com.android.billingclient.api.BillingClient.BillingResponseCode.ERROR;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.NETWORK_ERROR;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_DISCONNECTED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_TIMEOUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mct.app.helper.iap.billing.enums.AcknowledgeOutcome;
import com.mct.app.helper.iap.billing.enums.PurchaseState;
import com.mct.app.helper.iap.billing.enums.RefreshReason;
import com.mct.app.helper.iap.billing.models.OwnedPurchase;
import com.mct.app.helper.iap.billing.models.PurchasesResult;
import com.mct.app.helper.iap.billing.models.RefreshResult;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The refresh state machine with a fake Play backend and a manual clock: sharing, the
 * purchase-during-refresh race (I3), acknowledge counting, timeout, release and dedupe.
 */
public class PurchasesRefresherTest {

    private FakeScheduler scheduler;
    private FakeBackend backend;
    private PurchasesRefresher refresher;
    private final List<RefreshResult> global = new ArrayList<>();

    @Before
    public void setUp() {
        scheduler = new FakeScheduler();
        backend = new FakeBackend();
        backend.global = global::add;
        refresher = new PurchasesRefresher(backend, scheduler);
    }

    // --- fixtures ---

    private static OwnedPurchase sub(String id, PurchaseState state, boolean ack) {
        return new OwnedPurchase("subs", Collections.singletonList(id), "tok-" + id, null,
                state, ack, true, false, 1, 0L, false, null);
    }

    private static PurchasesResult ok(OwnedPurchase... purchases) {
        return new PurchasesResult(OK, null, OK, null, Arrays.asList(purchases), Collections.emptyList(), false);
    }

    // --- tests ---

    @Test
    public void concurrentRequestsShareOneQueryAndResult() {
        List<RefreshResult> a = new ArrayList<>();
        List<RefreshResult> b = new ArrayList<>();
        refresher.request(RefreshReason.MANUAL, null, a::add);
        refresher.request(RefreshReason.MANUAL, null, b::add);
        assertEquals(1, backend.queries.size());

        backend.answer(ok(sub("annual", PurchaseState.PURCHASED, true)));
        assertEquals(1, a.size());
        assertSame(a.get(0), b.get(0));
        assertEquals("global listener once", 1, global.size());
        assertFalse(refresher.isInFlight());
    }

    @Test
    public void purchaseDuringRefresh_staleResultIsNotDeliveredAndQueryReissued() {
        refresher.request(RefreshReason.CONNECTED, null, null);
        // the user completes a purchase while the query is in flight
        refresher.onPurchaseUpdated(false);

        backend.answer(ok()); // stale: predates the purchase
        assertTrue("stale 'not subscribed' must not reach the app", global.isEmpty());
        assertEquals("query re-issued", 2, backend.queries.size());

        backend.answer(ok(sub("annual", PurchaseState.PURCHASED, true)));
        assertEquals(1, global.size());
        assertEquals(Boolean.TRUE, global.get(0).getPurchases().isSubscribed());
        assertEquals(RefreshReason.PURCHASE_UPDATED, global.get(0).getReason());
    }

    @Test
    public void purchaseDuringAcknowledgePhase_isAlsoCaught() {
        refresher.request(RefreshReason.MANUAL, null, null);
        backend.answer(ok(sub("weekly", PurchaseState.PURCHASED, false)));
        assertEquals(1, backend.acks.size());

        refresher.onPurchaseUpdated(false);
        backend.finishAck("tok-weekly", OK);
        assertTrue(global.isEmpty());
        assertEquals(2, backend.queries.size());
    }

    @Test
    public void okPurchaseUpdateStartsRefreshWhenAutoRefreshOn() {
        refresher.onPurchaseUpdated(true);
        assertEquals(1, backend.queries.size());
        backend.answer(ok(sub("annual", PurchaseState.PURCHASED, true)));
        assertEquals(RefreshReason.PURCHASE_UPDATED, global.get(0).getReason());
    }

    @Test
    public void acknowledgesUnackedPurchasedOnlyAndWaitsForAllAcks() {
        refresher.request(RefreshReason.MANUAL, null, null);
        backend.answer(ok(
                sub("unacked", PurchaseState.PURCHASED, false),
                sub("acked", PurchaseState.PURCHASED, true),
                sub("pending", PurchaseState.PENDING, false),
                sub("failing", PurchaseState.PURCHASED, false)));

        assertEquals(Arrays.asList("tok-unacked", "tok-failing"), new ArrayList<>(backend.acks.keySet()));
        backend.finishAck("tok-unacked", OK);
        assertTrue("delivered only after every acknowledge returned", global.isEmpty());
        backend.finishAck("tok-failing", ERROR);

        RefreshResult r = global.get(0);
        assertEquals(AcknowledgeOutcome.ACKNOWLEDGED, r.getOutcome("tok-unacked").getAcknowledgeOutcome());
        assertEquals(AcknowledgeOutcome.ALREADY_ACKNOWLEDGED, r.getOutcome("tok-acked").getAcknowledgeOutcome());
        assertEquals(AcknowledgeOutcome.PENDING, r.getOutcome("tok-pending").getAcknowledgeOutcome());
        assertEquals(AcknowledgeOutcome.FAILED, r.getOutcome("tok-failing").getAcknowledgeOutcome());
        assertEquals(ERROR, r.getOutcome("tok-failing").getResponseCode());
    }

    @Test
    public void failedQueryIsDeliveredAsUnknown() {
        refresher.request(RefreshReason.MANUAL, null, null);
        backend.answer(new PurchasesResult(NETWORK_ERROR, "offline", OK, null,
                Collections.emptyList(), Collections.emptyList(), false));
        assertFalse(global.get(0).isSuccess());
        assertNull(global.get(0).getPurchases().isSubscribed());
        assertEquals("delivered once", 1, backend.delivered.size());
    }

    @Test
    public void timeoutDeliversFailureAndIgnoresLateAnswer() {
        List<RefreshResult> cb = new ArrayList<>();
        refresher.request(RefreshReason.MANUAL, null, cb::add);
        scheduler.advance(PurchasesRefresher.TIMEOUT_MS);

        assertEquals(1, cb.size());
        assertEquals(SERVICE_TIMEOUT, cb.get(0).getPurchases().getResponseCode());
        assertFalse(refresher.isInFlight());

        backend.answer(ok(sub("annual", PurchaseState.PURCHASED, true)));
        assertEquals("late answer ignored", 1, global.size());

        refresher.request(RefreshReason.MANUAL, null, null);
        assertEquals("a new refresh can start after a timeout", 2, backend.queries.size());
    }

    @Test
    public void releaseFailsPendingCallbacksAndIgnoresLateWork() {
        List<RefreshResult> cb = new ArrayList<>();
        refresher.request(RefreshReason.MANUAL, null, cb::add);
        refresher.release();

        assertEquals(1, cb.size());
        assertEquals(SERVICE_DISCONNECTED, cb.get(0).getPurchases().getResponseCode());
        assertTrue("global listener is not called for a released refresh", global.isEmpty());

        backend.answer(ok());
        assertEquals(1, cb.size());
        assertTrue(global.isEmpty());
    }

    @Test
    public void duplicateAlreadyOwnedIsDeduplicated() {
        refresher.request(RefreshReason.ITEM_ALREADY_OWNED, "annual", null);
        refresher.request(RefreshReason.ITEM_ALREADY_OWNED, "annual", null);
        backend.answer(ok(sub("annual", PurchaseState.PURCHASED, true)));
        // launch result and purchases listener both report it, the second just after delivery
        refresher.request(RefreshReason.ITEM_ALREADY_OWNED, "annual", null);

        assertEquals(1, backend.queries.size());
        assertEquals(1, global.size());
        assertEquals(RefreshReason.ITEM_ALREADY_OWNED, global.get(0).getReason());
        assertEquals("annual", global.get(0).getTriggerProductId());

        scheduler.advance(PurchasesRefresher.ALREADY_OWNED_DEDUPE_MS);
        refresher.request(RefreshReason.ITEM_ALREADY_OWNED, "annual", null);
        assertEquals("a later attempt refreshes again", 2, backend.queries.size());
    }

    @Test
    public void joinedAlreadyOwnedUpgradesReasonOfRunningRefresh() {
        refresher.request(RefreshReason.CONNECTED, null, null);
        refresher.request(RefreshReason.ITEM_ALREADY_OWNED, "annual", null);
        backend.answer(ok());
        assertEquals(1, backend.queries.size());
        assertEquals(RefreshReason.ITEM_ALREADY_OWNED, global.get(0).getReason());
    }

    @Test
    public void legacyCachePolicy() {
        PurchasesResult allOk = ok();
        PurchasesResult subsOnly = new PurchasesResult(OK, null, NETWORK_ERROR, null,
                Collections.emptyList(), Collections.emptyList(), false);

        assertFalse("no product details: cache untouched", PurchasesRefresher.canUpdateLegacyCache(false, allOk));
        assertFalse("no product details: never marked fetched", PurchasesRefresher.canMarkLegacyCacheFetched(false, allOk));
        assertTrue(PurchasesRefresher.canUpdateLegacyCache(true, subsOnly));
        assertFalse(PurchasesRefresher.canMarkLegacyCacheFetched(true, subsOnly));
        assertTrue(PurchasesRefresher.canMarkLegacyCacheFetched(true, allOk));
    }

    @Test
    public void acknowledgeTrackerJoinsConcurrentCallsForSameToken() {
        AcknowledgeTracker tracker = new AcknowledgeTracker();
        List<AcknowledgeTracker.AckResult> playCalls = new ArrayList<>();
        AcknowledgeTracker.AckCall play = (token, result) -> playCalls.add(result);
        int[] legacy = {Integer.MIN_VALUE};
        int[] refresh = {Integer.MIN_VALUE};

        tracker.acknowledge("t", play, (code, msg) -> legacy[0] = code);   // auto-acknowledge path
        tracker.acknowledge("t", play, (code, msg) -> refresh[0] = code);  // refresh path
        assertEquals("one Play call for both", 1, playCalls.size());

        playCalls.get(0).onResult(OK, null);
        assertEquals(OK, legacy[0]);
        assertEquals(OK, refresh[0]);

        int[] again = {Integer.MIN_VALUE};
        tracker.acknowledge("t", play, (code, msg) -> again[0] = code);
        assertEquals("already acknowledged this session: no Play call", 1, playCalls.size());
        assertEquals(OK, again[0]);
    }

    @Test
    public void acknowledgeTrackerRetriesAfterFailure() {
        AcknowledgeTracker tracker = new AcknowledgeTracker();
        List<AcknowledgeTracker.AckResult> playCalls = new ArrayList<>();
        AcknowledgeTracker.AckCall play = (token, result) -> playCalls.add(result);
        tracker.acknowledge("t", play, (code, msg) -> {
        });
        playCalls.get(0).onResult(ERROR, "boom");
        tracker.acknowledge("t", play, (code, msg) -> {
        });
        assertEquals(2, playCalls.size());
    }

    // --- fakes ---

    private static final class FakeBackend implements PurchasesRefresher.Backend {
        final List<Consumer<PurchasesResult>> queries = new ArrayList<>();
        final Map<String, AcknowledgeTracker.AckResult> acks = new LinkedHashMap<>();
        final List<PurchasesResult> delivered = new ArrayList<>();
        PurchasesRefreshListener global;

        void answer(PurchasesResult r) {
            queries.get(queries.size() - 1).accept(r);
        }

        void finishAck(String token, int code) {
            acks.remove(token).onResult(code, null);
        }

        @Override
        public void beforeQuery(@NonNull RefreshReason reason) {
        }

        @Override
        public void queryPurchases(@NonNull Consumer<PurchasesResult> callback) {
            queries.add(callback);
        }

        @Override
        public void acknowledge(@NonNull String purchaseToken, @NonNull AcknowledgeTracker.AckResult callback) {
            acks.put(purchaseToken, callback);
        }

        @Override
        public boolean isAcknowledgeable(@NonNull OwnedPurchase purchase) {
            return purchase.isSubscription();
        }

        @Override
        public boolean isSignatureVerificationEnabled() {
            return false;
        }

        @Override
        public void onDelivered(@NonNull PurchasesResult result) {
            delivered.add(result);
        }

        @Nullable
        @Override
        public PurchasesRefreshListener globalListener() {
            return global;
        }

        @Override
        public void log(@NonNull String message) {
        }
    }
}
