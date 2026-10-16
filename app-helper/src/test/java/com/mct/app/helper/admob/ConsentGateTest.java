package com.mct.app.helper.admob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Activity;

import com.mct.app.helper.admob.ads.BaseAds;
import com.mct.app.helper.admob.ads.FakeFullScreenAds;
import com.mct.app.helper.admob.ads.FakeViewAds;
import com.mct.app.helper.admob.listener.LoadCallback;
import com.mct.app.helper.admob.listener.LoadError;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSystemClock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ad loads must not be sent before UMP consent allows them, and an offline first launch must not
 * block ads for the whole process.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ConsentGateTest {

    private final AtomicBoolean umpAllows = new AtomicBoolean(false);
    private final AtomicInteger retries = new AtomicInteger();
    private final ConsentGate.RetryHandler retry = () -> {
        retries.incrementAndGet();
        return true;
    };
    private ConsentGate gate;
    private ActivityController<Activity> controller;
    private Activity activity;

    @Before
    public void setUp() {
        gate = ConsentGate.get();
        gate.setEnabled(true);
        gate.setChangeListener(null);
        gate.reset();
        controller = Robolectric.buildActivity(Activity.class).setup();
        activity = controller.get();
    }

    @After
    public void tearDown() {
        gate.setEnabled(true);
        gate.setChangeListener(null);
        gate.reset();
    }

    @Test
    public void noConsentFlowStarted_loadsPassThrough() {
        assertTrue(gate.allowsLoad());
        FakeFullScreenAds ads = new FakeFullScreenAds();
        ads.load(activity, null);
        assertNotNull("load must reach onLoad when init never started consent", ads.lastSession);
    }

    @Test
    public void whileGathering_loadIsParkedThenReplayedWithApplicationContext() {
        gate.begin(umpAllows::get, retry);
        FakeFullScreenAds ads = new FakeFullScreenAds();
        RecordingLoad cb = new RecordingLoad();

        ads.load(activity, cb);
        assertNull("no ad request before consent", ads.lastSession);
        assertEquals(1, gate.parkedCount());

        umpAllows.set(true);
        gate.complete(false);
        assertNotNull("parked load replays once consent allows", ads.lastSession);
        assertSame("full-screen loads never park the Activity", activity.getApplicationContext(), ads.lastLoadContext);
        assertEquals(0, gate.parkedCount());
        assertNull(cb.error);
    }

    @Test
    public void repeatedLoadsOfSameAd_parkOnceAndAllCallbacksComplete() {
        gate.begin(umpAllows::get, retry);
        FakeFullScreenAds ads = new FakeFullScreenAds();
        RecordingLoad a = new RecordingLoad();
        RecordingLoad b = new RecordingLoad();
        ads.load(activity, a);
        ads.load(activity, b);
        ads.load(activity, null);
        assertEquals(1, gate.parkedCount());

        umpAllows.set(true);
        gate.complete(false);
        ads.completeLoad();
        assertTrue(a.loaded);
        assertTrue(b.loaded);
    }

    @Test
    public void decidedWithoutPermission_parkedAndNewLoadsFail() {
        gate.begin(umpAllows::get, retry);
        FakeFullScreenAds ads = new FakeFullScreenAds();
        RecordingLoad parked = new RecordingLoad();
        ads.load(activity, parked);

        gate.complete(false); // UMP answered without error, but canRequestAds() is false
        assertNull(ads.lastSession);
        assertEquals(LoadError.CODE_CONSENT_NOT_OBTAINED, parked.error.getCode());

        RecordingLoad later = new RecordingLoad();
        ads.load(activity, later);
        assertNull(ads.lastSession);
        assertEquals(LoadError.CODE_CONSENT_NOT_OBTAINED, later.error.getCode());
    }

    @Test
    public void offlineFirstLaunch_errorKeepsLoadsParkedAndRetriesOnConnectivity() {
        gate.begin(umpAllows::get, retry);
        FakeFullScreenAds ads = new FakeFullScreenAds();
        RecordingLoad cb = new RecordingLoad();
        ads.load(activity, cb);

        gate.complete(true); // consent-info update failed, canRequestAds() false
        assertTrue(gate.isFlowFinished());
        assertFalse("an error must not decide 'no ads'", gate.isDecided());
        assertNull(cb.error);
        assertEquals(1, gate.parkedCount());

        gate.onConnectivityAvailable();
        assertEquals("connectivity retries the update", 1, retries.get());

        umpAllows.set(true);
        gate.complete(false); // retry succeeded
        assertNotNull(ads.lastSession);
        assertNull(cb.error);
    }

    @Test
    public void offlineFirstLaunch_newLoadRetriesThrottled() {
        gate.begin(umpAllows::get, retry);
        gate.complete(true);
        ShadowSystemClock.advanceBy(Duration.ofMillis(ConsentGate.RETRY_THROTTLE_MS));

        new FakeFullScreenAds().load(activity, null);
        assertEquals("parking after a failed update asks for a retry", 1, retries.get());

        // retry still in flight: no second retry
        new FakeFullScreenAds().load(activity, null);
        assertEquals(1, retries.get());

        // retry failed again; a new load within the throttle window does not hammer UMP
        gate.complete(true);
        new FakeFullScreenAds().load(activity, null);
        assertEquals(1, retries.get());

        ShadowSystemClock.advanceBy(Duration.ofMillis(ConsentGate.RETRY_THROTTLE_MS));
        new FakeFullScreenAds().load(activity, null);
        assertEquals(2, retries.get());
    }

    @Test
    public void viewAdParkedFromDeadActivity_failsWithContextGone() {
        gate.begin(umpAllows::get, retry);
        FakeViewAds ads = new FakeViewAds();
        RecordingLoad cb = new RecordingLoad();
        ads.load(activity, cb);

        controller.pause().stop().destroy();
        umpAllows.set(true);
        gate.complete(false);

        assertNull("no AdView may be built on a destroyed activity", ads.lastSession);
        assertEquals(LoadError.CODE_CONTEXT_GONE, cb.error.getCode());
    }

    @Test
    public void viewAdParkedFromLiveActivity_replaysOnIt() {
        gate.begin(umpAllows::get, retry);
        FakeViewAds ads = new FakeViewAds();
        ads.load(activity, null);
        umpAllows.set(true);
        gate.complete(false);
        assertSame(activity, ads.lastLoadContext);
    }

    @Test
    public void storedConsentFromPreviousLaunch_allowsImmediately() {
        umpAllows.set(true);
        gate.begin(umpAllows::get, retry);
        FakeFullScreenAds ads = new FakeFullScreenAds();
        ads.load(activity, null);
        assertNotNull(ads.lastSession);
    }

    @Test
    public void disablingGate_releasesParkedLoads() {
        gate.begin(umpAllows::get, retry);
        FakeFullScreenAds ads = new FakeFullScreenAds();
        ads.load(activity, null);
        assertNull(ads.lastSession);

        gate.setEnabled(false);
        assertNotNull(ads.lastSession);
    }

    @Test
    public void managerClear_failsParkedLoadsAndKeepsOnOffChoice() {
        gate.begin(umpAllows::get, retry);
        FakeFullScreenAds ads = new FakeFullScreenAds();
        RecordingLoad cb = new RecordingLoad();
        ads.load(activity, cb);

        AdsManager.getInstance().clear();
        assertEquals("a waiting caller must not hang", LoadError.CODE_CONSENT_NOT_OBTAINED, cb.error.getCode());

        gate.setEnabled(false);
        AdsManager.getInstance().clear();
        assertFalse("clear() keeps the app's choice", gate.isEnabled());
    }

    @Test
    public void privacyFormAllowingAds_notifiesListenerAndReleasesParked() {
        List<Boolean> changes = new ArrayList<>();
        gate.setChangeListener(changes::add);
        gate.begin(umpAllows::get, retry);
        FakeFullScreenAds ads = new FakeFullScreenAds();
        ads.load(activity, null);
        gate.complete(true); // offline, undecided
        assertTrue(changes.isEmpty());

        umpAllows.set(true);
        gate.onConsentMaybeChanged();
        assertEquals(1, changes.size());
        assertTrue(changes.get(0));
        assertNotNull(ads.lastSession);
    }

    @Test
    public void runWhenFlowFinished_runsAfterErrorToo() {
        AtomicBoolean ran = new AtomicBoolean(false);
        gate.begin(umpAllows::get, retry);
        gate.runWhenFlowFinished(() -> ran.set(true));
        assertFalse(ran.get());
        gate.complete(true);
        assertTrue("the form is gone even when the update failed", ran.get());

        AtomicBoolean ranAfter = new AtomicBoolean(false);
        gate.runWhenFlowFinished(() -> ranAfter.set(true));
        assertTrue(ranAfter.get());
    }

    private static final class RecordingLoad implements LoadCallback {
        LoadError error;
        boolean loaded;

        @Override
        public void onAdLoaded(BaseAds<?> ads) {
            loaded = true;
        }

        @Override
        public void onAdFailedToLoad(BaseAds<?> ads, LoadError error) {
            this.error = error;
        }
    }
}
