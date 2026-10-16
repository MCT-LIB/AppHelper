package com.mct.app.helper.admob.listener;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.LoadAdError;

/**
 * Immutable value type describing why an ad failed to load. Wraps the
 * {@code code / message / domain} triple from AdMob's {@link LoadAdError}.
 */
public final class LoadError {

    /**
     * Library-side code: the ad was not requested because UMP consent does not allow ad requests
     * (still unresolved, or resolved without permission). Not an AdMob error code.
     */
    public static final int CODE_CONSENT_NOT_OBTAINED = -100;

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
