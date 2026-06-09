package com.mct.app.helper.admob.ads;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple cache of preloaded native ads for use with {@link com.mct.app.helper.admob.ads.natives.NativeAdsAdapter}.
 */
public class NativeAdsPool extends BaseAds<Object> {

    private static final int POOL_MAX_SIZE = 10;
    private static final int POOL_MAX_LOAD_SIZE = 5;

    /** Placeholder "ad" object so the base state machine has a non-null READY ad. */
    private static final Object SENTINEL = new Object();

    private int poolSize;
    private final List<NativeAd> nativeAdsList = new ArrayList<>();
    private final Set<OnPoolRefreshedListener> onPoolRefreshedListeners = new HashSet<>();

    private int position = -1;

    public NativeAdsPool(String adsUnitId) {
        super(adsUnitId, 0);
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = Math.min(poolSize, POOL_MAX_SIZE);
    }

    public NativeAd get() {
        return get(++position);
    }

    public NativeAd get(int position) {
        return nativeAdsList.isEmpty() ? null : nativeAdsList.get(Math.floorMod(position, nativeAdsList.size()));
    }

    public void addOnPoolRefreshedListener(OnPoolRefreshedListener listener) {
        if (listener != null) {
            onPoolRefreshedListeners.add(listener);
        }
    }

    public void removeOnPoolRefreshedListener(OnPoolRefreshedListener listener) {
        if (listener != null) {
            onPoolRefreshedListeners.remove(listener);
        }
    }

    private void notifyPoolRefreshed() {
        for (OnPoolRefreshedListener listener : new ArrayList<>(onPoolRefreshedListeners)) {
            listener.onPoolRefreshed();
        }
    }

    @Override
    protected void onLoad(@NonNull Context context, @NonNull LoadSession session) {
        if (poolSize <= 0) {
            session.failed(new LoadAdError(88, "Pool size is 0", AdError.UNDEFINED_DOMAIN, null, null));
            return;
        }
        if (nativeAdsList.size() >= poolSize) {
            session.loaded(SENTINEL);
            return;
        }
        notifyPoolRefreshed();
        AtomicInteger adRequestSize = new AtomicInteger();
        AdLoader adLoader = new AdLoader.Builder(context, getLoadAdsUnitId())
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .setVideoOptions(new VideoOptions.Builder().setStartMuted(true).build())
                        .build())
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdClicked() {
                        NativeAdsPool.this.onAdClicked();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        boolean stale = session.isStale();
                        session.failed(loadAdError);
                        if (!stale) {
                            notifyPoolRefreshed();
                        }
                    }
                })
                .forNativeAd(nativeAd -> {
                    if (session.isStale()) {
                        // pool was cleared mid-load: don't leak this late ad
                        nativeAd.destroy();
                        return;
                    }
                    nativeAd.setOnPaidEventListener(NativeAdsPool.this::onPaidEvent);
                    nativeAdsList.add(nativeAd);
                    if (adRequestSize.decrementAndGet() == 0) {
                        session.loaded(SENTINEL);
                        notifyPoolRefreshed();
                    }
                }).build();

        adRequestSize.set(Math.min(poolSize - nativeAdsList.size(), POOL_MAX_LOAD_SIZE));

        if (adRequestSize.get() == 1) {
            adLoader.loadAd(getAdRequest());
        } else {
            adLoader.loadAds(getAdRequest(), adRequestSize.get());
        }
    }

    @Override
    protected void onRelease(@Nullable Object ad) {
        nativeAdsList.forEach(NativeAd::destroy);
        nativeAdsList.clear();
        position = -1;
    }

    public interface OnPoolRefreshedListener {

        void onPoolRefreshed();
    }

}
