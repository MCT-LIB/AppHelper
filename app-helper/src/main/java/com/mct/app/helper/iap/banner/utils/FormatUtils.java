package com.mct.app.helper.iap.banner.utils;

import androidx.annotation.NonNull;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FormatUtils {


    @NonNull
    public static String formatTime(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }


    @NonNull
    public static String formatMoney(float value, String priceCurrencyCode) {
        return formatMoney(value, priceCurrencyCode, findLocale(priceCurrencyCode, Locale.getDefault(Locale.Category.FORMAT)));
    }

    @NonNull
    public static String formatMoney(float value, String priceCurrencyCode, Locale locale) {
        if (value < 0) {
            return "";
        }
        try {
            Currency currency = Currency.getInstance(priceCurrencyCode);
            NumberFormat numberFormat = NumberFormat.getCurrencyInstance(locale);
            numberFormat.setRoundingMode(RoundingMode.HALF_EVEN);
            numberFormat.setCurrency(currency);
            adjustDefaultFractionDigits(numberFormat, currency);
            return numberFormat.format(value);
        } catch (Exception e) {
            return "";
        }
    }

    private static final Map<String, Locale> sLocaleCache = new HashMap<>();

    private static Locale findLocale(String priceCurrencyCode, Locale fallback) {
        for (Map.Entry<String, Locale> entry : sLocaleCache.entrySet()) {
            if (entry.getKey().equals(priceCurrencyCode) && entry.getValue() != null) {
                return entry.getValue();
            }
        }
        Currency c = Currency.getInstance(priceCurrencyCode);
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
        numberFormat.setCurrency(c);
        String formattedValue = numberFormat.format(1).replaceAll("[\\d\\s.,]", "");
        for (Locale locale : Locale.getAvailableLocales()) {
            try {
                Currency currency = Currency.getInstance(locale);
                if (formattedValue.contains(currency.getSymbol())) {
                    sLocaleCache.put(priceCurrencyCode.toLowerCase(), locale);
                    return locale;
                }
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    private static void adjustDefaultFractionDigits(@NonNull NumberFormat numberFormat, Currency defaultCurrency) {
        Currency currency = numberFormat.getCurrency() == null ? defaultCurrency : numberFormat.getCurrency();
        if (currency != null) {
            int digits = currency.getDefaultFractionDigits();
            if (digits != -1) {
                int oldMinDigits = numberFormat.getMinimumFractionDigits();
                // Common patterns are "#.##", "#.00", "#".
                // Try to adjust all of them in a reasonable way.
                if (oldMinDigits == numberFormat.getMaximumFractionDigits()) {
                    numberFormat.setMinimumFractionDigits(digits);
                    numberFormat.setMaximumFractionDigits(digits);
                } else {
                    numberFormat.setMinimumFractionDigits(Math.min(digits, oldMinDigits));
                    numberFormat.setMaximumFractionDigits(digits);
                }
            }
        }
    }

    private FormatUtils() {
        //no instance
    }
}
