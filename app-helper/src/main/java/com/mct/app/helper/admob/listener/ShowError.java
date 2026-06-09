package com.mct.app.helper.admob.listener;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;

/**
 * Immutable value type describing why a full-screen ad failed to render. Wraps
 * the {@code code / message / domain} triple from AdMob's {@link AdError}
 * (delivered via {@code onAdFailedToShowFullScreenContent}).
 */
public final class ShowError {

    private final int code;
    private final String message;
    private final String domain;

    public ShowError(int code, String message, String domain) {
        this.code = code;
        this.message = message;
        this.domain = domain;
    }

    @NonNull
    public static ShowError of(@NonNull AdError error) {
        return new ShowError(error.getCode(), error.getMessage(), error.getDomain());
    }

    /** A synthetic error used when {@code show} throws synchronously (e.g. BadTokenException). */
    @NonNull
    public static ShowError of(@NonNull Throwable t) {
        return new ShowError(-1, String.valueOf(t.getMessage()), "com.mct.app.helper");
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
        return "ShowError{code=" + code + ", message='" + message + "', domain='" + domain + "'}";
    }
}
