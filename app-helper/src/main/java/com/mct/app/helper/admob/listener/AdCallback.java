package com.mct.app.helper.admob.listener;

/**
 * Lifecycle of a single full-screen ad show attempt. All methods are optional
 * (default no-op) so callers override only what they need.
 * <p>
 * Exactly one terminal outcome fires per show attempt, followed by
 * {@link #onComplete(boolean)}:
 * <table border="1">
 *   <caption>Outcome contract</caption>
 *   <tr><th>Outcome</th><th>Methods (in order)</th></tr>
 *   <tr><td>Ad shown &amp; closed</td><td>{@link #onShown()} → {@link #onDismissed()} → {@code onComplete(true)}</td></tr>
 *   <tr><td>Ad present, SDK refused to render</td><td>{@link #onFailedToShow(ShowError)} → {@code onComplete(false)}</td></tr>
 *   <tr><td>Library declined to show</td><td>{@link #onSkipped(SkipReason)} → {@code onComplete(false)}</td></tr>
 * </table>
 */
public interface AdCallback {

    /** The ad was rendered on screen. An impression is counted. */
    default void onShown() {
    }

    /**
     * The ad was closed by the user after being shown. This is the ONLY signal
     * that the user actually saw the ad — chain your next screen / next ad here.
     */
    default void onDismissed() {
    }

    /**
     * The ad was available but the SDK refused to render it. Distinct from
     * {@link #onSkipped(SkipReason)} (which means the library chose not to show).
     */
    default void onFailedToShow(ShowError error) {
    }

    /**
     * The library decided not to show — nothing was rendered, no impression.
     * Inspect {@code reason} to decide fallback behaviour.
     */
    default void onSkipped(SkipReason reason) {
    }

    /**
     * Convenience terminal hook: fires exactly once after the show attempt ends,
     * regardless of which outcome occurred. Handy for "advance regardless" flows
     * (e.g. splash) without duplicating logic across the outcome methods.
     *
     * @param wasShown true if an impression happened (i.e. {@link #onShown()} fired)
     */
    default void onComplete(boolean wasShown) {
    }
}
