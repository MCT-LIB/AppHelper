package com.mct.app.helper.iap.banner.countdown;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Countdown - A utility class for managing countdown timers.
 * <p>
 * The Countdown class provides functionality for creating and managing countdown timers.
 * You can start, stop, and add listeners to these timers to receive updates as the timer counts down.
 * <p>
 * Usage example:
 * <code>
 * <pre>
 * // Create a Countdown instance and set the initial time (in milliseconds)
 * Countdown countdown = Countdown.newInstance()
 *                              .setTime(60000); // 60 seconds
 *
 * // Add listeners to receive countdown updates
 * countdown.addListener(new Countdown.CountDownListener() {
 *     //@Override
 *     public void onCountDown(int hour, int minute, int second) {
 *         // Handle countdown updates (every 1 second)
 *     }
 * });
 * countdown.addListener(new Countdown.CountDownMilliListener() {
 *     //@Override
 *     public void onCountDown(int mill) {
 *         // Handle countdown updates (every 100 milliseconds)
 *     }
 * });
 *
 * // Start the countdown
 * countdown.startCountDown();
 *
 * // Stop the countdown when needed
 * countdown.stopCountDown();
 * </pre>
 * </code>
 */
public class Countdown {

    // Singleton instance of the Countdown class
    private static volatile Countdown INSTANCE;

