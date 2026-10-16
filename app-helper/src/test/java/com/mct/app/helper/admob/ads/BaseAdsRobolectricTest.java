package com.mct.app.helper.admob.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;

import com.google.android.gms.ads.FullScreenContentCallback;
import com.mct.app.helper.admob.AdState;
import com.mct.app.helper.admob.AdsManager;
import com.mct.app.helper.admob.listener.AdCallback;
import com.mct.app.helper.admob.listener.LoadCallback;
import com.mct.app.helper.admob.listener.LoadError;
import com.mct.app.helper.admob.listener.ShowError;
import com.mct.app.helper.admob.listener.ShowMode;
import com.mct.app.helper.admob.listener.SkipReason;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Robolectric tests for the {@link BaseAds} state machine. These need a real
 * {@code Looper}/{@code Context} (so {@code assertMainThread} and the GMS callback
 * adapters work), which a plain JVM test cannot provide.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class BaseAdsRobolectricTest {

    private Activity activity;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
    }

    @After
    public void tearDown() {
        AdsManager.getInstance().clear();
    }

    @Test
    public void load_success_movesToReady() {
        FakeFullScreenAds ad = new FakeFullScreenAds();
        RecordingLoadCallback lc = new RecordingLoadCallback();

        ad.load(activity, lc);
        assertEquals(AdState.LOADING, ad.getState());
        assertTrue(ad.isLoading());

        Object loaded = ad.completeLoad();
        assertEquals(AdState.READY, ad.getState());
        assertTrue(ad.isReady());
        assertEquals(loaded, ad.getAds());
        assertEquals(1, lc.loaded);
        assertEquals(0, lc.failed);
    }

    @Test
    public void load_failure_backToIdle() {
        FakeFullScreenAds ad = new FakeFullScreenAds();
        RecordingLoadCallback lc = new RecordingLoadCallback();

        ad.load(activity, lc);
        ad.failLoad();

        assertEquals(AdState.IDLE, ad.getState());
        assertNull(ad.getAds());
        assertEquals(1, lc.failed);
    }

    @Test
    public void load_whileReady_invokesLoadedImmediately() {
        FakeFullScreenAds ad = new FakeFullScreenAds();
        ad.load(activity, null);
        ad.completeLoad();

        RecordingLoadCallback lc = new RecordingLoadCallback();
        ad.load(activity, lc);
        assertEquals(1, lc.loaded); // already ready -> immediate success, no reload
        assertEquals(AdState.READY, ad.getState());
    }

    @Test
    public void clearDuringLoad_releasesLateAd() {
        FakeFullScreenAds ad = new FakeFullScreenAds();
        ad.load(activity, null);

        ad.clear();
        assertEquals(AdState.IDLE, ad.getState());

        Object late = ad.completeLoad(); // stale token -> must be released, not kept
        assertEquals(AdState.IDLE, ad.getState());
        assertNull(ad.getAds());
        assertTrue("late ad must be released to avoid a leak", ad.released.contains(late));
    }

    @Test
    public void clearDuringLoad_notifiesQueuedLoadCallback() {
        FakeFullScreenAds ad = new FakeFullScreenAds();
        RecordingLoadCallback lc = new RecordingLoadCallback();

        ad.load(activity, lc);
        assertEquals(AdState.LOADING, ad.getState());

        ad.clear();

        // clear() must fail the in-flight callback, not silently drop it
        assertEquals(1, lc.failed);
        assertEquals(0, lc.loaded);
        assertEquals(AdState.IDLE, ad.getState());
    }

    @Test
    public void clearDuringLoadIfNeededShow_stillCompletesCallback() {
        // Repro for the "show but never releases the flag" bug: a
        // show(LOAD_IF_NEEDED) starts a load, then clear() (e.g. orientation
        // auto-reload) cancels it mid-flight. onComplete MUST still fire so the
        // caller (e.g. a splash awaiting completion) is never left hanging.
        FakeFullScreenAds ad = new FakeFullScreenAds();
        ad.setAutoLoadAfterDismiss(false);
        RecordingAdCallback cb = new RecordingAdCallback();

        ad.show("alias", activity, ShowMode.LOAD_IF_NEEDED, cb);
        assertEquals(AdState.LOADING, ad.getState());

        ad.clear();

        assertTrue("interrupted show must still complete", cb.completeCalled);
        assertFalse(cb.completeWasShown);
        assertEquals(SkipReason.NOT_LOADED, cb.skipReason);
    }

    @Test
    public void show_whenShowActionThrows_rollsBackToIdle() {
        FakeFullScreenAds ad = readyAd();
        ad.throwOnShow = true;

        RecordingAdCallback cb = new RecordingAdCallback();
        ad.show("alias", activity, ShowMode.IF_READY, cb);

        assertEquals("must not get stuck in SHOWING", AdState.IDLE, ad.getState());
        assertNull(ad.getAds());
        assertTrue(cb.failedToShow);
        assertTrue(cb.completeCalled);
        assertFalse(cb.completeWasShown);
    }

    @Test
    public void show_dismissFlow_firesShownThenDismissed() {
        FakeFullScreenAds ad = readyAd();
        ad.setAutoLoadAfterDismiss(false);

        RecordingAdCallback cb = new RecordingAdCallback();
        ad.show("alias", activity, ShowMode.IF_READY, cb);
        assertEquals(AdState.SHOWING, ad.getState());
        assertTrue(ad.isShowing());

        ad.driveShown();
        assertTrue(cb.shown);

        ad.driveDismissed();
        assertTrue(cb.dismissed);
        assertTrue(cb.completeCalled);
        assertTrue(cb.completeWasShown);
        assertEquals(AdState.IDLE, ad.getState());
        assertTrue("dismiss time recorded for global gap", ad.getLastDismissedAt() > 0);
    }

    @Test
    public void show_failedToShow_reportsFailureNotDismiss() {
        FakeFullScreenAds ad = readyAd();
        ad.setAutoLoadAfterDismiss(false);

        RecordingAdCallback cb = new RecordingAdCallback();
        ad.show("alias", activity, ShowMode.IF_READY, cb);
        ad.driveFailedToShow();

        assertTrue(cb.failedToShow);
        assertFalse(cb.dismissed);
        assertTrue(cb.completeCalled);
        assertFalse(cb.completeWasShown);
        assertEquals(AdState.IDLE, ad.getState());
        assertEquals("fail-to-show is not a dismiss", 0, ad.getLastDismissedAt());
    }

    @Test
    public void show_whenNotLoaded_skipsNotLoaded() {
        FakeFullScreenAds ad = new FakeFullScreenAds(); // IDLE
        RecordingAdCallback cb = new RecordingAdCallback();
        ad.show("alias", activity, ShowMode.IF_READY, cb);
        assertEquals(SkipReason.NOT_LOADED, cb.skipReason);
        assertTrue(cb.completeCalled);
    }

    @Test
    public void show_loadIfNeeded_loadsThenShows() {
        FakeFullScreenAds ad = new FakeFullScreenAds();
        ad.setAutoLoadAfterDismiss(false);
        RecordingAdCallback cb = new RecordingAdCallback();

        ad.show("alias", activity, ShowMode.LOAD_IF_NEEDED, cb);
        assertEquals("should start loading", AdState.LOADING, ad.getState());

        ad.completeLoad();             // load done -> continuation shows
        assertEquals(AdState.SHOWING, ad.getState());

        ad.driveShown();
        ad.driveDismissed();
        assertTrue(cb.shown);
        assertTrue(cb.dismissed);
    }

    @Test
    public void show_withinOwnInterval_afterDismiss_skipsCooldown() {
        // placement with a 45s interval that enforces it
        FakeFullScreenAds ad = new FakeFullScreenAds(45_000, true);
        ad.setAutoLoadAfterDismiss(false);

        ad.load(activity, null);
        ad.completeLoad();
        ad.show("alias", activity, ShowMode.IF_READY, null);
        ad.driveShown();
        ad.driveDismissed(); // lastDismissedAt = now

        // reload a fresh ad, but we are still inside the 45s interval
        ad.load(activity, null);
        ad.completeLoad();
        assertEquals(AdState.READY, ad.getState());

        RecordingAdCallback cb = new RecordingAdCallback();
        ad.show("alias", activity, ShowMode.IF_READY, cb);
        assertEquals(SkipReason.COOLDOWN, cb.skipReason);
    }

    @Test
    public void clear_afterDismiss_keepsOwnIntervalAcrossReload() {
        // I2: dropping the loaded ad (renderer loss, reconnect reload) must not
        // forget when the last impression ended.
        FakeFullScreenAds ad = new FakeFullScreenAds(45_000, true);
        ad.setAutoLoadAfterDismiss(false);
        ad.load(activity, null);
        ad.completeLoad();
        ad.show("alias", activity, ShowMode.IF_READY, null);
        ad.driveShown();
        ad.driveDismissed();
        long dismissedAt = ad.getLastDismissedAt();
        assertTrue(dismissedAt > 0);

        ad.load(activity, null);
        ad.completeLoad();
        ad.clear();                       // drop the loaded replacement

        assertEquals(AdState.IDLE, ad.getState());
        assertNull(ad.getAds());
        assertEquals("load state reset", 0, ad.getAdsLoadedTime());
        assertEquals("frequency timing kept", dismissedAt, ad.getLastDismissedAt());

        ad.load(activity, null);
        ad.completeLoad();
        RecordingAdCallback cb = new RecordingAdCallback();
        ad.show("alias", activity, ShowMode.IF_READY, cb);
        assertEquals(SkipReason.COOLDOWN, cb.skipReason);
        assertEquals(AdState.READY, ad.getState());
    }

    @Test
    public void clear_neverShown_leavesNoTiming() {
        FakeFullScreenAds ad = readyAd();
        ad.clear();
        assertEquals(0, ad.getLastDismissedAt());
        assertFalse(ad.isDismissNearly());
    }

    @Test
    public void dismiss_withAutoLoad_reloadsImmediately() {
        FakeFullScreenAds ad = readyAd(); // autoLoadAfterDismiss defaults to true
        ad.show("alias", activity, ShowMode.IF_READY, new RecordingAdCallback());
        ad.driveShown();
        ad.driveDismissed();
        assertEquals("auto-load should kick off a new load after dismiss", AdState.LOADING, ad.getState());
    }

    @Test
    public void staleDismiss_afterClearAndReload_mustNotReleaseReplacementAd() {
        // Repro: clear() while the ad is ON SCREEN (manual clearAds, or the
        // orientation auto-reload), then a replacement loads. When the user finally
        // closes the on-screen ad, its dismiss event is STALE - it must not destroy
        // the freshly loaded replacement or reset its state.
        FakeFullScreenAds ad = readyAd();
        ad.setAutoLoadAfterDismiss(false);
        ad.show("alias", activity, ShowMode.IF_READY, new RecordingAdCallback());
        ad.driveShown();
        assertEquals(AdState.SHOWING, ad.getState());

        ad.clear();                       // mid-show clear
        ad.load(activity, null);
        Object replacement = ad.completeLoad();
        assertEquals(AdState.READY, ad.getState());

        ad.driveDismissed();              // stale terminal event from the OLD show

        assertEquals("replacement ad must survive a stale dismiss", AdState.READY, ad.getState());
        assertEquals(replacement, ad.getAds());
        assertFalse("replacement must not be released", ad.released.contains(replacement));
        assertTrue("an impression still ended on screen -> global gap stays armed",
                ad.getLastDismissedAt() > 0);
    }

    @Test
    public void staleDismiss_afterClearReloadAndReshow_mustNotKillTheNewShow() {
        // Hardest variant: clear() mid-show, reload AND re-show, so the state is
        // SHOWING again when the OLD impression's dismiss event finally arrives.
        // The state guard alone cannot tell the two impressions apart - the
        // identity guard (shownAd) must.
        FakeFullScreenAds ad = readyAd();
        ad.setAutoLoadAfterDismiss(false);
        ad.show("alias", activity, ShowMode.IF_READY, new RecordingAdCallback());
        FullScreenContentCallback oldImpl = ad.lastContentCallback; // impression #1

        ad.clear();                       // mid-show clear
        ad.load(activity, null);
        Object replacement = ad.completeLoad();
        ad.show("alias", activity, ShowMode.IF_READY, new RecordingAdCallback()); // impression #2
        assertEquals(AdState.SHOWING, ad.getState());

        oldImpl.onAdDismissedFullScreenContent(); // stale terminal event of #1

        assertEquals("the new impression must keep SHOWING", AdState.SHOWING, ad.getState());
        assertEquals(replacement, ad.getAds());
        assertFalse("replacement must not be released", ad.released.contains(replacement));

        // and the new impression still terminates normally
        ad.driveDismissed();
        assertEquals(AdState.IDLE, ad.getState());
    }

    @Test
    public void staleFailedToShow_afterClearAndReload_mustNotReleaseReplacementAd() {
        FakeFullScreenAds ad = readyAd();
        ad.setAutoLoadAfterDismiss(false);
        ad.show("alias", activity, ShowMode.IF_READY, new RecordingAdCallback());

        ad.clear();
        ad.load(activity, null);
        Object replacement = ad.completeLoad();

        ad.driveFailedToShow();           // stale terminal event from the OLD show

        assertEquals(AdState.READY, ad.getState());
        assertEquals(replacement, ad.getAds());
        assertFalse(ad.released.contains(replacement));
    }

    @Test
    public void watchdog_showingPastMaxDuration_reportsStale() {
        FakeFullScreenAds ad = readyAd();
        ad.setAutoLoadAfterDismiss(false);
        ad.show("alias", activity, ShowMode.IF_READY, null);
        assertEquals(AdState.SHOWING, ad.getState());
        assertFalse("fresh show is not stale", ad.isShowingStale());

        ad.maxShowDurationOverride = 1L;  // 1ms threshold
        awaitMillis(5);
        assertTrue("SHOWING past max duration must be reported stale", ad.isShowingStale());

        ad.clear();                       // what the manager watchdog does
        assertEquals(AdState.IDLE, ad.getState());
        assertFalse(ad.isShowing());
    }

    private static void awaitMillis(long ms) {
        long until = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < until) {
            Thread.yield();
        }
    }

    private FakeFullScreenAds readyAd() {
        FakeFullScreenAds ad = new FakeFullScreenAds();
        ad.load(activity, null);
        ad.completeLoad();
        return ad;
    }

    static class RecordingLoadCallback implements LoadCallback {
        int loaded;
        int failed;

        @Override
        public void onAdLoaded(BaseAds<?> ad) {
            loaded++;
        }

        @Override
        public void onAdFailedToLoad(BaseAds<?> ad, LoadError error) {
            failed++;
        }
    }

    static class RecordingAdCallback implements AdCallback {
        boolean shown;
        boolean dismissed;
        boolean failedToShow;
        boolean completeCalled;
        boolean completeWasShown;
        SkipReason skipReason;

        @Override
        public void onShown() {
            shown = true;
        }

        @Override
        public void onDismissed() {
            dismissed = true;
        }

        @Override
        public void onFailedToShow(ShowError error) {
            failedToShow = true;
        }

        @Override
        public void onSkipped(SkipReason reason) {
            skipReason = reason;
        }

        @Override
        public void onComplete(boolean wasShown) {
            completeCalled = true;
            completeWasShown = wasShown;
        }
    }
}
