package com.mct.app.helper.apprater;

import androidx.annotation.NonNull;

public class RateConfig {

    private final String uniqueName;
    private final long timeUntilPrompt;
    private final int launchesUntilPrompt;
    private final long timeUntilPromptForRemindLater;
    private final int launchesUntilPromptForRemindLater;
    private final int maxShownTimes;
    private final boolean isCheckVersionName;
    private final boolean isCheckVersionCode;
    private final boolean isShowDontRemind;
    private final boolean isCancelable;

    private RateConfig(@NonNull Builder builder) {
        uniqueName = builder.uniqueName;
        timeUntilPrompt = builder.timeUntilPrompt;
        launchesUntilPrompt = builder.launchesUntilPrompt;
        timeUntilPromptForRemindLater = builder.timeUntilPromptForRemindLater;
        launchesUntilPromptForRemindLater = builder.launchesUntilPromptForRemindLater;
        maxShownTimes = builder.maxShownTimes;
        isCheckVersionName = builder.isCheckVersionName;
        isCheckVersionCode = builder.isCheckVersionCode;
        isShowDontRemind = builder.isShowDontRemind;
        isCancelable = builder.isCancelable;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public long getTimeUntilPrompt() {
        return timeUntilPrompt;
    }

    public int getLaunchesUntilPrompt() {
        return launchesUntilPrompt;
    }

    public long getTimeUntilPromptForRemindLater() {
        return timeUntilPromptForRemindLater;
    }

    public int getLaunchesUntilPromptForRemindLater() {
        return launchesUntilPromptForRemindLater;
    }

    public int getMaxShownTimes() {
        return maxShownTimes;
    }

    public boolean isCheckVersionName() {
        return isCheckVersionName;
    }

    public boolean isCheckVersionCode() {
        return isCheckVersionCode;
    }

    public boolean isShowDontRemind() {
        return isShowDontRemind;
    }

    public boolean isCancelable() {
        return isCancelable;
    }

    public static class Builder {

        private static final int DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000;
        private final String uniqueName;
        private long timeUntilPrompt = DAY_IN_MILLISECONDS * 3; // default 3 days
        private int launchesUntilPrompt = 7;
        private long timeUntilPromptForRemindLater = DAY_IN_MILLISECONDS * 2; // default 2 days
        private int launchesUntilPromptForRemindLater = 5;
        private int maxShownTimes = -1;
        private boolean isCheckVersionName = false;
        private boolean isCheckVersionCode = false;
        private boolean isShowDontRemind = true;
        private boolean isCancelable = true;

        public Builder(String uniqueName) {
            this.uniqueName = uniqueName;
        }

        public Builder setTimeUntilPrompt(long millisecond) {
            this.timeUntilPrompt = millisecond;
            return this;
        }

        public Builder setLaunchesUntilPrompt(int launches) {
            this.launchesUntilPrompt = launches;
            return this;
        }

        public Builder setTimeUntilPromptForRemindLater(long millisecond) {
            this.timeUntilPromptForRemindLater = millisecond;
            return this;
        }

        public Builder setLaunchesUntilPromptForRemindLater(int launches) {
            this.launchesUntilPromptForRemindLater = launches;
            return this;
        }

        public Builder setMaxShownTimes(int maxShownTimes) {
            this.maxShownTimes = maxShownTimes;
            return this;
        }

        public Builder setCheckVersionName(boolean check) {
            this.isCheckVersionName = check;
            return this;
        }

        public Builder setCheckVersionCode(boolean check) {
            this.isCheckVersionCode = check;
            return this;
        }

        public Builder setShowDontRemind(boolean show) {
            this.isShowDontRemind = show;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            this.isCancelable = cancelable;
            return this;
        }

        public RateConfig build() {
            return new RateConfig(this);
        }
    }

}
