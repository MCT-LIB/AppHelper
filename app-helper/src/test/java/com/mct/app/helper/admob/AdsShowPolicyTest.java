package com.mct.app.helper.admob;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link AdsShowPolicy} — the pure full-screen frequency-cap logic.
 * <p>
 * These run on a plain JVM (no Android / GMS), so they exercise the exact policy
 * that {@code AdsManager.checkShowFullScreenAdsCondition} delegates to.
 */
public class AdsShowPolicyTest {

    private static final long NOW = 1_000_000_000_000L; // arbitrary fixed "now"
    private static final long GAP = AdsManager.DEFAULT_FULL_SCREEN_ADS_GAP_MS; // 15s
    private static final long OVERDUE = 4 * 60 * 60 * 1000L; // 4h
    private static final long INTERVAL = 45 * 1000L; // 45s (interstitial-like)

    ///////////////////////////////////////////////////////////////////////////
    // isWithinGlobalGap
    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void withinGap_noPreviousAd_isFalse() {
        // lastDismiss == 0 means no full-screen ad has ever been dismissed
        assertFalse(AdsShowPolicy.isWithinGlobalGap(NOW, 0L, GAP));
    }

    @Test
    public void withinGap_justDismissed_isTrue() {
        long lastDismiss = NOW - 1; // dismissed 1ms ago
        assertTrue(AdsShowPolicy.isWithinGlobalGap(NOW, lastDismiss, GAP));
    }

    @Test
    public void withinGap_halfwayThroughGap_isTrue() {
        long lastDismiss = NOW - GAP / 2;
        assertTrue(AdsShowPolicy.isWithinGlobalGap(NOW, lastDismiss, GAP));
    }

    @Test
    public void withinGap_exactlyAtGapBoundary_isFalse() {
        // boundary is exclusive: now - last == gap is NOT "within"
        long lastDismiss = NOW - GAP;
        assertFalse(AdsShowPolicy.isWithinGlobalGap(NOW, lastDismiss, GAP));
    }

    @Test
    public void withinGap_pastGap_isFalse() {
        long lastDismiss = NOW - GAP - 1;
        assertFalse(AdsShowPolicy.isWithinGlobalGap(NOW, lastDismiss, GAP));
    }

    @Test
    public void withinGap_gapDisabled_isFalse() {
        long lastDismiss = NOW - 1; // even if dismissed just now
        assertFalse(AdsShowPolicy.isWithinGlobalGap(NOW, lastDismiss, 0L));
        assertFalse(AdsShowPolicy.isWithinGlobalGap(NOW, lastDismiss, -100L));
    }

    ///////////////////////////////////////////////////////////////////////////
    // isShowingStale (stuck-SHOWING watchdog)
    ///////////////////////////////////////////////////////////////////////////

    private static final long MAX_SHOW = 10 * 60 * 1000L; // 10 min

    @Test
    public void showingStale_neverShown_isFalse() {
        assertFalse(AdsShowPolicy.isShowingStale(0L, NOW, MAX_SHOW));
    }

    @Test
    public void showingStale_withinMaxDuration_isFalse() {
        assertFalse(AdsShowPolicy.isShowingStale(NOW - 1_000, NOW, MAX_SHOW));
        // boundary is exclusive: exactly max duration is not yet stale
        assertFalse(AdsShowPolicy.isShowingStale(NOW - MAX_SHOW, NOW, MAX_SHOW));
    }

    @Test
    public void showingStale_beyondMaxDuration_isTrue() {
        assertTrue(AdsShowPolicy.isShowingStale(NOW - MAX_SHOW - 1, NOW, MAX_SHOW));
    }

    @Test
    public void showingStale_watchdogDisabled_isFalse() {
        assertFalse(AdsShowPolicy.isShowingStale(NOW - MAX_SHOW * 10, NOW, 0L));
        assertFalse(AdsShowPolicy.isShowingStale(NOW - MAX_SHOW * 10, NOW, -1L));
    }

