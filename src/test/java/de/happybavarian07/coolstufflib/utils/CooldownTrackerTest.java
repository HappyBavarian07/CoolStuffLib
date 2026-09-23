package de.happybavarian07.coolstufflib.utils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class CooldownTrackerTest {
    private final AtomicLong now = new AtomicLong(1_000);
    private final CooldownTracker tracker = new CooldownTracker(500, now::get);

    @Test
    void blocksUntilCooldownPassed() {
        assertTrue(tracker.tryUse("a"));
        assertFalse(tracker.tryUse("a"));
        now.addAndGet(200);
        assertEquals(300, tracker.remainingMillis("a"));
        now.addAndGet(300);
        assertTrue(tracker.tryUse("a"));
    }

    @Test
    void keysAreIndependent() {
        assertTrue(tracker.tryUse("a"));
        assertTrue(tracker.tryUse("b"));
    }

    @Test
    void expiredEntriesArePruned() {
        tracker.tryUse("a");
        now.addAndGet(600);
        tracker.tryUse("b");
        assertEquals(1, tracker.size());
    }

    @Test
    void zeroCooldownNeverBlocks() {
        CooldownTracker none = new CooldownTracker(0, now::get);
        assertTrue(none.tryUse("a"));
        assertTrue(none.tryUse("a"));
    }
}
