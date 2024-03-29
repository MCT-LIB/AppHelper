package com.mct.app.helper.apprater;

public interface RateDialog {

    void setOnRateNowListener(Runnable listener);

    void setOnRateLaterListener(Runnable listener);

    void setDontShowAgainListener(Runnable listener);

    void setDontRemindButtonVisible(boolean visible);

    void setCancelable(boolean cancelable);

    void show();

    void dismiss();
}