    ///////////////////////////////////////////////////////////////////////////
    // canShowFullScreenNow
    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void canShow_nothingShowingNoRecentDismiss_isTrue() {
        assertTrue(AdsShowPolicy.canShowFullScreenNow(false, NOW, 0L, GAP));
    }

    @Test
    public void canShow_anotherFullScreenShowing_isFalse() {
        // overlap guard wins even when no recent dismiss
        assertFalse(AdsShowPolicy.canShowFullScreenNow(true, NOW, 0L, GAP));
    }

    @Test
    public void canShow_withinGapAfterDismiss_isFalse() {
        long lastDismiss = NOW - 1_000; // 1s ago, inside 15s gap
        assertFalse(AdsShowPolicy.canShowFullScreenNow(false, NOW, lastDismiss, GAP));
    }

    @Test
    public void canShow_afterGapElapsed_isTrue() {
        long lastDismiss = NOW - GAP; // gap fully elapsed
        assertTrue(AdsShowPolicy.canShowFullScreenNow(false, NOW, lastDismiss, GAP));
    }

    @Test
    public void canShow_overlapTakesPriorityOverElapsedGap() {
        long lastDismiss = NOW - GAP - 5_000; // gap long gone...
        assertFalse(AdsShowPolicy.canShowFullScreenNow(true, NOW, lastDismiss, GAP)); // ...but still showing
    }

    ///////////////////////////////////////////////////////////////////////////
    // Regression: different placements must NOT lock each other for a full interval
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Reproduces the original "low show rate" bug scenario.
     * <p>
     * App-open ad (interval 60s) is dismissed; an interstitial (interval 45s) is
     * a DIFFERENT placement. Under the old cross-cooldown it was blocked for the
     * app-open's full interval. With the global gap it must be allowed again as
     * soon as the short back-to-back gap elapses — well before 45/60s.
     */
    @Test
    public void differentPlacement_unblockedAfterGap_notAfterFullInterval() {
        long appOpenDismissTime = NOW;

        // 5s later: still inside the 15s gap -> interstitial blocked (no back-to-back)
        long t5s = NOW + 5_000;
        assertFalse(AdsShowPolicy.canShowFullScreenNow(false, t5s, appOpenDismissTime, GAP));

        // 15s later: gap elapsed -> interstitial allowed, even though neither the
        // 45s nor the 60s placement interval has passed.
        long t15s = NOW + GAP;
        assertTrue(AdsShowPolicy.canShowFullScreenNow(false, t15s, appOpenDismissTime, GAP));

        // sanity: 15s is far less than the smallest placement interval (45s)
        assertTrue("global gap must be much shorter than placement intervals", GAP < 45_000);
    }

    ///////////////////////////////////////////////////////////////////////////
    // isExpired
    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void expired_neverLoaded_isFalse() {
        assertFalse(AdsShowPolicy.isExpired(0L, NOW, OVERDUE));
    }

    @Test
    public void expired_freshlyLoaded_isFalse() {
        assertFalse(AdsShowPolicy.isExpired(NOW - 1_000, NOW, OVERDUE));
    }

    @Test
    public void expired_pastOverdue_isTrue() {
        assertTrue(AdsShowPolicy.isExpired(NOW - OVERDUE - 1, NOW, OVERDUE));
    }

    @Test
    public void expired_exactlyAtOverdue_isFalse() {
        // boundary exclusive: now - loadedAt == overdue is NOT expired
        assertFalse(AdsShowPolicy.isExpired(NOW - OVERDUE, NOW, OVERDUE));
    }

    ///////////////////////////////////////////////////////////////////////////
    // isWithinInterval
    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void interval_noImpressionYet_isFalse() {
        assertFalse(AdsShowPolicy.isWithinInterval(0L, NOW, INTERVAL));
    }

    @Test
    public void interval_zeroInterval_isFalse() {
        // view ads pass interval 0 -> never throttled
        assertFalse(AdsShowPolicy.isWithinInterval(NOW - 1, NOW, 0L));
    }

