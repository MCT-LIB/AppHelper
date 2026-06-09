package com.mct.app.helper.admob.listener;

/**
 * Controls what happens when a full-screen / rewarded ad is asked to show but is
 * not currently cached. Makes "load then show" an explicit, self-documenting
 * option instead of a separate method name (the v1 {@code showSyncLoad}).
 */
public enum ShowMode {

    /**
     * Show only if an ad is already cached. If not ready, nothing is shown and
     * {@link AdCallback#onSkipped(SkipReason)} is invoked
     * ({@link SkipReason#NOT_LOADED} / {@link SkipReason#LOADING}).
     */
    IF_READY,

    /**
     * If no ad is cached, load one first and then show it as soon as it is ready.
     * Load failures are reported via {@link AdCallback#onSkipped(SkipReason)}.
     * This is the v1 {@code showSyncLoad} behaviour, now named.
     */
    LOAD_IF_NEEDED
}
