package com.mct.app.helper.admob.listener;

/**
 * Why the library decided NOT to show an ad (no impression happened).
 * <p>
 * Reported via {@link AdCallback#onSkipped(SkipReason)}. Each constant maps 1:1
 * to a guard in the show pipeline, so callers can react intelligently (e.g.
 * retry later for {@link #COOLDOWN}, give up for {@link #PREMIUM}).
 */
public enum SkipReason {

    /** User is premium — ads are disabled. */
    PREMIUM,

    /** No ad is registered under the given alias. */
    NOT_REGISTERED,

    /** No ad is cached and the caller used {@code ShowMode.IF_READY}. */
    NOT_LOADED,

    /** A load is in flight and the caller used {@code ShowMode.IF_READY}. */
    LOADING,

    /** This placement is still inside its own interval since its last impression. */
    COOLDOWN,

    /** Blocked by the global "no back-to-back full-screen ads" gap. */
    GLOBAL_GAP,

    /** Another full-screen ad is currently on screen. */
    OVERLAP,

    /** The activity is null, finishing or destroyed. */
    ACTIVITY_INVALID
}
