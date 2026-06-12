package com.mct.app.helper.admob.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Application;

import com.mct.app.helper.admob.AdsManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Robolectric tests for {@link NativeFullScreenAds}'s activity guard — the only
 * mechanism that tears the native dialog down (and so releases the {@code SHOWING}
 * state) when the host activity is destroyed.
 * <p>
 * These drive the real guard methods against a shadow {@link Activity}/
 * {@link Application}. They do NOT render the dialog (that needs a real GMS
 * {@code NativeAd} + {@code NativeAdView}, which is not faithfully reproducible
 * under Robolectric); instead they assert the guard's registration/teardown
 * contract directly, which is where the leak / stuck-SHOWING bugs lived.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class NativeFullScreenAdsRobolectricTest {

    private ActivityController<Activity> controller;
    private Activity activity;
    private Application app;
    private int baseline;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(Activity.class).setup();
        activity = controller.get();
        app = activity.getApplication();
        baseline = registeredCount();
    }

    @After
    public void tearDown() {
        AdsManager.getInstance().clear();
    }

    /**
     * Number of {@link Application.ActivityLifecycleCallbacks} currently registered
     * on the real {@link Application} — the actual store the guard registers into,
     * so this detects a leaked callback directly.
     */
    private int registeredCount() {
        try {
            Field f = Application.class.getDeclaredField("mActivityLifecycleCallbacks");
            f.setAccessible(true);
            Object list = f.get(app);
            return list == null ? 0 : ((List<?>) list).size();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void registerActivityGuard_registersOneCallback_andReturnsTrue() {
        NativeFullScreenAds ad = new NativeFullScreenAds("ca-app-test", 0);

        assertTrue("guard must register when the activity has an application",
                ad.registerActivityGuard(activity));
        assertEquals(baseline + 1, registeredCount());
    }

    @Test
    public void registerActivityGuard_isIdempotent_noLeakOnReRegister() {
        NativeFullScreenAds ad = new NativeFullScreenAds("ca-app-test", 0);

        ad.registerActivityGuard(activity);
        ad.registerActivityGuard(activity); // must unregister the previous one first

        assertEquals("re-register must not leak a second callback", baseline + 1, registeredCount());
    }

    @Test
    public void activityDestroyed_withoutDialog_unregistersGuard() {
        NativeFullScreenAds ad = new NativeFullScreenAds("ca-app-test", 0);
        ad.registerActivityGuard(activity);
        assertEquals(baseline + 1, registeredCount());

        // No dialog is up, so the guard takes the "else" branch and unregisters
        // itself rather than getting stuck.
        controller.destroy();

        assertEquals("guard must unregister itself on host destroy", baseline, registeredCount());
    }

    @Test
    public void onRelease_unregistersGuard() {
        // Repro for the leak fix: if show() throws after the guard is registered,
        // the rollback path runs onRelease() (via clear()/releaseAd) — which must
        // tear the guard down rather than leak it.
        NativeFullScreenAds ad = new NativeFullScreenAds("ca-app-test", 0);
        ad.registerActivityGuard(activity);
        assertEquals(baseline + 1, registeredCount());

        ad.clear(); // -> releaseAd -> onRelease -> unregisterActivityGuard

        assertEquals("onRelease must unregister the guard", baseline, registeredCount());
    }

    @Test
    public void unregisterActivityGuard_isSafeWhenNothingRegistered() {
        NativeFullScreenAds ad = new NativeFullScreenAds("ca-app-test", 0);

        ad.unregisterActivityGuard(); // no-op, must not throw

        assertEquals(baseline, registeredCount());
    }
}
