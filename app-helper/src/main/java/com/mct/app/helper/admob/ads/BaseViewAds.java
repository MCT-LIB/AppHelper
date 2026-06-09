package com.mct.app.helper.admob.ads;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mct.app.helper.admob.ads.BaseAds;
import com.mct.app.helper.admob.listener.LoadCallback;

/**
 * Base for view ads (banner, native). A view ad stays {@link com.mct.app.helper.admob.AdState#READY}
 * whether or not it is attached to a container — "showing" is a view-tree fact
 * (the ad has a parent), not a separate state — so the same ad can be re-shown
 * or moved between containers without reloading.
 */
public abstract class BaseViewAds<AdsView extends View> extends BaseAds<AdsView> {

    public BaseViewAds(String adsUnitId) {
        super(adsUnitId, 0);
    }

    /** Destroy native resources of the view ad (e.g. {@code AdView.destroy()}). */
    protected abstract void onDestroyView(@NonNull AdsView view);

    @Override
    protected final void onRelease(@Nullable AdsView view) {
        if (view != null) {
            removeInParent(view);
            onDestroyView(view);
        }
    }

    @Override
    public boolean isShowing() {
        AdsView view = getAds();
        return view != null && view.getParent() != null;
    }

    /** Show the ad in {@code container}, loading first if necessary. */
    public final void show(@NonNull String alias, @NonNull ViewGroup container) {
        assertMainThread();
        setCustomAlias(alias);
        if (isReady()) {
            attach(container);
            return;
        }
        load(container.getContext(), new LoadCallback() {
            @Override
            public void onAdLoaded(BaseAds<?> ad) {
                attach(container);
            }
        });
    }

    /**
     * Show the ad in {@code container}. When {@code reload} is true the current ad
     * is cleared first so a fresh one is loaded (use for e.g. collapsible banners
     * that need a new request); otherwise a ready ad is reused.
     */
    public final void forceShow(@NonNull String alias, @NonNull ViewGroup container, boolean reload) {
        assertMainThread();
        if (reload) {
            clear();
        }
        show(alias, container);
    }

    /** Detach the ad from its container (the ad object is kept and can be re-shown). */
    public final void hide() {
        assertMainThread();
        removeInParent(getAds());
    }

    private void attach(@NonNull ViewGroup container) {
        AdsView view = getAds();
        if (view == null) {
            return;
        }
        if (view.getParent() == container) {
            return; // already shown in this container
        }
        removeInParent(view);
        container.removeAllViews();
        container.addView(view);
    }

    private void removeInParent(View view) {
        if (view != null && view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }
}
