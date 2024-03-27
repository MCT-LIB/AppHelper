package com.mct.app.helper.admob.ads.natives.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.nativead.NativeAd;
import com.mct.app.helper.R;
import com.mct.app.helper.admob.ads.natives.NativeTemplate;
import com.mct.app.helper.admob.ads.natives.NativeTemplateStyle;
import com.mct.app.helper.admob.ads.natives.NativeTemplateView;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class NativeAdsAdapter extends RecyclerViewAdapterWrapper implements NativeAdsPool.OnPoolRefreshedListener {

    private static final int TYPE_NATIVE_ADS = 9999;
    private static final int DEFAULT_AD_ITEM_INTERVAL = 4;
    private static final int DEFAULT_AD_ITEM_OFFSET = 2;

    private final NativeAdsPool nativeAdsPool;
    private final NativeTemplateStyle templateStyle;
    private final @LayoutRes int templateLayoutRes;
    private final @LayoutRes int itemContainerLayoutRes;
    private final @IdRes int itemContainerId;
    private final int adsItemInterval;
    private final int adsItemOffset;
    private final Set<AdViewHolder> boundViewHolders;
    private WeakReference<NativeAd> cacheNativeAd;

    private NativeAdsAdapter(@NonNull Builder builder) {
        super(builder.adapter);
        this.nativeAdsPool = builder.nativeAdsPool;
        this.templateStyle = builder.templateStyle;
        this.templateLayoutRes = builder.templateLayoutRes;
        this.itemContainerLayoutRes = builder.itemContainerLayoutRes;
        this.itemContainerId = builder.itemContainerId;
        this.adsItemInterval = builder.adsItemInterval;
        this.adsItemOffset = builder.adsItemOffset;
        this.boundViewHolders = new HashSet<>();
    }

    @Override
    public void onPoolRefreshed() {
        updateNativeAdsHolders();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        nativeAdsPool.registerOnPoolRefreshedListener(this);
        setCacheNativeAd(nativeAdsPool.peek());
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        nativeAdsPool.unregisterOnPoolRefreshedListener(this);
        setCacheNativeAd(null);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_NATIVE_ADS) {
            return createAdViewHolder(parent);
        }
        return super.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AdViewHolder) {
            bindAdViewHolder((AdViewHolder) holder);
            if (holder.isRecyclable()) {
                boundViewHolders.add((AdViewHolder) holder);
            }
        } else {
            if (nativeAdsPool.isAdsUnavailable()) {
                super.onBindViewHolder(holder, position);
                return;
            }
            super.onBindViewHolder(holder, convertAdPositionToOriginPosition(position));
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof AdViewHolder) {
            recycleAdViewHolder((AdViewHolder) holder);
            boundViewHolders.remove(holder);
        } else {
            super.onViewRecycled(holder);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (nativeAdsPool.isAdsUnavailable()) {
            return super.getItemViewType(position);
        }
        if (isAdPosition(position)) {
            return TYPE_NATIVE_ADS;
        }
        return super.getItemViewType(convertAdPositionToOriginPosition(position));
    }

    @Override
    public int getItemCount() {
        if (nativeAdsPool.isAdsUnavailable()) {
            return super.getItemCount();
        }
        int realCount = super.getItemCount();
        return realCount + (realCount + adsItemOffset) / adsItemInterval;
    }

    public boolean isAdsItem(int position) {
        return getItemViewType(position) == TYPE_NATIVE_ADS;
    }

    private int convertAdPositionToOriginPosition(int position) {
        int numAdsBeforePosition = (position + 1 + adsItemOffset) / (adsItemInterval + 1);
        return position - numAdsBeforePosition;
    }

    private boolean isAdPosition(int position) {
        return (position + 1 + adsItemOffset) % (adsItemInterval + 1) == 0;
    }

    @NonNull
    private RecyclerView.ViewHolder createAdViewHolder(@NonNull ViewGroup parent) {
        return new AdViewHolder(parent, itemContainerLayoutRes, itemContainerId, templateLayoutRes);
    }

    private void bindAdViewHolder(@NonNull AdViewHolder holder) {
        // recycle the last ad and get a new one
        nativeAdsPool.push(holder.getNativeAd());
        NativeAd nativeAd = nativeAdsPool.pop();
        if (nativeAd == null) {
            nativeAd = getCacheNativeAd();
        } else {
            setCacheNativeAd(nativeAd);
        }
        holder.setNativeAd(nativeAd, templateStyle);
    }

    private void recycleAdViewHolder(@NonNull AdViewHolder holder) {
        // recycle the ad
        nativeAdsPool.push(holder.getNativeAd());
    }

    private void updateNativeAdsHolders() {
        setCacheNativeAd(nativeAdsPool.peek());
        Optional.ofNullable(getCacheNativeAd()).ifPresent(nativeAd -> boundViewHolders.stream()
                .filter(holder -> holder.getNativeAd() == null)
                .forEach(holder -> holder.setNativeAd(nativeAd, templateStyle))
        );
    }

    private NativeAd getCacheNativeAd() {
        return Optional.ofNullable(cacheNativeAd).map(WeakReference::get).orElse(null);
    }

    private void setCacheNativeAd(NativeAd nativeAd) {
        if (nativeAd == null) {
            if (cacheNativeAd != null) {
                cacheNativeAd.clear();
                cacheNativeAd = null;
            }
            return;
        }
        if (cacheNativeAd == null || cacheNativeAd.get() == null) {
            cacheNativeAd = new WeakReference<>(nativeAd);
        }
    }

    private static class AdViewHolder extends RecyclerView.ViewHolder {

        private final NativeTemplateView templateView;
        private NativeAd nativeAd;

        private AdViewHolder(ViewGroup parent, int itemContainerLayoutRes, int itemContainerId, int templateLayoutRes) {
            super(createItemView(parent, itemContainerLayoutRes));
            templateView = new NativeTemplateView(parent.getContext(), templateLayoutRes);
            templateView.setVisibility(View.GONE);
            ViewGroup container = itemView.findViewById(itemContainerId);
            container.addView(templateView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        private NativeAd getNativeAd() {
            return nativeAd;
        }

        private void setNativeAd(NativeAd nativeAd, NativeTemplateStyle templateStyle) {
            this.nativeAd = nativeAd;
            if (nativeAd != null) {
                templateView.setNativeAd(nativeAd);
                templateView.setStyles(templateStyle);
                templateView.setVisibility(View.VISIBLE);
            } else {
                templateView.setVisibility(View.GONE);
            }
        }

        private static View createItemView(@NonNull ViewGroup parent, int itemContainerLayoutRes) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return inflater.inflate(itemContainerLayoutRes, parent, false);
        }
    }

    public static class Builder {

        private RecyclerView.Adapter<?> adapter;
        private NativeAdsPool nativeAdsPool;
        private NativeTemplateStyle templateStyle;
        private @LayoutRes int templateLayoutRes;
        private @LayoutRes int itemContainerLayoutRes;
        private @IdRes int itemContainerId;
        private int adsItemInterval;
        private int adsItemOffset;

        public Builder(RecyclerView.Adapter<?> adapter, NativeAdsPool pool) {
            this.adapter = adapter;
            this.nativeAdsPool = pool;
            this.templateStyle = null;
            this.templateLayoutRes = R.layout.gnt_template_view_medium;
            this.itemContainerLayoutRes = R.layout.gnt_item_container;
            this.itemContainerId = R.id.gnt_container;
            this.adsItemInterval = DEFAULT_AD_ITEM_INTERVAL;
            this.adsItemOffset = DEFAULT_AD_ITEM_OFFSET;
        }

        public Builder setAdapter(RecyclerView.Adapter<? extends RecyclerView.ViewHolder> adapter) {
            this.adapter = adapter;
            return this;
        }

        public Builder setNativeAdsPool(NativeAdsPool nativeAdsPool) {
            this.nativeAdsPool = nativeAdsPool;
            return this;
        }

        public Builder setNativeTemplateStyle(NativeTemplateStyle templateStyle) {
            this.templateStyle = templateStyle;
            return this;
        }

        public Builder setNativeTemplate(@NonNull NativeTemplate template) {
            this.templateLayoutRes = template.layoutRes;
            return this;
        }

        public Builder setTemplateLayoutRes(int templateLayoutRes) {
            this.templateLayoutRes = templateLayoutRes;
            return this;
        }

        public Builder setItemContainerLayoutRes(int itemContainerLayoutRes) {
            this.itemContainerLayoutRes = itemContainerLayoutRes;
            return this;
        }

        public Builder setItemContainerId(int itemContainerId) {
            this.itemContainerId = itemContainerId;
            return this;
        }

        public Builder setAdsItemConfig(int adsItemInterval, int adsItemOffset) {
            if (adsItemInterval < 0) {
                throw new IllegalArgumentException("adsItemInterval must be greater than 0");
            }
            if (adsItemOffset < 0) {
                adsItemOffset = 0;
            }
            if (adsItemOffset > adsItemInterval) {
                adsItemOffset = adsItemInterval;
            }
            this.adsItemInterval = adsItemInterval;
            this.adsItemOffset = adsItemInterval - adsItemOffset;
            return this;
        }

        public NativeAdsAdapter build() {
            return new NativeAdsAdapter(this);
        }

    }
}
