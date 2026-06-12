package com.mct.app.helper.admob.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.view.View;
import android.widget.FrameLayout;

import com.mct.app.helper.admob.AdState;
import com.mct.app.helper.admob.AdsManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Robolectric tests for {@link BaseViewAds} (banner/native) lifecycle: attach on
 * show, detach on hide, and — crucially — {@code onDestroyView} on clear, which is
 * the contract behind the v1 banner {@code AdView.destroy()} leak fix.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class BaseViewAdsRobolectricTest {

    private Activity activity;
    private FrameLayout container;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        container = new FrameLayout(activity);
    }

    @After
    public void tearDown() {
        AdsManager.getInstance().clear();
    }

    @Test
    public void show_loadsThenAttachesToContainer() {
        FakeViewAds ad = new FakeViewAds();
        ad.show("alias", container);
        assertEquals(AdState.LOADING, ad.getState());

        View view = ad.completeLoad(activity);
        assertEquals(AdState.READY, ad.getState());
        assertEquals(1, container.getChildCount());
        assertSame(view, container.getChildAt(0));
        assertTrue(ad.isShowing()); // "showing" == attached for view ads
    }

    @Test
    public void hide_detachesButKeepsAd() {
        FakeViewAds ad = new FakeViewAds();
        ad.show("alias", container);
        ad.completeLoad(activity);

        ad.hide();
        assertEquals(0, container.getChildCount());
        assertFalse(ad.isShowing());
        assertEquals("ad kept (re-showable) after hide", AdState.READY, ad.getState());
        assertEquals("hide must not destroy the ad", 0, ad.destroyed);
    }

    @Test
    public void clear_destroysAndDetaches() {
        FakeViewAds ad = new FakeViewAds();
        ad.show("alias", container);
        ad.completeLoad(activity);

        ad.clear();
        assertEquals(AdState.IDLE, ad.getState());
        assertNull(ad.getAds());
        assertEquals(0, container.getChildCount());
        assertEquals("clear must release the view ad's native resources", 1, ad.destroyed);
    }

    @Test
    public void forceShowReload_clearsThenReloads() {
        FakeViewAds ad = new FakeViewAds();
        ad.show("alias", container);
        ad.completeLoad(activity);

        ad.forceShow("alias", container, true); // reload
        assertEquals("reload destroys the old ad", 1, ad.destroyed);
        assertEquals("reload kicks off a fresh load", AdState.LOADING, ad.getState());

        ad.completeLoad(activity);
        assertEquals(1, container.getChildCount());
    }

    @Test
    public void hide_whileLoading_cancelsPendingAttach() {
        // Repro: showView() before the ad is ready queues an attach behind the
        // load; the user then hides (or leaves the screen). When the load lands,
        // the ad must NOT pop back into the container the caller already hid.
        FakeViewAds ad = new FakeViewAds();
        ad.show("alias", container);
        assertEquals(AdState.LOADING, ad.getState());

        ad.hide(); // before the load completes

        ad.completeLoad(activity);
        assertEquals("ad stays cached for the next show", AdState.READY, ad.getState());
        assertEquals("hidden container must stay empty", 0, container.getChildCount());
        assertFalse(ad.isShowing());
    }

    @Test
    public void show_secondContainerWhileLoading_attachesToLatestOnly() {
        // The LAST show() wins when the attach is deferred behind a load.
        FakeViewAds ad = new FakeViewAds();
        FrameLayout other = new FrameLayout(activity);

        ad.show("alias", container);  // queued attach #1
        ad.show("alias", other);      // queued attach #2 -> latest desired target

        ad.completeLoad(activity);
        assertEquals("stale target must stay empty", 0, container.getChildCount());
        assertEquals("latest target gets the ad", 1, other.getChildCount());
        assertTrue(ad.isShowing());
    }
}
