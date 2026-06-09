package com.mct.app.helper.admob.ads;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * Test double for a view ad (banner/native). Captures the {@link LoadSession} so a
 * test can complete the load deterministically, and counts {@link #onDestroyView}
 * calls to verify resources are released (the v1 banner-leak fix).
 */
public class FakeViewAds extends BaseViewAds<View> {

    public LoadSession lastSession;
    public int destroyed;

    public FakeViewAds() {
        super("ca-view-test");
    }

    @Override
    protected void onLoad(@NonNull Context context, @NonNull LoadSession session) {
        lastSession = session;
    }

    @Override
    protected void onDestroyView(@NonNull View view) {
        destroyed++;
    }

    public View completeLoad(Context context) {
        View view = new View(context);
        lastSession.loaded(view);
        return view;
    }
}
