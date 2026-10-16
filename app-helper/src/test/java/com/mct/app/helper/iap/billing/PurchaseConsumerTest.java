package com.mct.app.helper.iap.billing;

import static com.android.billingclient.api.BillingClient.BillingResponseCode.DEVELOPER_ERROR;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ERROR;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_NOT_OWNED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.NETWORK_ERROR;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_DISCONNECTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mct.app.helper.iap.billing.enums.PurchaseState;
import com.mct.app.helper.iap.billing.models.ConsumeResult;
import com.mct.app.helper.iap.billing.models.OwnedPurchase;
import com.mct.app.helper.iap.billing.models.PurchasesResult;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Consume by token with a fake Play backend: one result per call, sharing and caching per token,
 * retry after refresh, release.
 */
public class PurchaseConsumerTest {

    private FakeScheduler scheduler;
    private FakePlay play;
    private PurchaseConsumer consumer;
    private final List<ConsumeResult> global = new ArrayList<>();

    @Before
    public void setUp() {
        scheduler = new FakeScheduler();
        play = new FakePlay();
        play.global = global::add;
        consumer = new PurchaseConsumer(play, scheduler);
    }

    private static OwnedPurchase pack(String token, PurchaseState state) {
        return new OwnedPurchase("inapp", Collections.singletonList("credits_10"), token, null,
                state, false, false, false, 1, 0L, false, null);
    }

    private static OwnedPurchase sub(String token) {
        return new OwnedPurchase("subs", Collections.singletonList("annual"), token, null,
                PurchaseState.PURCHASED, true, true, false, 1, 0L, false, null);
    }

    private static PurchasesResult ok(OwnedPurchase... purchases) {
        return new PurchasesResult(OK, null, OK, null, Arrays.asList(purchases), Collections.emptyList(), false);
    }

    @Test
    public void reportsOnceAndCachesSuccess() {
        List<ConsumeResult> a = new ArrayList<>();
        consumer.consume("t", false, a::add);
        assertEquals(1, play.calls.size());
        assertTrue("no result before Play answers", a.isEmpty());

        play.answer("t", OK);
        assertEquals(1, a.size());
        assertTrue(a.get(0).isSuccess());
        assertFalse(a.get(0).isRetryScheduled());

        List<ConsumeResult> again = new ArrayList<>();
        consumer.consume("t", false, again::add);
        assertEquals("consumed this session: no Play call", 1, play.total);
        assertTrue(again.get(0).isSuccess());
        assertTrue(consumer.isConsumed("t"));
    }

    @Test
    public void concurrentCallsShareOnePlayCall() {
        List<ConsumeResult> legacy = new ArrayList<>();
        List<ConsumeResult> app = new ArrayList<>();
        consumer.consume("t", false, legacy::add);
        consumer.consume("t", false, app::add);
        assertEquals(1, play.total);

        play.answer("t", OK);
        assertEquals(1, legacy.size());
        assertEquals(1, app.size());
        assertTrue(legacy.get(0).isSuccess());
        assertTrue(app.get(0).isSuccess());
    }

    @Test
    public void failureWithoutRetryIsNotKeptAndCanBeCalledAgain() {
        List<ConsumeResult> r = new ArrayList<>();
        consumer.consume("t", false, r::add);
        play.answer("t", ERROR);
        assertEquals(ERROR, r.get(0).getResponseCode());
        assertFalse(r.get(0).isRetryScheduled());
        assertTrue(consumer.getRetryTokens().isEmpty());

        consumer.consume("t", false, r::add);
        assertEquals("a failed token calls Play again", 2, play.total);
    }

    @Test
    public void failedTokenIsRetriedOnNextRefreshAndReportedToGlobal() {
        List<ConsumeResult> first = new ArrayList<>();
        consumer.consume("t", true, first::add);
        play.answer("t", NETWORK_ERROR);
        assertTrue(first.get(0).isRetryScheduled());
        assertEquals(Collections.singleton("t"), consumer.getRetryTokens());

        consumer.onRefreshDelivered(ok(pack("t", PurchaseState.PURCHASED)));
        assertEquals("retried", 2, play.total);
        play.answer("t", OK);

        assertEquals("per-call listener got only its own result", 1, first.size());
        assertEquals(1, global.size());
        assertTrue(global.get(0).isSuccess());
        assertTrue(consumer.getRetryTokens().isEmpty());
    }

