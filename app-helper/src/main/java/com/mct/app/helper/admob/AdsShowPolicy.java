package com.mct.app.helper.admob;

/**
 * Pure, side-effect-free decisions for ad load / show eligibility and full-screen
 * frequency capping.
 * <p>
 * Kept completely free of Android / GMS types so the policy logic can be unit
 * tested on a plain JVM. All time-based logic is plain timestamp arithmetic — the
 * v1 {@code Handler}-based interval cooldown is gone.
 * <p>
 * AdMob policy this encodes (see
 * <a href="https://support.google.com/admob/answer/6201362">Disallowed interstitial implementations</a>
 * and <a href="https://support.google.com/admob/answer/9341964">App open ad guidance</a>):
 * <ul>
 *     <li>Two full-screen ads must never be shown back-to-back (one immediately
 *     after another, including immediately before/after an app open ad) —
 *     enforced by the global gap.</li>
 *     <li>Frequency capping is per placement — each ad unit keeps its own
 *     interval; different placements must NOT mutually lock each other for the
 *     whole of each other's interval.</li>
 * </ul>
 */
public final class AdsShowPolicy {

    private AdsShowPolicy() {
    }

    ///////////////////////////////////////////////////////////////////////////
    // Primitive time predicates
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Whether a loaded ad is older than {@code overdueMs} and should be discarded
     * / reloaded.
     */
    public static boolean isExpired(long loadedAt, long now, long overdueMs) {
        return loadedAt > 0 && now - loadedAt > overdueMs;
    }

    /**
     * Whether this placement is still inside its own per-ad interval since its
     * last impression (so it must not be shown again yet).
     */
    public static boolean isWithinInterval(long lastImpressionAt, long now, long perAdIntervalMs) {
        return perAdIntervalMs > 0
                && lastImpressionAt > 0
                && now - lastImpressionAt < perAdIntervalMs;
    }

    /**
     * Whether a full-screen ad was dismissed within {@code nearlyMs} of now.
     */
    public static boolean isDismissNearly(long lastDismissedAt, long now, long nearlyMs) {
        return lastDismissedAt > 0 && now - lastDismissedAt < nearlyMs;
    }

    /**
     * Whether a full-screen ad was dismissed recently enough that showing another
     * one now would count as "back-to-back".
     *
     * @param now                       current time in millis
     * @param lastFullScreenDismissTime most recent successful full-screen dismiss
     *                                  across all placements (0 if none yet)
     * @param globalGapMs               minimum gap required between any two
     *                                  full-screen ads (&le; 0 disables the gap)
     */
    public static boolean isWithinGlobalGap(long now, long lastFullScreenDismissTime, long globalGapMs) {
        return globalGapMs > 0
                && lastFullScreenDismissTime > 0
                && now - lastFullScreenDismissTime < globalGapMs;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Per-ad eligibility (replaces BaseAds#isCanLoadAds / #isCanShowAds)
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Whether a new load may be started for an ad in the given state.
     * <p>
     * Loadable when idle, or when a cached ad has expired (so it can be refreshed).
     * Never while loading or showing, and never for premium users.
     */
    public static boolean canLoad(AdState state, long loadedAt, long now, boolean premium, long overdueMs) {
        if (premium) {
            return false;
        }
        if (state == AdState.IDLE) {
            return true;
        }
        return state == AdState.READY && isExpired(loadedAt, now, overdueMs);
    }

    /**
     * Whether a cached ad may be shown right now.
     * <p>
     * Showable only when {@link AdState#READY}, not expired, past its own
     * interval, and not premium. The GLOBAL back-to-back gap / overlap across
     * different full-screen placements is enforced separately by
     * {@link #canShowFullScreenNow}.
     *
     * @param lastImpressionAt last impression start for this placement
     *                         (full-screen); pass {@code 0} / interval {@code 0}
     *                         for view ads that have no interval
     */
    public static boolean canShow(AdState state,
                                  long loadedAt,
                                  long lastImpressionAt,
                                  long now,
                                  boolean premium,
                                  long perAdIntervalMs,
                                  long overdueMs) {
        if (premium) {
            return false;
        }
        if (state != AdState.READY) {
            return false;
        }
        if (isExpired(loadedAt, now, overdueMs)) {
            return false;
        }
        return !isWithinInterval(lastImpressionAt, now, perAdIntervalMs);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Global full-screen gate (across all placements)
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Decide whether the manager is allowed to begin showing a full-screen ad
     * right now. This is only the GLOBAL gate (overlap + back-to-back gap); the
     * per-placement availability and its own interval are checked separately by
     * {@link #canShow}.
     *
     * @param anyFullScreenShowing      another full-screen ad is currently on screen
     * @param now                       current time in millis
     * @param lastFullScreenDismissTime most recent successful full-screen dismiss
     *                                  across all placements (0 if none yet)
     * @param globalGapMs               minimum gap required between any two
     *                                  full-screen ads (&le; 0 disables the gap)
     * @return true if a full-screen ad may be shown now
     */
    public static boolean canShowFullScreenNow(boolean anyFullScreenShowing,
                                               long now,
                                               long lastFullScreenDismissTime,
                                               long globalGapMs) {
        if (anyFullScreenShowing) {
            // never overlap two full-screen ads
            return false;
        }
        // never show two full-screen ads back-to-back
        return !isWithinGlobalGap(now, lastFullScreenDismissTime, globalGapMs);
    }
}
