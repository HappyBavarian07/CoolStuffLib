package de.happybavarian07.coolstufflib.commandmanagement;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * <p>Two-step confirmation: the first call registers a pending request, the same call (same key and
 * signature) within the window confirms it.</p>
 */
final class ConfirmationTracker {
    private final long windowMillis;
    private final LongSupplier clock;
    private final Map<Object, Pending> pending = new ConcurrentHashMap<>();

    ConfirmationTracker(long windowMillis) {
        this(windowMillis, System::currentTimeMillis);
    }

    ConfirmationTracker(long windowMillis, LongSupplier clock) {
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    /** Returns {@code true} if this call confirms an earlier identical one, otherwise starts a new request. */
    boolean confirm(Object key, String signature) {
        long now = clock.getAsLong();
        Pending previous = pending.remove(key);
        if (previous != null && previous.signature().equals(signature) && now <= previous.expiresAt()) return true;
        pending.put(key, new Pending(signature, now + windowMillis));
        return false;
    }

    private record Pending(String signature, long expiresAt) {
    }
}
