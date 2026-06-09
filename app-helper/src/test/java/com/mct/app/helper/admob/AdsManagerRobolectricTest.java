package com.mct.app.helper.admob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.app.Activity;

import com.mct.app.helper.admob.ads.FakeFullScreenAds;
import com.mct.app.helper.admob.listener.AdCallback;
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
 * Robolectric tests for the manager-level full-screen show gate
 * (premium / not-registered / overlap / global back-to-back gap), exercising the
 * exact {@link SkipReason} mapping the demo previously had to guess at.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AdsManagerRobolectricTest {

    private Activity activity;
    private AdsManager manager;

    @Before
    public void setUp() {
        manager = AdsManager.getInstance();
        manager.clear();
        activity = Robolectric.buildActivity(Activity.class).setup().get();
    }

    @After
    public void tearDown() {
        manager.clear();
    }

    @Test
    public void show_unregisteredAlias_skipsNotRegistered() {
        RecordingAdCallback cb = new RecordingAdCallback();
        manager.showFullScreen("NOPE", activity, cb);
        assertEquals(SkipReason.NOT_REGISTERED, cb.skipReason);
    }

    @Test
    public void show_premiumUser_skipsPremium() {
        manager.putAds("A", new FakeFullScreenAds());
        manager.setPremium(true);

        RecordingAdCallback cb = new RecordingAdCallback();
        manager.showFullScreen("A", activity, cb);
        assertEquals(SkipReason.PREMIUM, cb.skipReason);
    }

    @Test
    public void show_whileAnotherFullScreenShowing_skipsOverlap() {
        FakeFullScreenAds a = new FakeFullScreenAds();
        FakeFullScreenAds b = new FakeFullScreenAds();
        manager.putAds("A", a);
        manager.putAds("B", b);

        a.load(activity, null);
        a.completeLoad();
        manager.showFullScreen("A", activity, null);
        assertEquals(AdState.SHOWING, a.getState());

        b.load(activity, null);
        b.completeLoad();
        RecordingAdCallback cb = new RecordingAdCallback();
        manager.showFullScreen("B", activity, cb);
        assertEquals(SkipReason.OVERLAP, cb.skipReason);
    }

    @Test
    public void show_withinGlobalGapAfterAnotherDismissed_skipsGlobalGap() {
        manager.setFullScreenAdsGapMs(60_000);

        FakeFullScreenAds a = new FakeFullScreenAds();
        FakeFullScreenAds b = new FakeFullScreenAds();
        a.setAutoLoadAfterDismiss(false);
        b.setAutoLoadAfterDismiss(false);
        manager.putAds("A", a);
        manager.putAds("B", b);

        // A: show then dismiss -> records a recent full-screen dismiss
        a.load(activity, null);
        a.completeLoad();
        manager.showFullScreen("A", activity, null);
        a.driveShown();
        a.driveDismissed();

        // B is ready but must be blocked by the global back-to-back gap
        b.load(activity, null);
        b.completeLoad();
        RecordingAdCallback cb = new RecordingAdCallback();
        manager.showFullScreen("B", activity, cb);
        assertEquals(SkipReason.GLOBAL_GAP, cb.skipReason);
    }

    @Test
    public void show_differentPlacementNotBlockedWhenGapDisabled() {
        manager.setFullScreenAdsGapMs(0); // disable global gap

        FakeFullScreenAds a = new FakeFullScreenAds();
        FakeFullScreenAds b = new FakeFullScreenAds();
        a.setAutoLoadAfterDismiss(false);
        manager.putAds("A", a);
        manager.putAds("B", b);

        a.load(activity, null);
        a.completeLoad();
        manager.showFullScreen("A", activity, null);
        a.driveShown();
        a.driveDismissed();

        b.load(activity, null);
        b.completeLoad();
        RecordingAdCallback cb = new RecordingAdCallback();
        manager.showFullScreen("B", activity, cb);

        assertNull("no gap -> B may show immediately", cb.skipReason);
        assertEquals(AdState.SHOWING, b.getState());
    }

    @Test
    public void loadIfNeeded_deferredShow_reChecksGlobalGate() {
        // H3 regression: a show that has to load first must re-pass the global gate
        // when the load finishes, not just at request time.
        FakeFullScreenAds a = new FakeFullScreenAds();
        FakeFullScreenAds b = new FakeFullScreenAds();
        manager.putAds("A", a);
        manager.putAds("B", b);

        // request B with LOAD_IF_NEEDED while nothing is showing -> gate passes, B starts loading
        RecordingAdCallback cbB = new RecordingAdCallback();
        manager.showFullScreen("B", b.getAlias(), activity, ShowMode.LOAD_IF_NEEDED, cbB);
        assertEquals(AdState.LOADING, b.getState());

        // meanwhile A starts showing
        a.load(activity, null);
        a.completeLoad();
        manager.showFullScreen("A", activity, null);
        assertEquals(AdState.SHOWING, a.getState());

        // B finishes loading -> deferred show must now be blocked by OVERLAP
        b.completeLoad();
        assertEquals(SkipReason.OVERLAP, cbB.skipReason);
        assertEquals("B must not have shown over A", AdState.READY, b.getState());
    }

    static class RecordingAdCallback implements AdCallback {
        SkipReason skipReason;
        boolean shown;

        @Override
        public void onShown() {
            shown = true;
        }

        @Override
        public void onFailedToShow(ShowError error) {
        }

        @Override
        public void onSkipped(SkipReason reason) {
            skipReason = reason;
        }
    }
}
