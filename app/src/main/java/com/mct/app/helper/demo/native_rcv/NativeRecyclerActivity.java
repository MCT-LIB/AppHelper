package com.mct.app.helper.demo.native_rcv;

import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.mct.app.helper.admob.AdsManager;
import com.mct.app.helper.admob.ads.NativeAdsPool;
import com.mct.app.helper.demo.Constant;
import com.mct.app.helper.demo.R;
import com.mct.app.helper.admob.ads.natives.NativeAdsAdapter;
import com.mct.app.helper.admob.ads.natives.NativeTemplate;
import com.mct.app.helper.admob.ads.natives.NativeTemplateStyle;
import com.mct.app.helper.admob.ads.natives.manager.NpaGridLayoutManager;
import com.mct.app.helper.admob.ads.natives.manager.NpaLinearLayoutManager;
import com.mct.app.helper.demo.native_rcv.adapter.GridSpacingItemDecoration;
import com.mct.app.helper.demo.native_rcv.adapter.UserAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NativeRecyclerActivity extends AppCompatActivity {

    NativeAdsAdapter adapter;
    ItemTouchHelper touchHelper;

    RecyclerView rcvData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native_recycler);

        initData();
        initListener();
    }

    private void initData() {
        rcvData = findViewById(R.id.rcv_user);
        touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {

            private boolean isDrawing;

            @Override
            public void onChildDrawOver(@NonNull Canvas c, @NonNull RecyclerView rcv, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (!isDrawing && isCurrentlyActive) {
                    isDrawing = true;
                    viewHolder.itemView.setAlpha(0.8f);
                }
                if (isDrawing && !isCurrentlyActive) {
                    isDrawing = false;
                    viewHolder.itemView.setAlpha(1f);
                }
                super.onChildDrawOver(c, rcv, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public boolean canDropOver(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder current, @NonNull RecyclerView.ViewHolder target) {
                return target.getItemViewType() != NativeAdsAdapter.TYPE_ADS;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView rcv, @NonNull RecyclerView.ViewHolder drag, @NonNull RecyclerView.ViewHolder drop) {
                UserAdapter userAdapter = (UserAdapter) adapter.getWrapped();
                int dragPos = drag.getAdapterPosition();
                int dropPos = drop.getAdapterPosition();

                int dragIndex = dragPos - adapter.getTotalAdsItemBeforePosition(dragPos);
                int dropIndex = dropPos - adapter.getTotalAdsItemBeforePosition(dropPos);
                Collections.swap(userAdapter.getUsers(), dragIndex, dropIndex);
                userAdapter.notifyItemMoved(dragPos, dropPos);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }
        });
        touchHelper.attachToRecyclerView(rcvData);
    }

    private void initListener() {
        findViewById(R.id.btn_style_1).setOnClickListener(this::onClick);
        findViewById(R.id.btn_style_2).setOnClickListener(this::onClick);
        findViewById(R.id.btn_style_3).setOnClickListener(this::onClick);
        findViewById(R.id.btn_style_4).setOnClickListener(this::onClick);
        findViewById(R.id.btn_style_3).performClick();
    }

    private void onClick(@NonNull View view) {
        String adsUnitId = Constant.NATIVE_ID;
        int numberOfAds = 3;
        if (view.getId() == R.id.btn_style_1) {
            adapter = new NativeAdsAdapter.Builder(createUserAdapter(R.layout.item_user), AdsManager.getInstance().getAds("native_pool", NativeAdsPool.class))
                    .setNativeTemplate(NativeTemplate.SMALL)
                    .setAdsItemConfig(3, 3)
                    .build();
            rcvData.setAdapter(adapter);
            rcvData.setLayoutManager(new NpaLinearLayoutManager(getApplicationContext()));
            setItemDecoration(null);
            return;
        }
        if (view.getId() == R.id.btn_style_2) {
            adapter = new NativeAdsAdapter.Builder(createUserAdapter(R.layout.item_user), AdsManager.getInstance().getAds("native_pool", NativeAdsPool.class))
                    .setNativeTemplate(NativeTemplate.MEDIUM_1)
                    .setAdsItemConfig(6, 1)
                    .build();
            rcvData.setAdapter(adapter);
            GridLayoutManager grid = new NpaGridLayoutManager(getApplicationContext(), 2);
            grid.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (adapter.isAdPosition(position)) {
                        return grid.getSpanCount();
                    }
                    return 1;
                }
            });
            rcvData.setLayoutManager(grid);
            setItemDecoration(null);
            return;
        }
        if (view.getId() == R.id.btn_style_3) {
            adapter = new NativeAdsAdapter.Builder(createUserAdapter(R.layout.item_user_a4), AdsManager.getInstance().getAds("native_pool", NativeAdsPool.class))
                    .setNativeTemplate(NativeTemplate.SMALL_A4)
                    .setAdsItemConfig(5, 3)
                    .build();
            rcvData.setAdapter(adapter);
            GridLayoutManager grid = new NpaGridLayoutManager(getApplicationContext(), 2);
            rcvData.setLayoutManager(grid);
            setItemDecoration(new GridSpacingItemDecoration(2, 24, true, 0));
            return;
        }
        if (view.getId() == R.id.btn_style_4) {
            NativeTemplateStyle style = new NativeTemplateStyle.Builder()
                    .withMainBackgroundColor(Color.parseColor("#dddddd"))
                    .withCallToActionBackgroundColor(Color.parseColor("#ff0063"))
                    .withCallToActionCornerRadius(16)
                    .build();
            adapter = new NativeAdsAdapter.Builder(createUserAdapter(R.layout.item_user_square), AdsManager.getInstance().getAds("native_pool", NativeAdsPool.class))
                    .setNativeTemplate(NativeTemplate.SMALL_SQUARE)
                    .setNativeTemplateStyle(style)
                    .setAdsItemConfig(5, 3)
                    .build();
            rcvData.setAdapter(adapter);
            GridLayoutManager grid = new NpaGridLayoutManager(getApplicationContext(), 2);
            rcvData.setLayoutManager(grid);
            setItemDecoration(new GridSpacingItemDecoration(2, 24, true, 0));
            return;
        }
    }

    @NonNull
    private UserAdapter createUserAdapter(int layoutId) {
        return new UserAdapter(layoutId, touchHelper, getListUser());
    }

    private void setItemDecoration(RecyclerView.ItemDecoration decoration) {
        removeGridDecoration();
        if (decoration != null) {
            rcvData.addItemDecoration(decoration);
            rcvData.invalidateItemDecorations();
        }
    }

    private void removeGridDecoration() {
        for (int i = 0; i < rcvData.getItemDecorationCount(); i++) {
            if (rcvData.getItemDecorationAt(i) instanceof GridSpacingItemDecoration) {
                rcvData.removeItemDecorationAt(i);
            }
        }
    }

    @NonNull
    private List<User> getListUser() {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            users.add(new User(i));
        }
        return users;
    }
}
