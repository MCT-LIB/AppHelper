package com.mct.app.helper.admob.ads.natives;

import static androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;
import static androidx.recyclerview.widget.RecyclerView.LayoutManager;
import static androidx.recyclerview.widget.RecyclerView.ViewHolder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.core.math.MathUtils;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.nativead.NativeAd;
import com.mct.app.helper.R;
import com.mct.app.helper.admob.AdsManager;
import com.mct.app.helper.admob.ads.natives.manager.NpaGridLayoutManager;
import com.mct.app.helper.admob.ads.natives.manager.NpaLinearLayoutManager;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * RecyclerView adapter for displaying native ads alongside regular content.
 * <p>
 * This adapter extends RecyclerViewAdapterWrapper to wrap your adapter
 * and seamlessly integrate native ad items into the regular content displayed by the RecyclerView.
 * </p>
 * <b>How to use:</b>
 * <pre>
 * NativeTemplateStyle style = new NativeTemplateStyle.Builder()
 *     .withMainBackgroundColor(Color.parseColor("#fafafa"))
 *     .withCallToActionBackgroundColor(Color.parseColor("#4285f4"))
 *     .withCallToActionCornerRadius(ScreenUtils.dp2px(2))
 *     .build();
 * NativeAdsAdapter adapter = new NativeAdsAdapter.Builder(yourAdapter, Constant.NATIVE_ID, 3)
 *     .setNativeTemplate(NativeTemplate.SMALL_A4)
 *     .setNativeTemplateStyle(style)
 *     //.setItemContainerLayoutRes(_yourItemContainerLayoutRes)
 *     //.setItemContainerId(_yourItemContainerId)
 *     .setAdsItemConfig(3, 3)
 *     .build();
 * recyclerView.setAdapter(adapter);
 * recyclerView.setLayoutManager(new NpaGridLayoutManager(recyclerView.getContext(), 2));
 * </pre>
 * <b>Note:</b>
 * if you get the error {@link IndexOutOfBoundsException} "Inconsistency detected.
 * Invalid view holder adapter position...",
 * please use {@link NpaLinearLayoutManager} or {@link NpaGridLayoutManager}
 */
public class NativeAdsAdapter extends RecyclerViewAdapterWrapper {

    public static final int TYPE_ADS = 9999;
    private static final int DEFAULT_AD_ITEM_INTERVAL = 4;
    private static final int DEFAULT_AD_ITEM_OFFSET = 2;

    private final String adsUnitId;
    private final int adsCacheSize;
    private final int adsItemInterval;
    private final int adsItemOffset;
    private final NativeTemplateStyle templateStyle;
    private final int templateLayoutRes;
    private final int itemLoadingLayoutRes;
    private final int itemContainerLayoutRes;
    private final int itemContainerId;
    private final Set<AdViewHolder> boundAdsViewHolders;

    private RecyclerView rcv;
    private NativeAdsPool nativeAdsPool;

    private NativeAdsAdapter(@NonNull Builder builder) {
        super(builder.adapter);
        this.adsUnitId = builder.adsUnitId;
        this.adsCacheSize = builder.adsCacheSize;
        this.adsItemInterval = builder.adsItemInterval;
        this.adsItemOffset = builder.adsItemOffset;
        this.templateStyle = builder.templateStyle;
        this.templateLayoutRes = builder.templateLayoutRes;
        this.itemLoadingLayoutRes = builder.itemLoadingLayoutRes;
        this.itemContainerLayoutRes = builder.itemContainerLayoutRes;
        this.itemContainerId = builder.itemContainerId;
        this.boundAdsViewHolders = new LinkedHashSet<>();
    }

