package com.mct.app.helper;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.MaterialColors;
import com.mct.app.helper.admob.AdsManager;

public class MainActivity extends AppCompatActivity {

    private ViewGroup container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(MaterialColors.getColor(this, android.R.attr.colorBackground, 0));
        setContentView(R.layout.activity_main);

        container = findViewById(R.id.frame_container);
        findViewById(R.id.btn_show_banner).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_hide_banner).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_native).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_hide_native).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_interstitial).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_rewarded).setOnClickListener(this::clickButton);
        findViewById(R.id.btn_show_rewarded_interstitial).setOnClickListener(this::clickButton);

    }

    private void clickButton(@NonNull View view) {
        if (view.getId() == R.id.btn_show_banner) {
            AdsManager.getInstance().show(Constant.BANNER_ID, container);
            return;
        }
        if (view.getId() == R.id.btn_hide_banner) {
            AdsManager.getInstance().hide(Constant.BANNER_ID);
            return;
        }
        if (view.getId() == R.id.btn_show_native) {
            AdsManager.getInstance().show(Constant.NATIVE_ID, container);
            return;
        }
        if (view.getId() == R.id.btn_hide_native) {
            AdsManager.getInstance().hide(Constant.NATIVE_ID);
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
    }
}