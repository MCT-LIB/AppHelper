package com.mct.app.helper.native_rcv;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mct.app.helper.Constant;
import com.mct.app.helper.R;
import com.mct.app.helper.admob.ads.natives.NativeAdsAdapter;
import com.mct.app.helper.admob.ads.natives.NativeTemplate;
import com.mct.app.helper.admob.ads.natives.NativeTemplateStyle;
import com.mct.app.helper.native_rcv.adapter.GridSpacingItemDecoration;
import com.mct.app.helper.native_rcv.adapter.UserAdapter;

import java.util.ArrayList;
import java.util.List;

public class NativeRecyclerActivity extends AppCompatActivity {

    private List<User> users;
    private RecyclerView rcvUser;
    private NativeAdsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native_recycler);

        users = getListUser();
        rcvUser = findViewById(R.id.rcv_user);

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
            UserAdapter userAdapter = new UserAdapter(R.layout.item_user, users, (user, position) -> {
                String msg = "Clicked " + user.getName()
                        + " position in rcv " + position
                        + " position in list " + adapter.convertAdPositionToOriginPosition(position);
                showToast(msg);
                Log.e("ddd", msg);
            });
            adapter = new NativeAdsAdapter.Builder(userAdapter, adsUnitId, numberOfAds)
                    .setNativeTemplate(NativeTemplate.SMALL)
                    .setAdsItemConfig(3, 3)
                    .build();
            rcvUser.setAdapter(adapter);
            rcvUser.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            setItemDecoration(null);
            return;
        }
        if (view.getId() == R.id.btn_style_2) {
            UserAdapter userAdapter = new UserAdapter(R.layout.item_user, users, (user, position) -> {
                String msg = "Clicked " + user.getName()
                        + " position in rcv " + position
                        + " position in list " + adapter.convertAdPositionToOriginPosition(position);
                showToast(msg);
                Log.e("ddd", msg);
            });
            adapter = new NativeAdsAdapter.Builder(userAdapter, adsUnitId, numberOfAds)
                    .setNativeTemplate(NativeTemplate.MEDIUM)
                    .setAdsItemConfig(6, 1)
                    .build();
            rcvUser.setAdapter(adapter);
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
            rcvUser.setLayoutManager(grid);
            setItemDecoration(null);
            return;
        }
        if (view.getId() == R.id.btn_style_3) {
            UserAdapter userAdapter = new UserAdapter(R.layout.item_user_a4, users, (user, position) -> {
                String msg = "Clicked " + user.getName()
                        + " position in rcv " + position
                        + " position in list " + adapter.convertAdPositionToOriginPosition(position);
                showToast(msg);
                Log.e("ddd", msg);
            });
            adapter = new NativeAdsAdapter.Builder(userAdapter, adsUnitId, numberOfAds)
                    .setNativeTemplate(NativeTemplate.SMALL_A4)
                    .setAdsItemConfig(5, 3)
                    .build();
            rcvUser.setAdapter(adapter);
            GridLayoutManager grid = new GridLayoutManager(getApplicationContext(), 2);
            rcvUser.setLayoutManager(grid);
            setItemDecoration(new GridSpacingItemDecoration(2, 24, true, 0));
            return;
        }
        if (view.getId() == R.id.btn_style_4) {
            NativeTemplateStyle style = new NativeTemplateStyle.Builder()
                    .withMainBackgroundColor(Color.parseColor("#dddddd"))
                    .withCallToActionBackgroundColor(Color.parseColor("#ff0063"))
                    .withCallToActionCornerRadius(16)
                    .build();
            UserAdapter userAdapter = new UserAdapter(R.layout.item_user_square, users, (user, position) -> {
                String msg = "Clicked " + user.getName()
                        + " position in rcv " + position
                        + " position in list " + adapter.convertAdPositionToOriginPosition(position);
                showToast(msg);
                Log.e("ddd", msg);
            });
            adapter = new NativeAdsAdapter.Builder(userAdapter, adsUnitId, numberOfAds)
                    .setNativeTemplate(NativeTemplate.SMALL_SQUARE)
                    .setNativeTemplateStyle(style)
                    .setAdsItemConfig(5, 3)
                    .build();
            rcvUser.setAdapter(adapter);
            GridLayoutManager grid = new GridLayoutManager(getApplicationContext(), 2);
            rcvUser.setLayoutManager(grid);
            setItemDecoration(new GridSpacingItemDecoration(2, 24, true, 0));
            return;
        }
    }

    private void setItemDecoration(RecyclerView.ItemDecoration decoration) {
        removeAllItemDecoration();
        if (decoration != null) {
            rcvUser.addItemDecoration(decoration);
            rcvUser.invalidateItemDecorations();
        }
    }

    private void removeAllItemDecoration() {
        for (int i = 0; i < rcvUser.getItemDecorationCount(); i++) {
            rcvUser.removeItemDecorationAt(i);
        }
    }

    private Toast toast;

    private void showToast(String message) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        toast.show();
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
