package com.mct.app.helper.admob.ads.natives.adapter;

import static androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;

import android.annotation.SuppressLint;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

@SuppressWarnings("rawtypes,unchecked")
class RecyclerViewAdapterWrapper extends RecyclerView.Adapter {

    private final RecyclerView.Adapter wrapped;

    public RecyclerViewAdapterWrapper(RecyclerView.Adapter wrapped) {
        this.wrapped = wrapped;
        this.wrapped.registerAdapterDataObserver(new AdapterDataObserver() {
            @SuppressLint("NotifyDataSetChanged")
            public void onChanged() {
                notifyDataSetChanged();
            }

            public void onItemRangeChanged(int positionStart, int itemCount) {
                notifyItemRangeChanged(positionStart, itemCount);
            }

            public void onItemRangeInserted(int positionStart, int itemCount) {
                notifyItemRangeInserted(positionStart, itemCount);
            }

            public void onItemRangeRemoved(int positionStart, int itemCount) {
                notifyItemRangeRemoved(positionStart, itemCount);
            }

            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                notifyItemMoved(fromPosition, toPosition);
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return wrapped.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        wrapped.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount() {
        return wrapped.getItemCount();
    }

    @Override
    public int getItemViewType(int position) {
        return wrapped.getItemViewType(position);
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        wrapped.setHasStableIds(hasStableIds);
    }

    @Override
    public long getItemId(int position) {
        return wrapped.getItemId(position);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        wrapped.onViewRecycled(holder);
    }

    @Override
    public boolean onFailedToRecycleView(@NonNull ViewHolder holder) {
        return wrapped.onFailedToRecycleView(holder);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        wrapped.onViewAttachedToWindow(holder);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        wrapped.onViewDetachedFromWindow(holder);
    }

    @Override
    public void registerAdapterDataObserver(@NonNull AdapterDataObserver observer) {
        wrapped.registerAdapterDataObserver(observer);
    }

    @Override
    public void unregisterAdapterDataObserver(@NonNull AdapterDataObserver observer) {
        wrapped.unregisterAdapterDataObserver(observer);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        wrapped.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        wrapped.onDetachedFromRecyclerView(recyclerView);
    }

}