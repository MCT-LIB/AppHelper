package com.mct.app.helper.native_rcv;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mct.app.helper.Constant;
import com.mct.app.helper.R;
import com.mct.app.helper.admob.ads.natives.NativeTemplate;
import com.mct.app.helper.admob.ads.natives.NativeTemplateStyle;
import com.mct.app.helper.admob.ads.natives.adapter.NativeAdsAdapter;
import com.mct.app.helper.admob.ads.natives.adapter.NativeAdsPool;
import com.mct.app.helper.native_rcv.adapter.GridSpacingItemDecoration;
import com.mct.app.helper.native_rcv.adapter.UserAdapter;

import java.util.ArrayList;
import java.util.List;

public class NativeRecyclerActivity extends AppCompatActivity {

    NativeAdsAdapter adapter;
    NativeAdsPool pool;

    private RecyclerView rcvData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native_recycler);

        rcvData = findViewById(R.id.rcv_data);
        pool = new NativeAdsPool(Constant.NATIVE_ID);
        pool.init(getApplicationContext());
        pool.load(3);

        findViewById(R.id.btn_style_1).setOnClickListener(this::onClick);
        findViewById(R.id.btn_style_2).setOnClickListener(this::onClick);
        findViewById(R.id.btn_style_3).setOnClickListener(this::onClick);
        findViewById(R.id.btn_style_4).setOnClickListener(this::onClick);
        findViewById(R.id.btn_style_3).performClick();
    }

    private void onClick(@NonNull View view) {
        if (view.getId() == R.id.btn_style_1) {
            adapter = new NativeAdsAdapter.Builder(new UserAdapter(R.layout.item_user, getListUser()), pool)
                    .setNativeTemplate(NativeTemplate.SMALL)
                    .setAdsItemConfig(3, 3)
                    .build();
            rcvData.setAdapter(adapter);
            rcvData.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            setItemDecoration(null);
            return;
        }
        if (view.getId() == R.id.btn_style_2) {
            adapter = new NativeAdsAdapter.Builder(new UserAdapter(R.layout.item_user, getListUser()), pool)
                    .setNativeTemplate(NativeTemplate.MEDIUM)
                    .setAdsItemConfig(6, 1)
                    .build();
            rcvData.setAdapter(adapter);
            GridLayoutManager grid = new GridLayoutManager(getApplicationContext(), 2);
            grid.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (adapter.isAdsItem(position)) {
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
            adapter = new NativeAdsAdapter.Builder(new UserAdapter(R.layout.item_user_a4, getListUser()), pool)
                    .setNativeTemplate(NativeTemplate.SMALL_A4)
                    .setAdsItemConfig(5, 3)
                    .build();
            rcvData.setAdapter(adapter);
            GridLayoutManager grid = new GridLayoutManager(getApplicationContext(), 2);
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
            adapter = new NativeAdsAdapter.Builder(new UserAdapter(R.layout.item_user_square, getListUser()), pool)
                    .setNativeTemplate(NativeTemplate.SMALL_SQUARE)
                    .setNativeTemplateStyle(style)
                    .setAdsItemConfig(5, 3)
                    .build();
            rcvData.setAdapter(adapter);
            GridLayoutManager grid = new GridLayoutManager(getApplicationContext(), 2);
            rcvData.setLayoutManager(grid);
            setItemDecoration(new GridSpacingItemDecoration(2, 24, true, 0));
            return;
        }
    }

    private void setItemDecoration(RecyclerView.ItemDecoration decoration) {
        removeAllItemDecoration();
        if (decoration != null) {
            rcvData.addItemDecoration(decoration);
            rcvData.invalidateItemDecorations();
        }
    }

    private void removeAllItemDecoration() {
        for (int i = 0; i < rcvData.getItemDecorationCount(); i++) {
            rcvData.removeItemDecorationAt(i);
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
