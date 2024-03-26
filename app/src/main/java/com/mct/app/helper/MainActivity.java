package com.mct.app.helper;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.MaterialColors;
import com.mct.app.helper.admob.AdsManager;
import com.mct.app.helper.admob.ads.natives.NativeTemplate;
import com.mct.app.helper.native_rcv.NativeRecyclerActivity;

public class MainActivity extends AppCompatActivity {

    private static final String NATIVE_SMALL = "NativeSmall";
    private static final String NATIVE_MEDIUM = "NativeMedium";
    private static final String NATIVE_LARGE = "NativeLarge";

    private ViewGroup container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(MaterialColors.getColor(this, android.R.attr.colorBackground, 0));
        setContentView(R.layout.activity_main);

        AdsManager.getInstance().config(adsConfigurator -> adsConfigurator
                .nativeAds(Constant.NATIVE_ID).template(NativeTemplate.SMALL).alias(NATIVE_SMALL).and()
                .nativeAds(Constant.NATIVE_ID).template(NativeTemplate.MEDIUM).alias(NATIVE_MEDIUM).and()
                .nativeAds(Constant.NATIVE_ID).template(NativeTemplate.LARGE).alias(NATIVE_LARGE).and()
                .apply());

        container = findViewById(R.id.frame_container);
        findViewById(R.id.btn_show_banner).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_banner_collapse).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_hide_banner).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_interstitial).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_rewarded).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_rewarded_interstitial).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_native_small).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_native_medium).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_native_large).setOnClickListener(this::clickButton);
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
            AdsManager.getInstance().show(Constant.BANNER_ID, container);
            return;
        }
        if (view.getId() == R.id.btn_show_banner_collapse) {
            AdsManager.getInstance().forceShow(Constant.BANNER_COLLAPSE_ID, container, false);
            return;
        }
        if (view.getId() == R.id.btn_hide_banner) {
            AdsManager.getInstance().hide(Constant.BANNER_ID);
            AdsManager.getInstance().hide(Constant.BANNER_COLLAPSE_ID);
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
        if (view.getId() == R.id.btn_show_native_small) {
            AdsManager.getInstance().show(NATIVE_SMALL, container);
            return;
        }
        if (view.getId() == R.id.btn_show_native_medium) {
            AdsManager.getInstance().show(NATIVE_MEDIUM, container);
            return;
        }
        if (view.getId() == R.id.btn_show_native_large) {
            AdsManager.getInstance().show(NATIVE_LARGE, container);
            return;
        }
        if (view.getId() == R.id.btn_hide_native) {
            AdsManager.getInstance().hide(NATIVE_SMALL);
            AdsManager.getInstance().hide(NATIVE_MEDIUM);
            AdsManager.getInstance().hide(NATIVE_LARGE);
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
}