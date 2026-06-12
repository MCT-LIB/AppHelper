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
import java.util.concurrent.atomic.AtomicReference;

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
        // GMS contract for loadAds(request, n): forNativeAd fires once per filled slot and
        // onAdFailedToLoad fires for failed fills, but the TOTAL number of callbacks for a
        // partial fill is NOT guaranteed to be n. Counting callbacks down to zero can leave
        // the session unresolved forever -> pool stuck in LOADING -> perpetual adapter
        // spinners. The documented completion signal is adLoader.isLoading() checked inside
        // BOTH callbacks, so the session is finalized exactly once, on the last callback:
        // - at least one ad collected  -> loaded (partial fills are a success; the pool
        //   tops itself up on the next load() via shouldTopUpWhenReady())
        // - nothing collected         -> failed
        AtomicInteger collected = new AtomicInteger();
        AtomicReference<LoadAdError> lastError = new AtomicReference<>();
        AtomicReference<AdLoader> loaderRef = new AtomicReference<>();
        Runnable finalizeIfDone = () -> {
            AdLoader loader = loaderRef.get();
            if (loader == null || loader.isLoading()) {
                return; // more callbacks coming
            }
            if (session.isStale()) {
                return; // pool was cleared mid-load; late ads are destroyed in forNativeAd
            }
            if (collected.get() > 0) {
                session.loaded(SENTINEL);
            } else {
                LoadAdError error = lastError.get();
                session.failed(error != null ? error
                        : new LoadAdError(89, "No native ads filled", AdError.UNDEFINED_DOMAIN, null, null));
            }
            notifyPoolRefreshed();
        };
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
                        lastError.set(loadAdError);
                        finalizeIfDone.run();
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
                    collected.incrementAndGet();
                    finalizeIfDone.run();
                }).build();

        loaderRef.set(adLoader);

        int requestSize = Math.min(poolSize - nativeAdsList.size(), POOL_MAX_LOAD_SIZE);
        if (requestSize == 1) {
            adLoader.loadAd(getAdRequest());
        } else {
            adLoader.loadAds(getAdRequest(), requestSize);
        }
    }

    @Override
    protected void onRelease(@Nullable Object ad) {
        nativeAdsList.forEach(NativeAd::destroy);
        nativeAdsList.clear();
        position = -1;
    }

    /**
     * The pool is READY as soon as its first batch lands, but a single GMS
     * {@code loadAds} fills at most {@link #POOL_MAX_LOAD_SIZE} ads. While the pool
     * still holds fewer than its configured {@link #poolSize}, allow {@code load()}
     * to run again and append another batch instead of short-circuiting on
     * {@code isReady()}.
     */
    @Override
    protected boolean shouldTopUpWhenReady() {
        return nativeAdsList.size() < poolSize;
    }

    public interface OnPoolRefreshedListener {

        void onPoolRefreshed();
    }

}
