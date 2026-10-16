package com.mct.app.helper.iap.billing;

import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Makes a per-token Play call (acknowledge, and consume through {@link PurchaseConsumer})
 * idempotent per purchase token. The legacy auto-acknowledge path and the
 * refresh can both try to acknowledge the same purchase at the same time; with this tracker the
 * second caller joins the call already in flight and both get its result, and a token acknowledged
 * earlier in the session answers OK without calling Play again.
 * <p>
 * Main thread only. {@link AckCall} must deliver its result on the main thread.
 */
final class AcknowledgeTracker {

    interface AckResult {
        void onResult(int responseCode, @Nullable String debugMessage);
    }

    interface AckCall {
        void acknowledge(@NonNull String purchaseToken, @NonNull AckResult result);
    }

    private final Map<String, List<AckResult>> inFlight = new HashMap<>();
    private final Set<String> acknowledged = new HashSet<>();
    private int generation;

    void acknowledge(@NonNull String token, @NonNull AckCall call, @NonNull AckResult callback) {
        if (acknowledged.contains(token)) {
            callback.onResult(OK, null);
            return;
        }
        List<AckResult> waiting = inFlight.get(token);
        if (waiting != null) {
            waiting.add(callback);
            return;
        }
        waiting = new ArrayList<>();
        waiting.add(callback);
        inFlight.put(token, waiting);
        int gen = generation;
        call.acknowledge(token, (code, message) -> {
            if (gen != generation) {
                return; // released: its waiters were already dropped or failed
            }
            List<AckResult> callbacks = inFlight.remove(token);
            if (code == OK) {
                acknowledged.add(token);
            }
            if (callbacks != null) {
                for (AckResult r : callbacks) {
                    r.onResult(code, message);
                }
            }
        });
    }

    boolean isInFlight(@NonNull String token) {
        return inFlight.containsKey(token);
    }

    boolean isDone(@NonNull String token) {
        return acknowledged.contains(token);
    }

    /** Drops in-flight waiters (their Play callbacks were cancelled by release). */
    void clearInFlight() {
        generation++;
        inFlight.clear();
    }

    /** Answers every in-flight waiter with {@code code}; late Play answers are then ignored. */
    void failInFlight(int code, @Nullable String message) {
        generation++;
        List<List<AckResult>> all = new ArrayList<>(inFlight.values());
        inFlight.clear();
        for (List<AckResult> callbacks : all) {
            for (AckResult r : callbacks) {
                r.onResult(code, message);
            }
        }
    }
}
