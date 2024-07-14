package com.mct.app.helper.demo;

import android.content.Context;
import android.graphics.Color;
import android.view.ContextThemeWrapper;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.mct.app.helper.iap.banner.Customizer;
import com.mct.app.helper.iap.banner.IapBanner;
import com.mct.app.helper.iap.banner.IapBannerBuilder;
import com.mct.app.helper.iap.banner.component.billing.BillingPeriod;
import com.mct.app.helper.iap.banner.component.billing.ProductPriceInfo;
import com.mct.app.helper.iap.banner.component.normal.Component;
import com.mct.app.helper.iap.banner.component.normal.CompositeComponent;

public class Paywall {

    public static void show(Context context) {
        new Paywall(context).show();
    }

    private final ProductPriceInfo productYear = ProductPriceInfo.fromPriceAmount(
            0,
            999_999,
            "VND",
            BillingPeriod.YEAR
    );
    private final ProductPriceInfo productQuarter = ProductPriceInfo.fromPriceAmount(
            0,
            222_222,
            "VND",
            BillingPeriod.QUARTER
    );
    private final ProductPriceInfo productMonth = ProductPriceInfo.fromPriceAmount(
            0,
            66_666,
            "VND",
            BillingPeriod.MONTH
    );

    private final Context context;
    private IapBanner banner;

    private CompositeComponent<?> selectedComponent;
    private CompositeComponent<?> yearComponent;
    private CompositeComponent<?> quarterComponent;
    private CompositeComponent<?> monthComponent;

    private Paywall(Context context) {
        this.context = new ContextThemeWrapper(context, com.google.android.material.R.style.Theme_Material3_Light);
    }

    public void show() {
        dismiss();
        banner = createBanner();
        yearComponent = banner.findComponentById(R.id.card_year);
        quarterComponent = banner.findComponentById(R.id.card_quarter);
        monthComponent = banner.findComponentById(R.id.card_month);

        banner.show(true);
    }

    private void dismiss() {
        if (banner != null) {
            banner.dismiss();
        }
    }

    private IapBanner createBanner() {

        String priceYear = productYear.getRealPrice();
        String priceQuarter = productQuarter.getRealPrice();
        String priceMonth = productMonth.getRealPrice();

        return new IapBannerBuilder(context, R.layout.iap_banner_paywall)
                .bindViewGroup(R.id.card_year, cc -> cc
                        .bindView(R.id.card_radio_button, Customizer.withDefaults())
                        .bindText(R.id.card_title, tv -> tv.text("Yearly"))
                        .bindText(R.id.card_price, tv -> tv.text(priceYear).bold().underline().highlight(Color.RED))
                        .setClickListener(v -> {
                            setSelectComponent(cc);
                            showToast("Click card_year!");
                        }))
                .bindViewGroup(R.id.card_quarter, cc -> cc
                        .bindView(R.id.card_radio_button, Customizer.withDefaults())
                        .bindText(R.id.card_title, tv -> tv.text("Quarterly"))
                        .bindText(R.id.card_price, tv -> tv.text(priceQuarter))
                        .setClickListener(v -> {
                            setSelectComponent(cc);
                            showToast("Click card_quarter!");
                        }))
                .bindViewGroup(R.id.card_month, cc -> cc
                        .bindView(R.id.card_radio_button, Customizer.withDefaults())
                        .bindText(R.id.card_title, tv -> tv.text("Monthly"))
                        .bindText(R.id.card_price, tv -> tv.text(priceMonth))
                        .setClickListener(v -> {
                            setSelectComponent(cc);
                            showToast("Click card_month!");
                        }))
                .bindDismiss(R.id.btn_close).and()
                .bindStatusBar(bar -> bar
                        .backgroundTransparent()
                        .darkAppearance())
                .bindNavigationBar(bar -> bar
                        .backgroundTransparent()
                        .darkAppearance())
                .setOnBannerShowListener((banner, dialog) -> {
                    setSelectComponent(yearComponent);
                    showToast("On show dialog");
                })
                .setOnBannerDismissListener((banner, dialog) -> {
                    showToast("On dismiss dialog");
                })
                .build();
    }

    private void setSelectComponent(CompositeComponent<?> component) {
        if (component == null || selectedComponent == component) {
            return;
        }
        selectedComponent = component;
        selectComponents(false, yearComponent, quarterComponent, monthComponent);
        selectComponents(true, selectedComponent);
    }

    private void selectComponents(boolean selected, @NonNull CompositeComponent<?>... components) {
        for (CompositeComponent<?> component : components) {
            if (component.getView() instanceof MaterialCardView) {
                setSelectCard((MaterialCardView) component.getView(), selected);
            }
            Component<?> checkBox = component.findComponentById(R.id.card_radio_button);
            if (checkBox != null && checkBox.getView() instanceof RadioButton) {
                setSelectRadioButton((RadioButton) checkBox.getView(), selected);
            }
        }
    }

    private void setSelectCard(MaterialCardView card, boolean selected) {
        if (card == null) {
            return;
        }
        int strokeAttr;
        int backgroundAttr;
        if (selected) {
            strokeAttr = com.google.android.material.R.attr.colorTertiary;
            backgroundAttr = com.google.android.material.R.attr.colorTertiaryContainer;
        } else {
            strokeAttr = com.google.android.material.R.attr.colorOutline;
            backgroundAttr = com.google.android.material.R.attr.colorSurfaceContainerLowest;
        }
        card.setStrokeColor(MaterialColors.getColor(card, strokeAttr));
        card.setCardBackgroundColor(MaterialColors.getColor(card, backgroundAttr));
    }

    private void setSelectRadioButton(RadioButton radioButton, boolean selected) {
        if (radioButton != null) {
            radioButton.setChecked(selected);
        }
    }

    private Toast toast;

    private void showToast(String text) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        toast.show();
    }
}
