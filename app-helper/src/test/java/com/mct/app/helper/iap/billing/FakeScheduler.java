package com.mct.app.helper.iap.billing;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/** Runs posts at once; delayed work runs when the manual clock is advanced. */
final class FakeScheduler implements PurchasesRefresher.Scheduler {
    private long now = 1_000_000L;
    private final List<long[]> times = new ArrayList<>();
    private final List<Runnable> delayed = new ArrayList<>();

    @Override
    public void post(@NonNull Runnable r) {
        r.run();
    }

    @NonNull
    @Override
    public PurchasesRefresher.Cancellable postDelayed(@NonNull Runnable r, long delayMs) {
        long[] at = {now + delayMs};
        times.add(at);
        delayed.add(r);
        return () -> {
            int i = delayed.indexOf(r);
            if (i >= 0) {
                delayed.remove(i);
                times.remove(i);
            }
        };
    }

    @Override
    public long now() {
        return now;
    }

    void advance(long ms) {
        now += ms;
        for (int i = 0; i < delayed.size(); ) {
            if (times.get(i)[0] <= now) {
                Runnable r = delayed.remove(i);
                times.remove(i);
                r.run();
            } else {
                i++;
            }
        }
    }
}
