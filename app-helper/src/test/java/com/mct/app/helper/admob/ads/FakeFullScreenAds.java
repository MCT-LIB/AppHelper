package com.mct.app.helper.admob.ads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.mct.app.helper.admob.listener.AdCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Test double for a full-screen ad. Lets a test drive the load/show lifecycle
 * deterministically without any real GMS ad: {@link #onLoad} just captures the
 * {@link LoadSession}, and {@link #onShowAds} captures the GMS content callback
 * (or throws, to exercise the show-failure rollback).
 * <p>
 * Lives in the {@code ads} package so tests can reach the protected hooks.
 */
public class FakeFullScreenAds extends BaseFullScreenAds<Object> {

    public LoadSession lastSession;
    /** Context handed to the last onLoad (consent-gate tests). */
    public Context lastLoadContext;
    public FullScreenContentCallback lastContentCallback;
    public AdCallback lastShowCallback;
    public boolean throwOnShow;
    /** When non-null, overrides the stuck-SHOWING watchdog threshold. */
    public Long maxShowDurationOverride;
    public final List<Object> released = new ArrayList<>();

    private final boolean allowInterval;

    public FakeFullScreenAds() {
        this(0, false);
    }

    public FakeFullScreenAds(long interval, boolean allowInterval) {
        super("ca-app-test", interval);
        this.allowInterval = allowInterval;
    }

    @Override
    protected void onLoad(@NonNull Context context, @NonNull LoadSession session) {
        lastLoadContext = context;
        lastSession = session;
    }

    @Override
    protected void onShowAds(@NonNull Activity activity, @NonNull Object ads,
                             @NonNull FullScreenContentCallback contentCallback,
                             @Nullable AdCallback callback) {
        if (throwOnShow) {
            throw new RuntimeException("boom");
        }
        lastContentCallback = contentCallback;
        lastShowCallback = callback;
    }

    @Override
    protected void onRelease(@Nullable Object ad) {
        released.add(ad);
    }

    @Override
    protected boolean allowAdsInterval() {
        return allowInterval;
    }

    @Override
    protected long getMaxShowDurationMs() {
        return maxShowDurationOverride != null ? maxShowDurationOverride : super.getMaxShowDurationMs();
    }

    // --- test drivers ---

    public Object completeLoad() {
        Object ad = new Object();
        lastSession.loaded(ad);
        return ad;
    }

    public void failLoad() {
        lastSession.failed(new LoadAdError(1, "fail", "domain", null, null));
    }

    public void driveShown() {
        lastContentCallback.onAdShowedFullScreenContent();
    }

    public void driveDismissed() {
        lastContentCallback.onAdDismissedFullScreenContent();
    }

    public void driveFailedToShow() {
        lastContentCallback.onAdFailedToShowFullScreenContent(new AdError(2, "show-fail", "domain"));
    }
}
