package com.mct.app.helper.apprater;

public interface RateDialog {

    void setDontRemindButtonVisible(boolean visible);

    void setCancelable(boolean cancelable);

    void setOnRateNowListener(Runnable listener);

    void setOnRemindLaterListener(Runnable listener);

    void setDontRemindListener(Runnable listener);

    void setOnDismissListener(Runnable listener);

    void show();

    void dismiss();
}
