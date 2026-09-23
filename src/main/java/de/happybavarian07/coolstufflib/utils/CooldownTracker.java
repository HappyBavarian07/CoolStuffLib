package de.happybavarian07.coolstufflib.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * <p>Per-key cooldowns (e.g. per player UUID). Expired entries are dropped on access.</p>
 */
public final class CooldownTracker {
    private final long cooldownMillis;
    private final LongSupplier clock;
    private final Map<Object, Long> lastUse = new ConcurrentHashMap<>();

    public CooldownTracker(long cooldownMillis) {
        this(cooldownMillis, System::currentTimeMillis);
    }

    public CooldownTracker(long cooldownMillis, LongSupplier clock) {
        this.cooldownMillis = Math.max(0L, cooldownMillis);
        this.clock = clock;
    }

    public long remainingMillis(Object key) {
        long now = clock.getAsLong();
        lastUse.values().removeIf(time -> now - time >= cooldownMillis);
        Long last = lastUse.get(key);
        return last == null ? 0 : Math.max(0, cooldownMillis - (now - last));
    }

    /** Starts the cooldown and returns {@code true} if {@code key} is not on cooldown. */
    public boolean tryUse(Object key) {
        if (remainingMillis(key) > 0) return false;
        lastUse.put(key, clock.getAsLong());
        return true;
    }

    public int size() {
        return lastUse.size();
    }
}
