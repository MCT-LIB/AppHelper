package com.mct.app.helper.admob.ads;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

public abstract class BaseViewAds<AdsView extends View> extends BaseAds<AdsView> {

    public BaseViewAds(String adsUnitId) {
        super(adsUnitId, 0);
    }

    public final void forceLoadAndShow(@NonNull ViewGroup container) {
        if (isLoading()) {
            disposeAdsLoadIfNeed();
        }
        if (isShowing()) {
            AdsView ads = getAds();
            if (ads != null && ads.getParent() == container) {
                // already showing in the same container
                // so no need hide it because removeAllViews() will remove it
                setShowing(false);
            } else {
                // already showing in another container so need hide it
                hide();
            }
        }
        setAds(null);
        show(container);
    }

    public final void show(@NonNull ViewGroup container) {
        if (isCanLoadAds()) {
            load(container.getContext(),
                    () -> show(container),
                    () -> {
                    }
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