    /**
     * Get a singleton instance of the Countdown class.
     *
     * @return The Countdown instance.
     */
    public static Countdown getInstance() {
        if (INSTANCE == null) {
            synchronized (Countdown.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Countdown();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Create a new instance of the Countdown class.
     *
     * @return A new Countdown instance.
     */
    @NonNull
    public static Countdown newInstance() {
        return new Countdown();
    }

    private long initTime;  // Initial time in milliseconds (does not change)
    private long time;      // Current time in milliseconds (changes when countdown is active)
    private boolean isCountDown;

    private final Handler handler;
    private final Runnable countDownRunnable;
    private List<CountDownListener> countDownListeners;
    private List<CountDownMilliListener> countDownMilliListeners;

    /**
     * Create a new Countdown instance.
     */
    private Countdown() {
        handler = new Handler(Looper.getMainLooper());
        countDownRunnable = new Runnable() {
            static final int ONE_SECOND = 1000;

            @Override
            public void run() {
                if (!isCountDown) {
                    return;
                }
                if ((time -= ONE_SECOND) <= 0) {
                    time = 0;
                }
                notifyTimeChanged();
                if (time == 0) {
                    stopCountDown();
                    return;
                }
                handler.postDelayed(this, ONE_SECOND);
            }
        };
    }

    /**
     * Initialize the countdown timer and reset the time to the initial value.
     */
    public void init() {
        time = initTime;
    }

    /**
     * Release and stop the countdown timer, clearing all listeners.
     */
    public void release() {
        stopCountDown();
        if (countDownListeners != null) {
            countDownListeners.clear();
            countDownListeners = null;
        }
        if (countDownMilliListeners != null) {
            countDownMilliListeners.clear();
            countDownMilliListeners = null;
        }
        milliHandler = null;
    }

    /**
     * Set the initial time for the countdown.
     *
     * @param time The initial time in milliseconds.
     */
    public void setTime(long time) {
        this.initTime = this.time = time;
    }

    /**
     * Get the initial time set for the countdown.
     *
     * @return The initial time in milliseconds.
     */
    public long getInitTime() {
        return initTime;
    }

    /**
     * Get the current time remaining in the countdown.
     *
     * @return The current time remaining in milliseconds.
     */
    public long getTime() {
        return time;
    }

    /**
     * Check if the countdown timer is active.
     *
     * @return `true` if the countdown is active, `false` otherwise.
     */
    public boolean isCountDown() {
        return isCountDown;
    }

    /**
     * Start the countdown timer.
     */
    public void startCountDown() {
        if (isCountDown) {
            return;
        }
        isCountDown = true;
        handler.removeCallbacks(countDownRunnable);
        handler.post(countDownRunnable);
    }

    /**
     * Stop the countdown timer.
     */
    public void stopCountDown() {
        if (!isCountDown) {
            return;
        }
        isCountDown = false;
        handler.removeCallbacks(countDownRunnable);
    }

    /**
     * Add a listener to receive countdown updates (every 1 second).
     *
     * @param listener The listener to add.
     */
    public void addListener(CountDownListener listener) {
        if (listener == null) {
            return;
        }
        if (countDownListeners == null) {
            countDownListeners = new ArrayList<>();
        }
        if (!countDownListeners.contains(listener)) {
            countDownListeners.add(listener);
        }
    }

    /**
     * Add a listener to receive countdown updates (every 100 milliseconds).
     *
     * @param listener The listener to add.
     */
    public void addListener(CountDownMilliListener listener) {
        if (listener == null) {
            return;
        }
        if (countDownMilliListeners == null) {
            countDownMilliListeners = new ArrayList<>();
        }
        if (!countDownMilliListeners.contains(listener)) {
            countDownMilliListeners.add(listener);
        }
    }

    /**
     * Remove a listener that was added to receive countdown updates (every 1 second).
     *
     * @param listener The listener to remove.
     */
    public void removeListener(CountDownListener listener) {
        if (countDownListeners != null) {
            countDownListeners.remove(listener);
        }
    }

    /**
     * Remove a listener that was added to receive countdown updates (every 100 milliseconds).
     *
     * @param listener The listener to remove.
     */
    public void removeListener(CountDownMilliListener listener) {
        if (countDownMilliListeners != null) {
            countDownMilliListeners.remove(listener);
        }
    }

    /**
     * Notify registered listeners about countdown updates.
     */
    private void notifyTimeChanged() {
        sendTimeListener();
        sendTimeMillisecondListener();
    }

    private void sendTimeListener() {
        if (countDownListeners == null) {
            return;
        }
        int[] times = splitTime(time);
        for (CountDownListener listener : countDownListeners) {
            listener.onCountDown(times[0], times[1], times[2]);
        }
    }

    /**
     * Send countdown updates to millisecond listeners.
     */
    private void sendTimeMillisecondListener() {
        if (countDownMilliListeners == null || countDownMilliListeners.isEmpty()) {
            return;
        }
        Handler handler = getOrCreateMilliHandler();
        for (int i = 0; i < 10; i++) {
            int what = i;
            int delay = (9 - what) * 100;
            handler.removeMessages(what);
            handler.sendMessageDelayed(handler.obtainMessage(what, (Runnable) () -> {
                if (countDownMilliListeners == null || countDownMilliListeners.isEmpty()) {
                    return;
                }
                for (CountDownMilliListener listener : countDownMilliListeners) {
                    listener.onCountDown(what);
                }
            }), delay);
        }
    }

    private Handler milliHandler;

    /**
     * Get or create a handler for sending millisecond updates.
     *
     * @return The handler for millisecond updates.
     */
    private Handler getOrCreateMilliHandler() {
        if (milliHandler == null) {
            milliHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    if (msg.obj instanceof Runnable) {
                        ((Runnable) msg.obj).run();
                    }
                }
            };
        }
        return milliHandler;
    }

    @NonNull
    private int[] splitTime(long time) {
        int hours = (int) (time / 3_600_000);
        int minutes = (int) ((time % 3_600_000) / 60_000);
        int seconds = (int) (((time % 3_600_000) % 60_000) / 1_000);

        return new int[]{hours, minutes, seconds};
    }

    /**
     * Listener interface for receiving countdown updates (every 1 second).
     */
    public interface CountDownListener {
        /**
         * Callback method called for each 1-second interval.
         *
         * @param hour   The remaining hours in the countdown (0-23).
         * @param minute The remaining minutes in the countdown (0-59).
         * @param second The remaining seconds in the countdown (0-59).
         */
        void onCountDown(int hour, int minute, int second);
    }

    /**
     * Listener interface for receiving countdown updates (every 100 milliseconds).
     */
    public interface CountDownMilliListener {
        /**
         * Callback method called for each 100-millisecond interval.
         *
         * @param mill The remaining milliseconds in the countdown (0-9).
         */
        void onCountDown(int mill);
    }
}
