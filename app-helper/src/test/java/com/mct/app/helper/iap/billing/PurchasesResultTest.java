package com.mct.app.helper.iap.billing;

import static com.android.billingclient.api.BillingClient.BillingResponseCode.NETWORK_ERROR;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.mct.app.helper.iap.billing.enums.AcknowledgeOutcome;
import com.mct.app.helper.iap.billing.enums.PurchaseState;
import com.mct.app.helper.iap.billing.enums.RefreshReason;
import com.mct.app.helper.iap.billing.enums.SubscriptionStatus;
import com.mct.app.helper.iap.billing.models.OwnedPurchase;
import com.mct.app.helper.iap.billing.models.PurchaseOutcome;
import com.mct.app.helper.iap.billing.models.PurchasesResult;
import com.mct.app.helper.iap.billing.models.RefreshResult;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Plain-JVM tests of the result model using fakes (no Play objects), the way an app would fake it.
 */
public class PurchasesResultTest {

    private static OwnedPurchase sub(String id, PurchaseState state, boolean suspended) {
        return new OwnedPurchase("subs", Collections.singletonList(id), "tok-" + id, null,
                state, false, true, suspended, 1, 0L, false, null);
    }

    @Test
    public void suspendedOrPendingSubscriptionIsNotActive() {
        PurchasesResult r = new PurchasesResult(OK, null, OK, null,
                Arrays.asList(sub("weekly", PurchaseState.PENDING, false), sub("annual", PurchaseState.PURCHASED, true)),
                Collections.emptyList(), true);
        assertTrue(r.isSuccess());
        assertFalse(r.hasActiveSubscription());
        assertEquals(Boolean.FALSE, r.isSubscribed());
        assertEquals(2, r.getSubscriptions().size());
    }

    @Test
    public void suspendedFakeIsNormalisedEitherWay() {
        OwnedPurchase byFlag = sub("annual", PurchaseState.PURCHASED, true);
        assertEquals(PurchaseState.SUSPENDED, byFlag.getState());
        assertTrue(byFlag.isSuspended());
        assertFalse(byFlag.isPurchased());

        OwnedPurchase byState = sub("annual", PurchaseState.SUSPENDED, false);
        assertEquals(PurchaseState.SUSPENDED, byState.getState());
        assertTrue(byState.isSuspended());
        assertFalse(byState.isEntitled());
    }

    @Test
    public void subscriptionStatusOrder() {
        OwnedPurchase active = sub("weekly", PurchaseState.PURCHASED, false);
        OwnedPurchase held = sub("annual", PurchaseState.SUSPENDED, false);
        OwnedPurchase pending = sub("monthly", PurchaseState.PENDING, false);

        assertEquals(SubscriptionStatus.ACTIVE, status(Arrays.asList(held, active, pending)));
        assertEquals(SubscriptionStatus.SUSPENDED, status(Arrays.asList(pending, held)));
        assertEquals(SubscriptionStatus.PENDING, status(Collections.singletonList(pending)));
        assertEquals(SubscriptionStatus.NONE, status(Collections.emptyList()));

        PurchasesResult both = new PurchasesResult(OK, null, OK, null,
                Arrays.asList(held, active), Collections.emptyList(), true);
        assertTrue("two plans: one active, one held", both.hasSuspendedSubscription());
        assertEquals(Boolean.TRUE, both.isSubscribed());
    }

    @Test
    public void subscriptionStatusUnknownWhenPlayFailsOrVerificationRejects() {
        PurchasesResult failed = new PurchasesResult(NETWORK_ERROR, "offline", OK, null,
                Collections.emptyList(), Collections.emptyList(), true);
        assertEquals(SubscriptionStatus.UNKNOWN, failed.getSubscriptionStatus());
        assertFalse(failed.hasSuspendedSubscription());

        PurchasesResult rejected = new PurchasesResult(OK, null, OK, null, Collections.emptyList(),
                Collections.singletonList(sub("annual", PurchaseState.PURCHASED, false)), true);
        assertEquals(SubscriptionStatus.UNKNOWN, rejected.getSubscriptionStatus());

        PurchasesResult heldAndRejected = new PurchasesResult(OK, null, OK, null,
                Collections.singletonList(sub("annual", PurchaseState.SUSPENDED, false)),
                Collections.singletonList(sub("weekly", PurchaseState.PURCHASED, false)), true);
        assertEquals("an accepted hold is known", SubscriptionStatus.SUSPENDED, heldAndRejected.getSubscriptionStatus());
        assertNull(heldAndRejected.isSubscribed());
    }

    private static SubscriptionStatus status(java.util.List<OwnedPurchase> purchases) {
        return new PurchasesResult(OK, null, OK, null, purchases, Collections.emptyList(), true)
                .getSubscriptionStatus();
    }

    @Test
    public void inAppFailureKeepsSubscriptionAnswer() {
        PurchasesResult r = new PurchasesResult(OK, null, NETWORK_ERROR, "offline",
                Collections.singletonList(sub("annual", PurchaseState.PURCHASED, false)),
                Collections.emptyList(), false);
        assertFalse(r.isSuccess());
        assertEquals(NETWORK_ERROR, r.getResponseCode());
        assertEquals("offline", r.getDebugMessage());
        assertEquals(Boolean.TRUE, r.isSubscribed());
    }

    @Test
    public void rejectedSubscriptionMakesStateUnknown() {
        // wrong licence key: the real subscription fails verification
        PurchasesResult r = new PurchasesResult(OK, null, OK, null,
                Collections.emptyList(),
                Collections.singletonList(sub("annual", PurchaseState.PURCHASED, false)), true);
        assertTrue(r.isSuccess());
        assertTrue(r.hasRejectedPurchases());
        assertTrue(r.hasRejectedSubscriptions());
        assertNull("must not read as 'not subscribed' and revoke", r.isSubscribed());
    }

    @Test
    public void refreshResultLooksUpOutcomeByToken() {
        OwnedPurchase annual = sub("annual", PurchaseState.PURCHASED, false);
        PurchasesResult pr = new PurchasesResult(OK, null, OK, null,
                Collections.singletonList(annual), Collections.emptyList(), true);
        PurchaseOutcome outcome = new PurchaseOutcome(annual, AcknowledgeOutcome.ACKNOWLEDGED, OK, null);
        RefreshResult rr = new RefreshResult(pr, Collections.singletonList(outcome), RefreshReason.ITEM_ALREADY_OWNED, "annual");

        assertTrue(rr.isSuccess());
        assertSame(outcome, rr.getOutcome("tok-annual"));
        assertNull(rr.getOutcome("other"));
        assertEquals("annual", rr.getTriggerProductId());
    }
}
