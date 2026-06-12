package com.mct.app.helper.admob.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
 * Repro for the {@link NativeAdsPool} "never tops up" bug.
 *
 * <p>GMS {@code AdLoader.loadAds(request, n)} fills at most
 * {@code POOL_MAX_LOAD_SIZE} (5) native ads per call, so a pool configured larger
 * than 5 needs more than one load to reach its target size. The pool's own javadoc
 * promises "the next load() naturally tops the pool up". It does not: once the
 * first batch leaves the pool {@link AdState#READY}, {@code BaseAds.load()}
 * short-circuits on {@code isReady()} and never issues another fill — so a
 * {@code poolSize} of 6..10 is silently capped at 5 until the 4h expiry.
 *
 * <p>The real {@code onLoad} can only be driven with live GMS, so this uses a
 * {@link FakeNativeAdsPool} double that reproduces the exact resolution policy
 * (cap each batch at 5, resolve READY once at least one ad is collected) without
 * touching {@code AdLoader}/{@code NativeAd} — the same approach as
 * {@link FakeFullScreenAds}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class NativeAdsPoolRobolectricTest {

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
    public void firstBatch_capsAtMaxLoadSize() {
        FakeNativeAdsPool pool = new FakeNativeAdsPool(10);

        pool.load(activity, null);
        assertEquals(AdState.LOADING, pool.getState());

        pool.completeBatch();
        assertEquals(AdState.READY, pool.getState());
        assertEquals("a single GMS loadAds() fills at most 5", 5, pool.filled);
    }

    @Test
    public void poolBelowTarget_topsUpOnNextLoad() {
        FakeNativeAdsPool pool = new FakeNativeAdsPool(10);

        // first fill -> 5 ads, pool becomes READY
        pool.load(activity, null);
        pool.completeBatch();
        assertEquals(5, pool.filled);
        assertEquals(AdState.READY, pool.getState());

        // ask the pool to load again: it is still below its configured size of 10,
        // so it must issue a SECOND fill (this is the behaviour the javadoc promises)
        pool.load(activity, null);
        pool.completeBatch();

        assertTrue("a second fill must actually run when the pool is under target",
                pool.onLoadCalls >= 2);
        assertEquals("pool must reach its configured size, not stay capped at 5",
                10, pool.filled);
    }

    /**
     * Test double that mimics the real {@link NativeAdsPool} GMS resolution policy
     * (batch capped at 5, resolve READY when at least one ad is collected) using a
     * plain counter instead of real {@code NativeAd} objects.
     */
    static class FakeNativeAdsPool extends NativeAdsPool {

        static final int MAX_LOAD = 5;

        final int targetSize;
        int filled;
        int onLoadCalls;
        private LoadSession pending;

        FakeNativeAdsPool(int poolSize) {
            super("ca-app-test");
            setPoolSize(poolSize);
            this.targetSize = Math.min(poolSize, 10); // mirrors POOL_MAX_SIZE
        }

        @Override
        protected void onLoad(@NonNull Context context, @NonNull LoadSession session) {
            onLoadCalls++;
            pending = session;
        }

        @Override
        protected void onRelease(@Nullable Object ad) {
            filled = 0;
        }

        /** Simulate one GMS loadAds() batch: fill up to MAX_LOAD slots, then resolve. */
        void completeBatch() {
            if (pending == null || pending.isStale()) {
                return; // no fresh load in flight (e.g. load() short-circuited)
            }
            filled += Math.min(targetSize - filled, MAX_LOAD);
            pending.loaded(new Object());
        }
    }
}
