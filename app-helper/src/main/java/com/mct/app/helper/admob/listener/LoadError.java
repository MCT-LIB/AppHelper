package com.mct.app.helper.admob.listener;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.LoadAdError;

/**
 * Immutable value type describing why an ad failed to load. Wraps the
 * {@code code / message / domain} triple from AdMob's {@link LoadAdError}.
 */
public final class LoadError {

    /**
     * Library-side code (not AdMob): the ad was not requested because UMP's {@code canRequestAds()}
     * is false after consent was decided, i.e. consent information is not available for this user
     * yet. It does NOT mean "user refused": a user who declines personalised ads still gets
     * non-personalised ads ({@code canRequestAds()} is true). Also used for loads failed by
     * {@code AdsManager.clear()} while waiting for consent.
     */
    public static final int CODE_CONSENT_NOT_OBTAINED = -100;

    /**
     * Library-side code: a load parked while consent was pending could not be replayed because the
     * Activity it was requested from is gone. Load again from a live screen.
     */
    public static final int CODE_CONTEXT_GONE = -101;

    private final int code;
    private final String message;
    private final String domain;

    public LoadError(int code, String message, String domain) {
        this.code = code;
        this.message = message;
        this.domain = domain;
    }

    @NonNull
    public static LoadError of(@NonNull LoadAdError error) {
        return new LoadError(error.getCode(), error.getMessage(), error.getDomain());
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDomain() {
        return domain;
    }

    @NonNull
    @Override
    public String toString() {
        return "LoadError{code=" + code + ", message='" + message + "', domain='" + domain + "'}";
    }
}
