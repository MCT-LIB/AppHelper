package com.mct.app.helper.admob.listener;

/**
 * Full-screen lifecycle ({@link AdCallback}) plus the rewarded-specific
 * earned-reward event. Because it extends {@link AdCallback}, the show machinery
 * is identical; the rewarded show path additionally fires
 * {@link #onUserEarnedReward(RewardItem)}.
 * <p>
 * Using a distinct callback <em>type</em> (rather than an extra parameter) is what
 * selects rewarded behaviour, so a rewarded show can never be confused with a
 * plain full-screen show.
 */
public interface RewardedAdCallback extends AdCallback {

    /**
     * The user earned the reward. Fires BEFORE {@link #onDismissed()}. May not
     * fire at all if the user closes the ad early.
     */
    default void onUserEarnedReward(RewardItem reward) {
    }
}
