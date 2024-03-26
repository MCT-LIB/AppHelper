package com.mct.app.helper.native_rcv;

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
import com.mct.app.helper.admob.ads.natives.adapter.NativeAdsAdapter;
import com.mct.app.helper.admob.ads.natives.adapter.NativeAdsPool;

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

        findViewById(R.id.btn_linear).setOnClickListener(this::onClick);
        findViewById(R.id.btn_grid_1).setOnClickListener(this::onClick);
        findViewById(R.id.btn_grid_2).setOnClickListener(this::onClick);
        findViewById(R.id.btn_linear).performClick();
    }

    private void onClick(View view) {
        if (view.getId() == R.id.btn_linear) {
            adapter = new NativeAdsAdapter.Builder(new UserAdapter(R.layout.item_user, getListUser()), pool)
                    .setNativeTemplate(NativeTemplate.MEDIUM)
                    .setAdsItemConfig(3, 1)
                    .build();
            rcvData.setAdapter(adapter);
            rcvData.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            return;
        }
        if (view.getId() == R.id.btn_grid_1) {
            adapter = new NativeAdsAdapter.Builder(new UserAdapter(R.layout.item_user, getListUser()), pool)
                    .setNativeTemplate(NativeTemplate.MEDIUM)
                    .setAdsItemConfig(6, 1)
                    .build();
            rcvData.setAdapter(adapter);
            GridLayoutManager grid = new GridLayoutManager(getApplicationContext(), 2);
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
            return;
        }
        if (view.getId() == R.id.btn_grid_2) {
            adapter = new NativeAdsAdapter.Builder(new UserAdapter(R.layout.item_user_a4, getListUser()), pool)
                    .setNativeTemplate(NativeTemplate.A4_PAGE)
                    .setAdsItemConfig(5, 3)
                    .build();
            rcvData.setAdapter(adapter);
            GridLayoutManager grid = new GridLayoutManager(getApplicationContext(), 2);
            rcvData.setLayoutManager(grid);
            return;
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
