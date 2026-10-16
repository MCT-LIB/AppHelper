package com.mct.app.helper.admob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;

import com.mct.app.helper.admob.ads.BaseAds;
import com.mct.app.helper.admob.ads.FakeFullScreenAds;
import com.mct.app.helper.admob.listener.LoadCallback;
import com.mct.app.helper.admob.listener.LoadError;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ad loads must not be sent before UMP consent allows them.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ConsentGateTest {

    private final AtomicBoolean umpAllows = new AtomicBoolean(false);
    private ConsentGate gate;
    private Activity activity;

    @Before
    public void setUp() {
        gate = ConsentGate.get();
        gate.reset();
        activity = Robolectric.buildActivity(Activity.class).setup().get();
    }

    @After
    public void tearDown() {
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
    public void whileGathering_loadIsParkedThenReplayedWhenAllowed() {
        gate.begin(umpAllows::get);
        FakeFullScreenAds ads = new FakeFullScreenAds();
        RecordingLoad cb = new RecordingLoad();

        ads.load(activity, cb);
        assertNull("no ad request before consent", ads.lastSession);
        assertEquals(1, gate.parkedCount());

        umpAllows.set(true);
        gate.complete();
        assertNotNull("parked load replays once consent allows", ads.lastSession);
        assertEquals(0, gate.parkedCount());
        assertNull(cb.error);
    }

    @Test
    public void consentResolvedWithoutPermission_parkedAndNewLoadsFail() {
        gate.begin(umpAllows::get);
        FakeFullScreenAds ads = new FakeFullScreenAds();
        RecordingLoad parked = new RecordingLoad();
        ads.load(activity, parked);

        gate.complete(); // UMP says no
        assertNull(ads.lastSession);
        assertEquals(LoadError.CODE_CONSENT_NOT_OBTAINED, parked.error.getCode());

        RecordingLoad later = new RecordingLoad();
        ads.load(activity, later);
        assertNull(ads.lastSession);
        assertEquals(LoadError.CODE_CONSENT_NOT_OBTAINED, later.error.getCode());
    }

    @Test
    public void storedConsentFromPreviousLaunch_allowsImmediately() {
        umpAllows.set(true);
        gate.begin(umpAllows::get);
        FakeFullScreenAds ads = new FakeFullScreenAds();
        ads.load(activity, null);
        assertNotNull(ads.lastSession);
    }

    @Test
    public void disablingGate_releasesParkedLoads() {
        gate.begin(umpAllows::get);
        FakeFullScreenAds ads = new FakeFullScreenAds();
        ads.load(activity, null);
        assertNull(ads.lastSession);

        gate.setEnabled(false);
        assertNotNull(ads.lastSession);
    }

    @Test
    public void runWhenResolved_waitsForCompletion() {
        AtomicBoolean ran = new AtomicBoolean(false);
        gate.begin(umpAllows::get);
        gate.runWhenResolved(() -> ran.set(true));
        assertFalse(ran.get());
        gate.complete();
        assertTrue(ran.get());

        AtomicBoolean ranAfter = new AtomicBoolean(false);
        gate.runWhenResolved(() -> ranAfter.set(true));
        assertTrue(ranAfter.get());
    }

    private static final class RecordingLoad implements LoadCallback {
        LoadError error;

        @Override
        public void onAdLoaded(BaseAds<?> ads) {
        }

        @Override
        public void onAdFailedToLoad(BaseAds<?> ads, LoadError error) {
            this.error = error;
        }
    }
}