    @Test
    public void retryFailingAgainStaysScheduled() {
        consumer.consume("t", true, null);
        play.answer("t", ERROR);
        consumer.onRefreshDelivered(ok(pack("t", PurchaseState.PURCHASED)));
        play.answer("t", ERROR);
        assertTrue(global.get(0).isRetryScheduled());
        assertEquals(Collections.singleton("t"), consumer.getRetryTokens());
    }

    @Test
    public void tokenGoneFromSuccessfulRefreshIsDroppedAsNotOwned() {
        consumer.consume("t", true, null);
        play.answer("t", ERROR);

        consumer.onRefreshDelivered(new PurchasesResult(OK, null, NETWORK_ERROR, null,
                Collections.emptyList(), Collections.emptyList(), false));
        assertTrue("a failed query proves nothing: kept", global.isEmpty());
        assertEquals(1, consumer.getRetryTokens().size());

        consumer.onRefreshDelivered(ok());
        assertEquals(1, play.total);
        assertEquals(ITEM_NOT_OWNED, global.get(0).getResponseCode());
        assertTrue(global.get(0).isNotOwned());
        assertTrue(consumer.getRetryTokens().isEmpty());
    }

    @Test
    public void itemNotOwnedIsFinal() {
        consumer.consume("t", true, null);
        play.answer("t", ITEM_NOT_OWNED);
        assertTrue(consumer.getRetryTokens().isEmpty());
    }

    @Test
    public void suspendedSubscriptionIsNeverConsumed() {
        OwnedPurchase held = new OwnedPurchase("subs", Collections.singletonList("annual"), "held", null,
                PurchaseState.SUSPENDED, true, false, true, 1, 0L, false, null);
        List<ConsumeResult> r = new ArrayList<>();
        consumer.consume(held, true, r::add);
        assertEquals(0, play.total);
        assertEquals(DEVELOPER_ERROR, r.get(0).getResponseCode());
        assertTrue(consumer.getRetryTokens().isEmpty());
    }

    @Test
    public void pendingPurchaseIsRefusedNotKeptAndNeverConsumedWithoutTheApp() {
        // R3: grant before consume
        List<ConsumeResult> r = new ArrayList<>();
        consumer.consume(pack("t", PurchaseState.PENDING), true, r::add);
        assertEquals(0, play.total);
        assertEquals(DEVELOPER_ERROR, r.get(0).getResponseCode());
        assertTrue(r.get(0).isPending());
        assertFalse(r.get(0).isRetryScheduled());
        assertTrue(consumer.getRetryTokens().isEmpty());

        consumer.onRefreshDelivered(ok(pack("t", PurchaseState.PURCHASED)));
        assertEquals("cleared purchase is not consumed before the app grants it", 0, play.total);
        assertTrue(global.isEmpty());

        // the app saw PURCHASED in the refresh, granted it, and passes it again
        consumer.consume(pack("t", PurchaseState.PURCHASED), true, r::add);
        assertEquals(1, play.total);
        play.answer("t", OK);
        assertTrue(r.get(1).isSuccess());
    }

    @Test
    public void keptTokenSeenPendingIsDroppedAndReportedNotConsumed() {
        // R3: a token passed by string that turns out PENDING is never consumed on its own
        consumer.consume("t", true, null);
        play.answer("t", ERROR);
        consumer.onRefreshDelivered(ok(pack("t", PurchaseState.PENDING)));
        assertEquals(1, play.total);
        assertTrue(global.get(0).isPending());
        assertTrue(consumer.getRetryTokens().isEmpty());

        consumer.onRefreshDelivered(ok(pack("t", PurchaseState.PURCHASED)));
        assertEquals("not consumed after it clears either", 1, play.total);
    }