    @NonNull
    @Override
    protected AdapterDataObserver createDataObserver() {
        return new DefaultAdapterDataObserver() {
            public void onItemRangeInserted(int positionStart, int itemCount) {
                invalidateBoundAdsViewHolders();
                notifyItemRangeInserted(positionStart, itemCount);
            }

            public void onItemRangeRemoved(int positionStart, int itemCount) {
                invalidateBoundAdsViewHolders();
                notifyItemRangeRemoved(positionStart, itemCount);
            }

            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                invalidateBoundAdsViewHolders();
                notifyItemMoved(fromPosition, toPosition);
            }

            private void invalidateBoundAdsViewHolders() {
                boundAdsViewHolders.stream()
                        .filter(holder -> holder.getAdapterPosition() >= getItemCount())
                        .forEach(holder -> notifyItemRemoved(holder.getAdapterPosition()));
            }
        };
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        AdsManager.getInstance().registerNativeAdsAdapter(this);
        rcv = recyclerView;
        nativeAdsPool = new NativeAdsPool(recyclerView.getContext(), adsUnitId);
        nativeAdsPool.loadAds(adsCacheSize);
        nativeAdsPool.setOnPoolRefreshedListener(this::updateNativeAdsHolders);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        AdsManager.getInstance().unregisterNativeAdsAdapter(this);
        nativeAdsPool.setOnPoolRefreshedListener(null);
        nativeAdsPool.clearAds();
        nativeAdsPool.dispose();
        nativeAdsPool = null;
        rcv = null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ADS) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new AdViewHolder(inflater,
                    parent,
                    templateLayoutRes,
                    itemLoadingLayoutRes,
                    itemContainerLayoutRes,
                    itemContainerId);
        }
        return super.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (holder instanceof AdViewHolder) {
            AdViewHolder adViewHolder = (AdViewHolder) holder;
            if (adViewHolder.isUnbindAds()) {
                adViewHolder.setNativeAd(rcv.getLayoutManager(),
                        nativeAdsPool.isLoading(),
                        nativeAdsPool.get(),
                        templateStyle
                );
            }
            if (holder.isRecyclable()) {
                boundAdsViewHolders.add((AdViewHolder) holder);
            }
            return;
        }
        super.onBindViewHolder(holder, convertAdPositionToOriginPosition(position));
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        if (holder instanceof AdViewHolder) {
            boundAdsViewHolders.remove(holder);
        }
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemViewType(int position) {
        if (isAdPosition(position)) {
            return TYPE_ADS;
        }
        return super.getItemViewType(convertAdPositionToOriginPosition(position));
    }

    @Override
    public int getItemCount() {
        if (isAdsUnavailable()) {
            return super.getItemCount();
        }
        int realCount = super.getItemCount();
        return realCount + (realCount + adsItemOffset) / adsItemInterval;
    }

    public int convertAdPositionToOriginPosition(int position) {
        if (isAdsUnavailable()) {
            return position;
        }
        return position - getTotalAdsItemBeforePosition(position);
    }

    public int getTotalAdsItemBeforePosition(int position) {
        if (isAdsUnavailable()) {
            return 0;
        }
        return (position + 1 + adsItemOffset) / (adsItemInterval + 1);
    }

    public boolean isAdPosition(int position) {
        if (isAdsUnavailable()) {
            return false;
        }
        return (position + 1 + adsItemOffset) % (adsItemInterval + 1) == 0;
    }

    public Set<AdViewHolder> getBoundAdsViewHolders() {
        return Collections.unmodifiableSet(boundAdsViewHolders);
    }

    public void loadAdsIfNecessary() {
        if (isAdsUnavailable()) {
            return;
        }
        if (nativeAdsPool.size() >= adsCacheSize) {
            return;
        }
        nativeAdsPool.loadAds(adsCacheSize);
    }

    public void updateNativeAdsHolders() {
        if (isAdsUnavailable()) {
            return;
        }
        boundAdsViewHolders.stream()
                .filter(AdViewHolder::isUnbindAds)
                .forEach(holder -> holder.setNativeAd(
                        rcv.getLayoutManager(),
                        nativeAdsPool.isLoading(),
                        nativeAdsPool.get(),
                        templateStyle
                ));
    }

    public boolean isAdsUnavailable() {
        return nativeAdsPool == null || nativeAdsPool.isAdsUnavailable();
    }

    public static class AdViewHolder extends ViewHolder {

        private final int itemViewHeight;
        private final View loadingView;
        private final NativeTemplateView templateView;

        private AdViewHolder(LayoutInflater inflater,
                             ViewGroup parent,
                             int templateLayoutRes,
                             int itemLoadingLayoutRes,
                             int itemContainerLayoutRes,
                             int itemContainerId) {
            super(inflate(inflater, parent, itemContainerLayoutRes));
            itemViewHeight = itemView.getLayoutParams().height;
            ViewGroup container = itemView.findViewById(itemContainerId);
            container.addView(loadingView = inflate(inflater, container, itemLoadingLayoutRes));
            container.addView(templateView = new NativeTemplateView(itemView.getContext(), templateLayoutRes),
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            showLoadingView();
        }

        private void setNativeAd(LayoutManager layoutManager,
                                 boolean loading,
                                 NativeAd nativeAd,
                                 NativeTemplateStyle templateStyle) {
            if (nativeAd != null) {
                showTemplateView();
                templateView.setNativeAd(nativeAd);
                templateView.setStyles(templateStyle);
            } else {
                showLoadingView();
            }
            if (loading) {
                return;
            }
            boolean hide;
            if (nativeAd == null) {
                if (layoutManager instanceof GridLayoutManager) {
                    GridLayoutManager g = (GridLayoutManager) layoutManager;
                    int spanCount = g.getSpanCount();
                    hide = spanCount == g.getSpanSizeLookup().getSpanSize(getAdapterPosition());
                } else {
                    hide = layoutManager instanceof LinearLayoutManager;
                }
            } else {
                hide = false;
            }
            if (hide) {
                setItemViewHeight(0);
            } else {
                setItemViewHeight(itemViewHeight);
            }
        }

        public boolean isUnbindAds() {
            return templateView.getVisibility() != View.VISIBLE;
        }

        public void showLoadingView() {
            templateView.setVisibility(View.INVISIBLE);
            loadingView.setVisibility(View.VISIBLE);
        }

        public void showTemplateView() {
            templateView.setVisibility(View.VISIBLE);
            loadingView.setVisibility(View.INVISIBLE);
        }

        private void setItemViewHeight(int height) {
            if (itemView.getLayoutParams().height != height) {
                itemView.getLayoutParams().height = height;
                itemView.requestLayout();
            }
        }

        private static View inflate(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, int res) {
            return inflater.inflate(res, parent, false);
        }
    }

    public static class Builder {

        private RecyclerView.Adapter<?> adapter;
        private String adsUnitId;
        private int adsCacheSize;
        private int adsItemInterval;
        private int adsItemOffset;
        private NativeTemplateStyle templateStyle;
        private int templateLayoutRes;
        private int itemLoadingLayoutRes;
        private int itemContainerLayoutRes;
        private int itemContainerId;

        /**
         * @param adapter      adapter will be wrapped
         * @param adsUnitId    your ads unit id
         * @param adsCacheSize how many ads will be load
         */
        public Builder(RecyclerView.Adapter<?> adapter, String adsUnitId, int adsCacheSize) {
            this.adapter = adapter;
            this.adsUnitId = adsUnitId;
            this.adsCacheSize = adsCacheSize;
            this.templateStyle = null;
            this.templateLayoutRes = R.layout.gnt_template_view_medium_1;
            this.itemLoadingLayoutRes = R.layout.gnt_item_loading;
            this.itemContainerLayoutRes = R.layout.gnt_item_container;
            this.itemContainerId = R.id.gnt_container;
            this.adsItemInterval = DEFAULT_AD_ITEM_INTERVAL;
            this.adsItemOffset = DEFAULT_AD_ITEM_OFFSET;
        }

        public Builder setAdapter(RecyclerView.Adapter<? extends ViewHolder> adapter) {
            this.adapter = adapter;
            return this;
        }

        public Builder setAdsUnitId(String adsUnitId) {
            this.adsUnitId = adsUnitId;
            return this;
        }

        public Builder setAdsCacheSize(int adsCacheSize) {
            this.adsCacheSize = adsCacheSize;
            return this;
        }

        public Builder setAdsItemConfig(int adsItemInterval, int adsItemOffset) {
            if (adsItemInterval < 0) {
                throw new IllegalArgumentException("adsItemInterval must be greater than 0");
            }
            adsItemOffset = MathUtils.clamp(adsItemOffset, 0, adsItemInterval);
            this.adsItemInterval = adsItemInterval;
            this.adsItemOffset = adsItemInterval - adsItemOffset;
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

        public Builder setTemplateLayoutRes(@LayoutRes int templateLayoutRes) {
            this.templateLayoutRes = templateLayoutRes;
            return this;
        }

        public Builder setItemLoadingLayoutRes(@LayoutRes int itemLoadingLayoutRes) {
            this.itemLoadingLayoutRes = itemLoadingLayoutRes;
            return this;
        }

        public Builder setItemContainerLayoutRes(@LayoutRes int itemContainerLayoutRes) {
            this.itemContainerLayoutRes = itemContainerLayoutRes;
            return this;
        }

        public Builder setItemContainerId(@IdRes int itemContainerId) {
            this.itemContainerId = itemContainerId;
            return this;
        }

        public NativeAdsAdapter build() {
            return new NativeAdsAdapter(this);
        }

    }
}
