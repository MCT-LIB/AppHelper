package com.mct.app.helper;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.mct.app.helper.admob.AdsManager;
import com.mct.app.helper.admob.ads.natives.NativeTemplate;
import com.mct.app.helper.admob.utils.BannerCollapseUtils;
import com.mct.app.helper.native_rcv.NativeRecyclerActivity;

public class MainActivity extends AppCompatActivity {

    private static final String NATIVE_EXTRA_LARGE = "NATIVE_EXTRA_LARGE";
    private static final String NATIVE_LARGE_1 = "NATIVE_LARGE_1";
    private static final String NATIVE_LARGE_2 = "NATIVE_LARGE_2";
    private static final String NATIVE_MEDIUM_1 = "NATIVE_MEDIUM_1";
    private static final String NATIVE_MEDIUM_2 = "NATIVE_MEDIUM_2";
    private static final String NATIVE_SMALL = "NATIVE_SMALL";
    private static final String SMALL_A4 = "SMALL_A4";
    private static final String SMALL_RECT = "SMALL_RECT";
    private static final String SMALL_SQUARE = "SMALL_SQUARE";

    private ViewGroup container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AdsManager.getInstance().config(adsConfigurator -> adsConfigurator
                .nativeAds(Constant.NATIVE_ID).template(NativeTemplate.EXTRA_LARGE).alias(NATIVE_EXTRA_LARGE).and()
                .nativeAds(Constant.NATIVE_ID).template(NativeTemplate.LARGE_1).alias(NATIVE_LARGE_1).and()
                .nativeAds(Constant.NATIVE_ID).template(NativeTemplate.LARGE_2).alias(NATIVE_LARGE_2).and()
                .nativeAds(Constant.NATIVE_ID).template(NativeTemplate.MEDIUM_1).alias(NATIVE_MEDIUM_1).and()
                .nativeAds(Constant.NATIVE_ID).template(NativeTemplate.MEDIUM_2).alias(NATIVE_MEDIUM_2).and()
                .nativeAds(Constant.NATIVE_ID).template(NativeTemplate.SMALL).alias(NATIVE_SMALL).and()
                .nativeAds(Constant.NATIVE_ID).template(NativeTemplate.SMALL_A4).alias(SMALL_A4).and()
                .nativeAds(Constant.NATIVE_ID).template(NativeTemplate.SMALL_RECT).alias(SMALL_RECT).and()
                .nativeAds(Constant.NATIVE_ID).template(NativeTemplate.SMALL_SQUARE).alias(SMALL_SQUARE).and()
                .apply());

