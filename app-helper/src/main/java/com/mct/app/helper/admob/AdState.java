package com.mct.app.helper.admob;

/**
 * Lifecycle state of a single ad instance.
 * <p>
 * Kept Android/GMS free so the eligibility logic in {@link AdsShowPolicy} can be
 * unit tested on a plain JVM.
 *
 * <pre>
 *   IDLE ──load──▶ LOADING ──success──▶ READY ──show──▶ SHOWING
 *    ▲                │                   │                 │
 *    │                └──fail─────────────┘                 │
 *    └──────clear / expire / dismiss / failToShow───────────┘
 * </pre>
 *
 * There is intentionally no sticky {@code FAILED} or {@code EXPIRED} state:
 * a load failure returns to {@link #IDLE} (retryable), and "expired" is a
 * derived predicate over {@link #READY} + the load timestamp. This makes it
 * structurally impossible for an ad to get permanently stuck.
 */
public enum AdState {

    /** No ad object held and nothing in flight. Initial / post-clear / post-consume / post-fail. */
    IDLE,

    /** A load request is in flight. Exactly one request may be outstanding. */
    LOADING,

    /** An ad object is held and ready to show (subject to expiry/interval checks). */
    READY,

    /** A full-screen ad is on screen, or a view ad is attached to a container. */
    SHOWING
}
