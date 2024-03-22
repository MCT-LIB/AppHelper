package com.mct.app.helper.iap.banner.component.normal;

import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.mct.app.helper.iap.banner.IapBanner;
import com.mct.app.helper.iap.banner.countdown.Countdown;
import com.mct.app.helper.iap.banner.utils.FormatUtils;

/**
 * TimeComponent - A component for displaying countdown time within an IapBanner.
 * <p>
 * The TimeComponent class allows you to display countdown time within an IapBanner. You can specify
 * the time unit to display (hour, minute, second, or millisecond), and the component will
 * automatically update its text based on the countdown provided by the IapBanner.
 */
public class TimeComponent<C extends TimeComponent<C>> extends TextComponent<C>
        implements Countdown.CountDownListener, Countdown.CountDownMilliListener {

    // Constants for time style flags
    @IntDef({TimeStyle.UNSET, TimeStyle.HOUR, TimeStyle.MINUTE, TimeStyle.SECOND, TimeStyle.MILLISECOND})
    private @interface TimeStyle {
        int UNSET = 0;
        int HOUR = 1;
        int MINUTE = 2;
        int SECOND = 3;
        int MILLISECOND = 4;
    }

    private @TimeStyle int style;

    /**
     * {@inheritDoc}
     */
    public TimeComponent(int id) {
        super(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(@NonNull IapBanner banner, View root) {
        super.init(banner, root);
        if (banner.getCountdown() != null) {
            if (isMillisecond()) {
                banner.getCountdown().addListener((Countdown.CountDownMilliListener) this);
            } else {
                banner.getCountdown().addListener((Countdown.CountDownListener) this);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release(@NonNull IapBanner banner, View root) {
        super.release(banner, root);
        if (banner.getCountdown() != null) {
            banner.getCountdown().removeListener((Countdown.CountDownListener) this);
            banner.getCountdown().removeListener((Countdown.CountDownMilliListener) this);
        }
    }

    /**
     * Handle the countdown event and update the displayed time.
     *
     * @param hour   The remaining hours.
     * @param minute The remaining minutes.
     * @param second The remaining seconds.
     */
    @Override
    public void onCountDown(int hour, int minute, int second) {
        int time;
        switch (style) {
            case TimeStyle.HOUR:
                time = hour;
                break;
            case TimeStyle.MINUTE:
                time = minute;
                break;
            case TimeStyle.SECOND:
                time = second;
                break;
            case TimeStyle.MILLISECOND:
            case TimeStyle.UNSET:
            default:
                return;
        }
        text(FormatUtils.formatTime(time)).setText();
    }

    /**
     * Handle the countdown event for milliseconds and update the displayed time.
     *
     * @param mill The remaining milliseconds.
     */
    @Override
    public void onCountDown(int mill) {
        text(FormatUtils.formatTime(mill)).setText();
    }

    /**
     * @return style of time component
     */
    public int getStyle() {
        return style;
    }

    /**
     * Check if the time style is set to display milliseconds.
     *
     * @return True if the time style is milliseconds; otherwise, false.
     */
    public boolean isMillisecond() {
        return style == TimeStyle.MILLISECOND;
    }

    /**
     * Set the time style to display hours.
     *
     * @return The TimeComponent instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C hour() {
        this.style = TimeStyle.HOUR;
        return (C) this;
    }

    /**
     * Set the time style to display minutes.
     *
     * @return The TimeComponent instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C minute() {
        this.style = TimeStyle.MINUTE;
        return (C) this;
    }

    /**
     * Set the time style to display seconds.
     *
     * @return The TimeComponent instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C second() {
        this.style = TimeStyle.SECOND;
        return (C) this;
    }

    /**
     * Set the time style to display milliseconds.
     *
     * @return The TimeComponent instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C millisecond() {
        this.style = TimeStyle.MILLISECOND;
        return (C) this;
    }

}
