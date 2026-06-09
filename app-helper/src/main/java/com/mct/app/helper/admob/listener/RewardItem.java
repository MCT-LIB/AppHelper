package com.mct.app.helper.admob.listener;

import androidx.annotation.NonNull;

/**
 * Immutable reward granted to the user by a rewarded ad. Wraps AdMob's
 * {@link com.google.android.gms.ads.rewarded.RewardItem} ({@code type} / {@code amount}).
 */
public final class RewardItem {

    private final String type;
    private final int amount;

    public RewardItem(String type, int amount) {
        this.type = type;
        this.amount = amount;
    }

    @NonNull
    public static RewardItem of(@NonNull com.google.android.gms.ads.rewarded.RewardItem item) {
        return new RewardItem(item.getType(), item.getAmount());
    }

    /** The reward type, e.g. "coins". */
    public String getType() {
        return type;
    }

    /** The reward amount. */
    public int getAmount() {
        return amount;
    }

    @NonNull
    @Override
    public String toString() {
        return "RewardItem{type='" + type + "', amount=" + amount + "}";
    }
}
