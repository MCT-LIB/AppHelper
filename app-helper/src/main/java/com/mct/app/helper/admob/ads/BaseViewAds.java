package com.mct.app.helper.admob.ads;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

public abstract class BaseViewAds<AdsView extends View> extends BaseAds<AdsView> {

    public BaseViewAds(String adsUnitId) {
        super(adsUnitId, 0);
    }

    public final void forceShow(@NonNull String alias, @NonNull ViewGroup container, boolean multiContainer) {
        AdsView ads = getAds();
        if (!multiContainer && isShowing() && ads != null && ads.getParent() != container) {
            // already showing in another container so need hide it
            hide();
        }
        clear();
        show(alias, container);
    }

    public final void show(@NonNull String alias, @NonNull ViewGroup container) {
        if (isLoading()) {
            setAdLoadCallbacks(
                    () -> show(alias, container),
                    () -> invokeCallback(null)
            );
            return;
        }
        if (isCanLoadAds()) {
            load(container.getContext(),
                    () -> show(alias, container),
                    () -> invokeCallback(null)
            );
            return;
        }
        AdsView ads = getAds();
        if (isShowing()) {
            // already showing in the same container
            if (ads != null && ads.getParent() == container) {
                return;
            }
            hide();
        }
        if (isCanShowAds()) {
            setShowing(true);
            setCustomAlias(alias);
            removeInParent(ads);
            container.removeAllViews();
            container.addView(ads);
        }
    }

    public final void hide() {
        setShowing(false);
        removeInParent(getAds());
    }

    private void removeInParent(View view) {
        if (view == null) {
            return;
        }
        if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }

}