        container = findViewById(R.id.frame_container);
        findViewById(R.id.btn_show_banner).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_banner_collapse).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_hide_banner).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_interstitial).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_rewarded).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_rewarded_interstitial).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_native_extra_large).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_native_large_1).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_native_large_2).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_native_medium_1).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_native_medium_2).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_native_small).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_native_small_a4).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_native_small_rect).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_native_small_square).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_hide_native).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_paywall).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_test_native_recycler).setOnClickListener(this::clickButton);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        container.getHandler().removeCallbacksAndMessages(null);
    }

    private void clickButton(@NonNull View view) {
        if (view.getId() == R.id.btn_show_banner) {
            setContainerSmall(false, false);
            AdsManager.getInstance().show(Constant.BANNER_ID, container);
            return;
        }
        if (view.getId() == R.id.btn_show_banner_collapse) {
            setContainerSmall(false, false);
            AdsManager.getInstance().forceShow(Constant.BANNER_COLLAPSE_ID, container, false);
            return;
        }
        if (view.getId() == R.id.btn_hide_banner) {
            AdsManager.getInstance().hide(Constant.BANNER_ID);
            AdsManager.getInstance().hide(Constant.BANNER_COLLAPSE_ID);
            BannerCollapseUtils.dismissBannerCollapsePopups();
            return;
        }
        if (view.getId() == R.id.btn_show_interstitial) {
            AdsManager.getInstance().show(Constant.INTERSTITIAL_ID, this, null);
            return;
        }
        if (view.getId() == R.id.btn_show_rewarded) {
            AdsManager.getInstance().show(Constant.REWARDED_ID, this, null, () -> {
                Toast.makeText(this, "Earned reward NORMAL", Toast.LENGTH_SHORT).show();
            });
            return;
        }
        if (view.getId() == R.id.btn_show_rewarded_interstitial) {
            AdsManager.getInstance().show(Constant.REWARDED_INTERSTITIAL_ID, this, null, () -> {
                Toast.makeText(this, "Earned reward INTERSTITIAL", Toast.LENGTH_SHORT).show();
            });
        }
        if (view.getId() == R.id.btn_show_native_extra_large) {
            setContainerSmall(false, true);
            AdsManager.getInstance().show(NATIVE_EXTRA_LARGE, container);
            return;
        }
        if (view.getId() == R.id.btn_show_native_large_1) {
            setContainerSmall(false, true);
            AdsManager.getInstance().show(NATIVE_LARGE_1, container);
            return;
        }
        if (view.getId() == R.id.btn_show_native_large_2) {
            setContainerSmall(false, true);
            AdsManager.getInstance().show(NATIVE_LARGE_2, container);
            return;
        }
        if (view.getId() == R.id.btn_show_native_medium_1) {
            setContainerSmall(false, true);
            AdsManager.getInstance().show(NATIVE_MEDIUM_1, container);
            return;
        }
        if (view.getId() == R.id.btn_show_native_medium_2) {
            setContainerSmall(false, true);
            AdsManager.getInstance().show(NATIVE_MEDIUM_2, container);
            return;
        }
        if (view.getId() == R.id.btn_show_native_small) {
            setContainerSmall(false, true);
            AdsManager.getInstance().show(NATIVE_SMALL, container);
            return;
        }
        if (view.getId() == R.id.btn_show_native_small_a4) {
            setContainerSmall(true, true);
            AdsManager.getInstance().show(SMALL_A4, container);
            return;
        }
        if (view.getId() == R.id.btn_show_native_small_rect) {
            setContainerSmall(true, true);
            AdsManager.getInstance().show(SMALL_RECT, container);
            return;
        }
        if (view.getId() == R.id.btn_show_native_small_square) {
            setContainerSmall(true, true);
            AdsManager.getInstance().show(SMALL_SQUARE, container);
            return;
        }
        if (view.getId() == R.id.btn_hide_native) {
            AdsManager.getInstance().hide(NATIVE_EXTRA_LARGE);
            AdsManager.getInstance().hide(NATIVE_LARGE_1);
            AdsManager.getInstance().hide(NATIVE_LARGE_2);
            AdsManager.getInstance().hide(NATIVE_MEDIUM_1);
            AdsManager.getInstance().hide(NATIVE_MEDIUM_2);
            AdsManager.getInstance().hide(NATIVE_SMALL);
            AdsManager.getInstance().hide(SMALL_A4);
            AdsManager.getInstance().hide(SMALL_RECT);
            AdsManager.getInstance().hide(SMALL_SQUARE);
            return;
        }
        if (view.getId() == R.id.btn_show_paywall) {
            Paywall.show(this);
            return;
        }
        if (view.getId() == R.id.btn_test_native_recycler) {
            startActivity(new Intent(getApplicationContext(), NativeRecyclerActivity.class));
        }
    }

    private void setContainerSmall(boolean small, boolean center) {
        ViewGroup.LayoutParams layoutParams = container.getLayoutParams();
        if (small) {
            layoutParams.width = getScreenWidth(getApplicationContext()) / 2;
        } else {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        }
        if (layoutParams instanceof FrameLayout.LayoutParams) {
            ((FrameLayout.LayoutParams) layoutParams).gravity = center
                    ? Gravity.CENTER
                    : Gravity.CENTER | Gravity.BOTTOM;
        }
        container.removeAllViews();
        container.requestLayout();
        BannerCollapseUtils.dismissBannerCollapsePopups();
    }

    private static int getScreenWidth(@NonNull Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return -1;
        }
        Point point = new Point();
        wm.getDefaultDisplay().getRealSize(point);
        return point.x;
    }

}