    @Test
    public void rejectedTokenStaysKeptIsReportedAndIsNotConsumed() {
        // R1
        consumer.consume("t", true, null);
        play.answer("t", ERROR);

        PurchasesResult rejected = new PurchasesResult(OK, null, OK, null, Collections.emptyList(),
                Collections.singletonList(pack("t", PurchaseState.PURCHASED)), true);
        consumer.onRefreshDelivered(rejected);
        assertEquals("not consumed while unverified", 1, play.total);
        assertEquals(1, global.size());
        assertTrue(global.get(0).isRejected());
        assertFalse("not reported as not owned", global.get(0).isNotOwned());
        assertTrue(global.get(0).isRetryScheduled());
        assertEquals(Collections.singleton("t"), consumer.getRetryTokens());

        // licence key fixed: the next refresh verifies it and the kept token is consumed
        consumer.onRefreshDelivered(ok(pack("t", PurchaseState.PURCHASED)));
        assertEquals(2, play.total);
        play.answer("t", OK);
        assertTrue(global.get(1).isSuccess());
    }

    @Test
    public void subscriptionAndPendingWithoutRetryAreRefused() {
        List<ConsumeResult> r = new ArrayList<>();
        consumer.consume(sub("s"), true, r::add);
        consumer.consume(pack("p", PurchaseState.PENDING), false, r::add);
        assertEquals(0, play.total);
        assertEquals(2, r.size());
        assertEquals(DEVELOPER_ERROR, r.get(0).getResponseCode());
        assertFalse("a subscription is never kept", r.get(0).isRetryScheduled());
        assertFalse(r.get(1).isRetryScheduled());
        assertTrue(consumer.getRetryTokens().isEmpty());
    }

    @Test
    public void purchasedOwnedPurchaseIsConsumed() {
        List<ConsumeResult> r = new ArrayList<>();
        consumer.consume(pack("t", PurchaseState.PURCHASED), false, r::add);
        play.answer("t", OK);
        assertTrue(r.get(0).isSuccess());
    }

    @Test
    public void refreshDoesNotStartSecondCallWhileOneIsInFlight() {
        consumer.consume("t", true, null);
        play.answer("t", ERROR);
        consumer.onRefreshDelivered(ok(pack("t", PurchaseState.PURCHASED)));
        consumer.onRefreshDelivered(ok(pack("t", PurchaseState.PURCHASED)));
        assertEquals(2, play.total);
    }

    @Test
    public void releaseAnswersInFlightOnceAndIgnoresLateAnswer() {
        List<ConsumeResult> r = new ArrayList<>();
        consumer.consume("t", true, r::add);
        consumer.release();
        assertEquals(1, r.size());
        assertEquals(SERVICE_DISCONNECTED, r.get(0).getResponseCode());
        assertFalse(r.get(0).isRetryScheduled());
        assertTrue(consumer.getRetryTokens().isEmpty());

        play.answer("t", OK);
        assertEquals("late Play answer ignored", 1, r.size());
        assertFalse(consumer.isConsumed("t"));
    }

    @Test
    public void releaseAnswersQueuedCallsThatHaveNotStarted() {
        // R4: posted to a busy main thread, then released before they ran
        scheduler.holdPosts = true;
        List<ConsumeResult> r = new ArrayList<>();
        consumer.consume("a", true, r::add);
        consumer.consume(pack("b", PurchaseState.PENDING), false, r::add);
        assertTrue(r.isEmpty());

        consumer.release();
        assertEquals(2, r.size());
        assertEquals(SERVICE_DISCONNECTED, r.get(0).getResponseCode());
        assertEquals(SERVICE_DISCONNECTED, r.get(1).getResponseCode());

        scheduler.runHeld();
        assertEquals("answered once only", 2, r.size());
        assertEquals("never sent to Play", 0, play.total);
    }

    // --- fakes ---

    private static final class FakePlay implements PurchaseConsumer.Backend {
        final List<Object[]> calls = new ArrayList<>();
        int total;
        ConsumeListener global;

        void answer(String token, int code) {
            for (int i = calls.size() - 1; i >= 0; i--) {
                if (calls.get(i)[0].equals(token)) {
                    AcknowledgeTracker.AckResult r = (AcknowledgeTracker.AckResult) calls.remove(i)[1];
                    r.onResult(code, null);
                    return;
                }
            }
            throw new AssertionError("no call for " + token);
        }

        @Override
        public void consume(@NonNull String purchaseToken, @NonNull AcknowledgeTracker.AckResult result) {
            total++;
            calls.add(new Object[]{purchaseToken, result});
        }

        @Nullable
        @Override
        public ConsumeListener globalListener() {
            return global;
        }

        @Override
        public void log(@NonNull String message) {
        }
    }
}
