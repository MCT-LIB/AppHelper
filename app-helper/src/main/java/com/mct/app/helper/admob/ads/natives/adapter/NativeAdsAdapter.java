package com.mct.app.helper.admob.ads.natives.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.nativead.NativeAd;
import com.mct.app.helper.R;
import com.mct.app.helper.admob.ads.natives.NativeTemplate;
import com.mct.app.helper.admob.ads.natives.NativeTemplateStyle;
import com.mct.app.helper.admob.ads.natives.NativeTemplateView;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class NativeAdsAdapter extends RecyclerViewAdapterWrapper {

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
        this.nativeAdsPool.setRefreshListener(this::updateNativeAd);
    }

    public int convertAdPositionToOriginPosition(int position) {
        int numAdsBeforePosition = (position + 1 + adsItemOffset) / (adsItemInterval + 1);
        return position - numAdsBeforePosition;
    }

    public boolean isAdPosition(int position) {
        return (position + 1 + adsItemOffset) % (adsItemInterval + 1) == 0;
    }

    @Override
    public int getItemCount() {
        int realCount = super.getItemCount();
        return realCount + (realCount + adsItemOffset) / adsItemInterval;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_NATIVE_ADS) {
            return onCreateAdViewHolder(parent);
        }
        return super.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AdViewHolder) {
            onBindAdViewHolder((AdViewHolder) holder);
            if (holder.isRecyclable()) {
                boundViewHolders.add((AdViewHolder) holder);
            }
        } else {
            super.onBindViewHolder(holder, convertAdPositionToOriginPosition(position));
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof AdViewHolder) {
            onRecycleAdViewHolder((AdViewHolder) holder);
            boundViewHolders.remove(holder);
        } else {
            super.onViewRecycled(holder);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isAdPosition(position)) {
            return TYPE_NATIVE_ADS;
        }
        return super.getItemViewType(convertAdPositionToOriginPosition(position));
    }

    @NonNull
    private RecyclerView.ViewHolder onCreateAdViewHolder(@NonNull ViewGroup parent) {
        return new AdViewHolder(parent, itemContainerLayoutRes, itemContainerId, templateLayoutRes);
    }

    private void onBindAdViewHolder(@NonNull AdViewHolder holder) {
        clearNativeAd(holder);
        holder.setNativeAd(nativeAdsPool.pop(), templateStyle, true);
    }

    private void onRecycleAdViewHolder(@NonNull AdViewHolder holder) {
        clearNativeAd(holder);
    }

    private void clearNativeAd(@NonNull AdViewHolder holder) {
        Optional.ofNullable(holder.getNativeAd()).ifPresent(nativeAdsPool::push);
        holder.clearNativeAds();
    }

    private void updateNativeAd() {
        Optional.ofNullable(nativeAdsPool.peek()).ifPresent(nativeAd -> boundViewHolders.stream()
                .filter(holder -> holder.getNativeAd() == null)
                .forEach(holder -> holder.setNativeAd(nativeAd, templateStyle, false))
        );
    }

    private static class AdViewHolder extends RecyclerView.ViewHolder {

        ViewGroup container;
        NativeTemplateView templateView;

        AdViewHolder(ViewGroup parent, int itemContainerLayoutRes, int itemContainerId, int templateLayoutRes) {
            super(createItemView(parent, itemContainerLayoutRes));
            container = itemView.findViewById(itemContainerId);
            templateView = new NativeTemplateView(parent.getContext(), templateLayoutRes);
            container.addView(templateView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        @Nullable
        NativeAd getNativeAd() {
            return (NativeAd) Optional.ofNullable(templateView.getTag())
                    .filter(o -> o instanceof NativeAd)
                    .orElse(null);
        }

        void clearNativeAds() {
            templateView.setTag(null);
        }

        void setNativeAd(NativeAd nativeAd, NativeTemplateStyle templateStyle, boolean setCacheTag) {
            if (setCacheTag) {
                templateView.setTag(nativeAd);
            }
            if (nativeAd != null) {
                templateView.setNativeAd(nativeAd);
                templateView.setStyles(templateStyle);
            }
        }

        static View createItemView(@NonNull ViewGroup parent, int itemContainerLayoutRes) {
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
