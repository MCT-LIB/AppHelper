package com.mct.app.helper.iap.billing;

import static com.android.billingclient.api.BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.mct.app.helper.iap.billing.enums.AcknowledgeOutcome;
import com.mct.app.helper.iap.billing.enums.PurchaseState;
import com.mct.app.helper.iap.billing.enums.SubscriptionStatus;
import com.mct.app.helper.iap.billing.models.OwnedPurchase;
import com.mct.app.helper.iap.billing.models.PurchasesResult;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Mapping of raw Play query answers to {@link PurchasesResult}. Robolectric only for org.json,
 * which {@link Purchase} parses.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PurchasesMapperTest {

    // JSON purchaseState: 0 = purchased, 4 = pending
    private static Purchase purchase(String productId, String token, int jsonState, boolean ack) throws JSONException {
        return purchase(productId, token, jsonState, ack, false);
    }

    // "suspended" is the JSON key Purchase.isSuspended() reads (billing 9.0.0)
    private static Purchase purchase(String productId, String token, int jsonState, boolean ack,
                                     boolean suspended) throws JSONException {
        String json = "{\"orderId\":\"" + (jsonState == 0 ? "GPA.1-" + token : "") + "\","
                + "\"packageName\":\"com.example\",\"productId\":\"" + productId + "\","
                + "\"purchaseTime\":1700000000000,\"purchaseState\":" + jsonState + ","
                + "\"purchaseToken\":\"" + token + "\",\"quantity\":1,"
                + "\"acknowledged\":" + ack + ",\"autoRenewing\":" + !suspended
                + ",\"suspended\":" + suspended + "}";
        return new Purchase(json, "sig-" + token);
    }

    private static PurchasesMapper.TypedQuery ok(Purchase... purchases) {
        return new PurchasesMapper.TypedQuery(OK, "", Arrays.asList(purchases));
    }

    private static PurchasesMapper.TypedQuery failed(int code) {
        return new PurchasesMapper.TypedQuery(code, "boom", null);
    }

    @Test
    public void bothOk_mapsStateAckTokenOrderAndType() throws Exception {
        PurchasesResult r = PurchasesMapper.map(
                ok(purchase("pro_annual", "t1", 0, false), purchase("pro_weekly", "t2", 4, false)),
                ok(purchase("credits_10", "t3", 0, false)),
                null);

        assertTrue(r.isSuccess());
        assertEquals(OK, r.getResponseCode());
        assertEquals(3, r.getPurchases().size());

        OwnedPurchase annual = r.find("pro_annual");
        assertEquals(PurchaseState.PURCHASED, annual.getState());
        assertTrue(annual.isSubscription());
        assertFalse(annual.isAcknowledged());
        assertEquals("t1", annual.getPurchaseToken());
        assertEquals("GPA.1-t1", annual.getOrderId());
        assertEquals(Collections.singletonList("pro_annual"), annual.getProducts());

        OwnedPurchase weekly = r.find("pro_weekly");
        assertEquals(PurchaseState.PENDING, weekly.getState());
        assertNull("pending purchases have no order id", weekly.getOrderId());
        assertFalse(weekly.isEntitled());

        assertFalse(r.find("credits_10").isSubscription());
        assertEquals(1, r.getActiveSubscriptions().size());
        assertEquals(Boolean.TRUE, r.isSubscribed());
    }

    @Test
    public void subsQueryFailed_isUnknownNotEmpty() throws Exception {
        PurchasesResult r = PurchasesMapper.map(failed(SERVICE_UNAVAILABLE),
                ok(purchase("credits_10", "t3", 0, false)), null);

        assertFalse(r.isSuccess());
        assertFalse(r.isSubscriptionsQueryOk());
        assertTrue(r.isInAppQueryOk());
        assertEquals(SERVICE_UNAVAILABLE, r.getResponseCode());
        assertNull("failed subs query must not read as 'not subscribed'", r.isSubscribed());
        assertEquals(1, r.getInAppPurchases().size());
    }

    @Test
    public void bothOkAndEmpty_isAuthoritativeNotSubscribed() {
        PurchasesResult r = PurchasesMapper.map(ok(), ok(), null);
        assertTrue(r.isSuccess());
        assertEquals(Boolean.FALSE, r.isSubscribed());
    }

    @Test
    public void subscriptionsUnsupported_isFailure() {
        PurchasesResult r = PurchasesMapper.map(failed(FEATURE_NOT_SUPPORTED), ok(), null);
        assertFalse(r.isSuccess());
        assertEquals(FEATURE_NOT_SUPPORTED, r.getResponseCode());
    }

    @Test
    public void noKey_acceptsAllButReportsUnverified() throws Exception {
        PurchasesResult r = PurchasesMapper.map(ok(purchase("pro_annual", "t1", 0, true)), ok(), null);
        assertFalse(r.isSignatureVerified());
        assertFalse(r.getPurchases().get(0).isSignatureVerified());
        assertTrue(r.getRejected().isEmpty());
    }

    @Test
    public void withVerifier_splitsAcceptedAndRejected() throws Exception {
        Purchase good = purchase("pro_annual", "good", 0, true);
        Purchase forged = purchase("pro_weekly", "forged", 0, true);
        PurchasesResult r = PurchasesMapper.map(ok(good, forged), ok(),
                p -> p.getPurchaseToken().equals("good"));

        assertTrue(r.isSignatureVerified());
        List<OwnedPurchase> accepted = r.getPurchases();
        assertEquals(1, accepted.size());
        assertTrue(accepted.get(0).isSignatureVerified());
        assertEquals(1, r.getRejected().size());
        assertEquals("forged", r.getRejected().get(0).getPurchaseToken());
    }

    @Test
    public void acknowledgeDecision() throws Exception {
        PurchasesResult r = PurchasesMapper.map(
                ok(purchase("a", "unacked", 0, false), purchase("b", "acked", 0, true), purchase("c", "pending", 4, false)),
                ok(), null);

        assertNull("unacknowledged PURCHASED sub must be acknowledged",
                PurchasesMapper.acknowledgeDecision(r.find("a"), true));
        assertEquals(AcknowledgeOutcome.ALREADY_ACKNOWLEDGED, PurchasesMapper.acknowledgeDecision(r.find("b"), true));
        assertEquals(AcknowledgeOutcome.PENDING, PurchasesMapper.acknowledgeDecision(r.find("c"), true));
        assertEquals("consumables are left to the app",
                AcknowledgeOutcome.NOT_REQUIRED, PurchasesMapper.acknowledgeDecision(r.find("a"), false));
    }

    @Test
    public void suspendedSubscription_mapsToSuspendedNotEntitledAndOwned() throws Exception {
        PurchasesResult r = PurchasesMapper.map(ok(purchase("pro_annual", "held", 0, true, true)), ok(), null);

        OwnedPurchase held = r.find("pro_annual");
        assertEquals(PurchaseState.SUSPENDED, held.getState());
        assertTrue(held.isSuspended());
        assertFalse(held.isPurchased());
        assertFalse(held.isEntitled());
        assertTrue(r.getActiveSubscriptions().isEmpty());
        assertEquals(Collections.singletonList(held), r.getSuspendedSubscriptions());
        assertTrue(r.hasSuspendedSubscription());
        assertEquals("on hold grants nothing", Boolean.FALSE, r.isSubscribed());
        assertEquals(SubscriptionStatus.SUSPENDED, r.getSubscriptionStatus());
    }

    @Test
    public void suspendedSubscription_isNeverAcknowledged() throws Exception {
        PurchasesResult r = PurchasesMapper.map(ok(purchase("pro_annual", "held", 0, false, true)), ok(), null);
        assertEquals(AcknowledgeOutcome.NOT_REQUIRED, PurchasesMapper.acknowledgeDecision(r.find("pro_annual"), true));
    }

    @Test
    public void suspendedSubscriptionRejectedBySignature_isUnknown() throws Exception {
        PurchasesResult r = PurchasesMapper.map(ok(purchase("pro_annual", "held", 0, true, true)), ok(), p -> false);
        assertFalse(r.hasSuspendedSubscription());
        assertNull(r.isSubscribed());
        assertEquals(SubscriptionStatus.UNKNOWN, r.getSubscriptionStatus());
    }

    @Test
    public void subscriptionsQueryAsksForSuspended_inAppDoesNot() {
        assertTrue(QueryHelper.typedParams(BillingClient.ProductType.SUBS).getIncludeSuspendedSubscriptions());
        assertFalse(QueryHelper.typedParams(BillingClient.ProductType.INAPP).getIncludeSuspendedSubscriptions());
    }
}