    @Test
    public void interval_withinWindow_isTrue() {
        assertTrue(AdsShowPolicy.isWithinInterval(NOW - INTERVAL / 2, NOW, INTERVAL));
    }

    @Test
    public void interval_pastWindow_isFalse() {
        assertFalse(AdsShowPolicy.isWithinInterval(NOW - INTERVAL - 1, NOW, INTERVAL));
    }

    ///////////////////////////////////////////////////////////////////////////
    // canLoad
    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void canLoad_idle_isTrue() {
        assertTrue(AdsShowPolicy.canLoad(AdState.IDLE, 0L, NOW, false, OVERDUE));
    }

    @Test
    public void canLoad_idleButPremium_isFalse() {
        assertFalse(AdsShowPolicy.canLoad(AdState.IDLE, 0L, NOW, true, OVERDUE));
    }

    @Test
    public void canLoad_loading_isFalse() {
        assertFalse(AdsShowPolicy.canLoad(AdState.LOADING, 0L, NOW, false, OVERDUE));
    }

    @Test
    public void canLoad_showing_isFalse() {
        assertFalse(AdsShowPolicy.canLoad(AdState.SHOWING, NOW, NOW, false, OVERDUE));
    }

    @Test
    public void canLoad_readyAndFresh_isFalse() {
        // already have a valid ad -> no reload
        assertFalse(AdsShowPolicy.canLoad(AdState.READY, NOW - 1_000, NOW, false, OVERDUE));
    }

    @Test
    public void canLoad_readyButExpired_isTrue() {
        // cached ad went stale -> reload allowed
        assertTrue(AdsShowPolicy.canLoad(AdState.READY, NOW - OVERDUE - 1, NOW, false, OVERDUE));
    }

    ///////////////////////////////////////////////////////////////////////////
    // canShow
    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void canShow_readyFreshNoInterval_isTrue() {
        assertTrue(AdsShowPolicy.canShow(AdState.READY, NOW - 1_000, 0L, NOW, false, INTERVAL, OVERDUE));
    }

    @Test
    public void canShow_notReady_isFalse() {
        assertFalse(AdsShowPolicy.canShow(AdState.IDLE, 0L, 0L, NOW, false, INTERVAL, OVERDUE));
        assertFalse(AdsShowPolicy.canShow(AdState.LOADING, 0L, 0L, NOW, false, INTERVAL, OVERDUE));
        assertFalse(AdsShowPolicy.canShow(AdState.SHOWING, NOW, NOW, NOW, false, INTERVAL, OVERDUE));
    }

    @Test
    public void canShow_premium_isFalse() {
        assertFalse(AdsShowPolicy.canShow(AdState.READY, NOW - 1_000, 0L, NOW, true, INTERVAL, OVERDUE));
    }

    @Test
    public void canShow_expired_isFalse() {
        assertFalse(AdsShowPolicy.canShow(AdState.READY, NOW - OVERDUE - 1, 0L, NOW, false, INTERVAL, OVERDUE));
    }

    @Test
    public void canShow_withinOwnInterval_isFalse() {
        long lastImpression = NOW - 1_000; // shown 1s ago, inside 45s interval
        assertFalse(AdsShowPolicy.canShow(AdState.READY, NOW - 2_000, lastImpression, NOW, false, INTERVAL, OVERDUE));
    }

    @Test
    public void canShow_afterOwnIntervalElapsed_isTrue() {
        long lastImpression = NOW - INTERVAL; // interval fully elapsed
        assertTrue(AdsShowPolicy.canShow(AdState.READY, NOW - INTERVAL - 1_000, lastImpression, NOW, false, INTERVAL, OVERDUE));
    }

    @Test
    public void canShow_viewAdZeroInterval_notThrottled() {
        // view ads: interval 0 -> only readiness matters, immediate re-show ok
        long lastImpression = NOW - 1; // shown 1ms ago
        assertTrue(AdsShowPolicy.canShow(AdState.READY, NOW - 1_000, lastImpression, NOW, false, 0L, OVERDUE));
    }
}
