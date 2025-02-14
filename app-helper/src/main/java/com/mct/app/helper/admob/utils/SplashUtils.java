package com.mct.app.helper.admob.utils;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.mct.app.helper.admob.AdsManager;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SplashUtils {

    private static final long DEF_MAX_LOAD_DURATION = 5000; // max time load ads
    private static final long DEF_MAX_INIT_DURATION = 2000; // max time init
    private static final long DEF_MIN_INIT_DURATION = 500;  // min time init

    /**
     * @param activity activity
     * @param alias    open ads or interstitial ads alias
     */
    @NonNull
    public static Builder with(@NonNull Activity activity, @NonNull String alias) {
        return new Builder(activity, alias, null);
    }

    /**
     * Priority alias1 > alias2
     *
     * @param activity activity
     * @param alias1   alias1: open ads or interstitial ads alias
     * @param alias2   alias2: open ads or interstitial ads alias
     */
    @NonNull
    public static Builder with(@NonNull Activity activity, @NonNull String alias1, String alias2) {
        return new Builder(activity, alias1, alias2);
    }

    public interface Starter {
        void start();
    }

    private static class StarterImpl implements Starter {

        private long startTime;
        private long maxLoadDuration = DEF_MAX_LOAD_DURATION; // max time load ads
        private long maxInitDuration = DEF_MAX_INIT_DURATION; // max time init
        private long minInitDuration = DEF_MIN_INIT_DURATION; // min time init
        private Activity activity;
        private String alias1;
        private String alias2;
        private Handler handler;
        private Runnable goToNextScreen;

        private boolean isStarted;
        private boolean isDisposed;

        private StarterImpl(Activity activity, String alias1, String alias2) {
            this.activity = activity;
            this.alias1 = alias1;
            this.alias2 = alias2;
        }

        @Override
        public void start() {
            if (isDisposed) {
                return; // already disposed
            }
            if (isStarted) {
                return; // already started
            }
            isStarted = true;
            startTime = System.currentTimeMillis();
            // init
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            Runnable nextScreen = this::handleNextScreen;

            // schedule nextScreen screen after maxLoadDuration
            handler.postDelayed(nextScreen, maxLoadDuration);

            // load ads
            if (alias1 != null && alias2 != null) {
                loadAdsScenario2(nextScreen);
            } else {
                loadAdsScenario1(nextScreen);
            }
        }

        private void loadAdsScenario1(Runnable nextScreen) {
            AdsManager.getInstance().load(alias1, activity.getApplicationContext(),
                    () -> {
                        if (isDisposed) {
                            return;
                        }
                        // remove schedule
                        handler.removeCallbacks(nextScreen);

                        // load success -> show
                        AdsManager.getInstance().show(alias1, activity, nextScreen::run);
                    }, () -> {
                        if (isDisposed) {
                            return;
                        }
                        // remove schedule
                        handler.removeCallbacks(nextScreen);

                        // load fail -> next screen
                        nextScreen.run();
                    }
            );
        }

        private void loadAdsScenario2(Runnable nextScreen) {

            AtomicBoolean dispose = new AtomicBoolean(false);
            AtomicReference<Boolean> load1 = new AtomicReference<>(null);
            AtomicReference<Boolean> load2 = new AtomicReference<>(null);

            Runnable onUpdate = () -> {
                if (dispose.get()) {
                    return;
                }
                if (isDisposed) {
                    return;
                }
                Boolean l1 = load1.get();
                Boolean l2 = load2.get();

                // load alias 1 ok
                if (l1 == Boolean.TRUE) {
                    dispose.set(true);

                    // remove schedule
                    handler.removeCallbacks(nextScreen);

                    // load success -> show
                    AdsManager.getInstance().show(alias1, activity, nextScreen::run);
                    return;
                }

                // load alias 1 false and load alias 2 ok
                if (l1 == Boolean.FALSE && l2 == Boolean.TRUE) {
                    dispose.set(true);

                    // remove schedule
                    handler.removeCallbacks(nextScreen);

                    // load success -> show
                    AdsManager.getInstance().show(alias2, activity, nextScreen::run);
                    return;
                }

                // both false
                if (l1 == Boolean.FALSE && l2 == Boolean.FALSE) {
                    dispose.set(true);

                    // remove schedule
                    handler.removeCallbacks(nextScreen);

                    // load fail -> next screen
                    nextScreen.run();
                }
            };

            AdsManager.getInstance().load(alias1, activity.getApplicationContext(), () -> {
                load1.set(true);
                onUpdate.run();
            }, () -> {
                load1.set(false);
                onUpdate.run();
            });
            AdsManager.getInstance().load(alias2, activity.getApplicationContext(), () -> {
                load2.set(true);
                onUpdate.run();
            }, () -> {
                load2.set(false);
                onUpdate.run();
            });
        }

        private void handleNextScreen() {
            if (isDisposed) {
                return;
            }
            // save current before dispose
            Handler handler = this.handler;
            Runnable goToNextScreen = this.goToNextScreen;

            // dispose
            dispose();

            // schedule next screen
            long duration = System.currentTimeMillis() - startTime;
            long delay = duration < maxInitDuration ? maxInitDuration - duration : 0;
            handler.postDelayed(goToNextScreen, Math.max(minInitDuration, delay));
        }

        private void dispose() {
            isDisposed = true;
            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
                handler = null;
            }
            activity = null;
            alias1 = null;
            alias2 = null;
            goToNextScreen = null;
        }
    }

    public static class Builder {

        private boolean isCalledBuild;
        private StarterImpl start;

        private Builder(@NonNull Activity activity, @NonNull String alias1, String alias2) {
            Objects.requireNonNull(activity);
            Objects.requireNonNull(alias1);
            this.start = new StarterImpl(activity, alias1, alias2);
        }

        /**
         * @param maxLoadDuration max time load ads
         */
        public Builder setMaxLoadDuration(long maxLoadDuration) {
            checkCalledBuild();
            this.start.maxLoadDuration = maxLoadDuration;
            return this;
        }

        /**
         * @param maxInitDuration max time init
         */
        public Builder setMaxInitDuration(long maxInitDuration) {
            checkCalledBuild();
            this.start.maxInitDuration = maxInitDuration;
            return this;
        }

        /**
         * @param minInitDuration min time init
         */
        public Builder setMinInitDuration(long minInitDuration) {
            checkCalledBuild();
            this.start.minInitDuration = minInitDuration;
            return this;
        }

        /**
         * @param handler handler
         */
        public Builder setHandler(Handler handler) {
            checkCalledBuild();
            this.start.handler = handler;
            return this;
        }

        /**
         * @param goToNextScreen runnable for next screen
         */
        public Builder setGoToNextScreen(Runnable goToNextScreen) {
            checkCalledBuild();
            this.start.goToNextScreen = goToNextScreen;
            return this;
        }

        public Starter build() {
            checkCalledBuild();
            this.isCalledBuild = true;
            Starter starter = start;
            start = null;
            return starter;
        }

        public void start() {
            build().start();
        }

        private void checkCalledBuild() {
            if (isCalledBuild) {
                throw new IllegalStateException("You can't call build() more than once");
            }
        }
    }

    private SplashUtils() {
        //no instance
    }
}
