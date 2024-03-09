package com.mct.app.helper.admob.ads;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoadCallback;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.mct.app.helper.R;
import com.mct.app.helper.admob.AdsUtils;

public class NativeAds extends BaseViewAds<NativeAdView> {

    private final int layoutRes;

    public NativeAds(String adsUnitId) {
        this(adsUnitId, AdsUtils.NATIVE_LAYOUT);
    }

    public NativeAds(String adsUnitId, @LayoutRes int layoutRes) {
        super(adsUnitId);
        this.layoutRes = layoutRes;
    }

    @LayoutRes
    public int getLayoutRes() {
        return layoutRes;
    }

    @SuppressLint("InflateParams")
    @Override
    protected void onLoadAds(@NonNull Context context, @NonNull AdLoadCallback<NativeAdView> callback) {
        new AdLoader.Builder(context, getLoadAdsUnitId())
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .setVideoOptions(new VideoOptions.Builder().setStartMuted(true).build())
                        .build())
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        callback.onAdFailedToLoad(loadAdError);
                    }
                })
                .forNativeAd(nativeAd -> {
                    LayoutInflater inflater = LayoutInflater.from(context);
                    NativeAdView nativeAdView = (NativeAdView) inflater.inflate(layoutRes, null);
                    populateNativeAdView(nativeAd, nativeAdView);
                    callback.onAdLoaded(nativeAdView);
                })
                .build()
                .loadAd(getAdRequest());
    }

    private void populateNativeAdView(@NonNull NativeAd nativeAd, @NonNull NativeAdView adView) {
        // Set the media view.
        adView.setMediaView(adView.findViewById(R.id.ad_media));

        // Set other ad assets.
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        // The headline and mediaContent are guaranteed to be in every NativeAd.
        setText(adView.getHeadlineView(), nativeAd.getHeadline());
        notNull(adView.getMediaView(), v -> v.setMediaContent(nativeAd.getMediaContent()));

        // These assets aren't guaranteed to be in every NativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.getBody() == null) {
            setVisible(adView.getBodyView(), View.INVISIBLE);
        } else {
            setVisible(adView.getBodyView(), View.VISIBLE);
            setText(adView.getBodyView(), nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            setVisible(adView.getCallToActionView(), View.INVISIBLE);
        } else {
            setVisible(adView.getCallToActionView(), View.VISIBLE);
            setText(adView.getCallToActionView(), nativeAd.getCallToAction());
        }

        if (nativeAd.getIcon() == null) {
            setVisible(adView.getIconView(), View.GONE);
        } else {
            setVisible(adView.getIconView(), View.VISIBLE);
            setImageDrawable(adView.getIconView(), nativeAd.getIcon().getDrawable());
        }

        if (nativeAd.getPrice() == null) {
            setVisible(adView.getPriceView(), View.INVISIBLE);
        } else {
            setVisible(adView.getPriceView(), View.VISIBLE);
            setText(adView.getPriceView(), nativeAd.getPrice());
        }

        if (nativeAd.getStore() == null) {
            setVisible(adView.getStoreView(), View.INVISIBLE);
        } else {
            setVisible(adView.getStoreView(), View.VISIBLE);
            setText(adView.getStoreView(), nativeAd.getStore());
        }

        if (nativeAd.getStarRating() == null) {
            setVisible(adView.getStarRatingView(), View.INVISIBLE);
        } else {
            setVisible(adView.getStarRatingView(), View.VISIBLE);
            setRating(adView.getStarRatingView(), nativeAd.getStarRating().floatValue());
        }

        if (nativeAd.getAdvertiser() == null) {
            setVisible(adView.getAdvertiserView(), View.INVISIBLE);
        } else {
            setVisible(adView.getAdvertiserView(), View.VISIBLE);
            setText(adView.getAdvertiserView(), nativeAd.getAdvertiser());
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd);

        // Updates the UI to say whether or not this ad has a video asset.
        if (nativeAd.getMediaContent() != null && nativeAd.getMediaContent().hasVideoContent()) {
            // Get the video controller for the ad. One will always be provided, even if the ad doesn't
            // have a video asset.
            VideoController vc = nativeAd.getMediaContent().getVideoController();
            // Create a new VideoLifecycleCallbacks object and pass it to the VideoController. The
            // VideoController will call methods on this object when events occur in the video
            // lifecycle.
            vc.setVideoLifecycleCallbacks(new VideoController.VideoLifecycleCallbacks() {
                @Override
                public void onVideoEnd() {
                    // Publishers should allow native ads to complete video playback before
                    // refreshing or replacing them with another ad in the same UI location.
                    super.onVideoEnd();
                }
            });
        }
    }

    /* --- helper methods --- */

    protected static <T> void notNull(T view, Consumer<T> consumer) {
        if (view != null) {
            consumer.accept(view);
        }
    }

    protected static void setText(View view, String text) {
        if (view instanceof TextView) {
            ((TextView) view).setText(text);
        }
    }

    protected static void setImageDrawable(View view, Drawable drawable) {
        if (view instanceof ImageView) {
            ((ImageView) view).setImageDrawable(drawable);
        }
    }

    protected static void setRating(View view, float rating) {
        if (view instanceof RatingBar) {
            ((RatingBar) view).setRating(rating);
        }
    }

    protected static void setVisible(View view, int visibility) {
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

}
