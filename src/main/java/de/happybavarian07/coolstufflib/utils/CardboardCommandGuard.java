package de.happybavarian07.coolstufflib.utils;

import org.bukkit.Bukkit;

import java.util.function.Supplier;

/**
 * <p>Thread-safe guard to detect and prevent command recursion.</p>
 */
public class CardboardCommandGuard {
    private static final ThreadLocal<Boolean> EXECUTING = ThreadLocal.withInitial(() -> false);
    private static final int MAX_DEPTH = 3;
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    /**
     * <p>Enters a command context.</p>
     *
     * @return {@code true} if allowed, {@code false} if recursion detected
     */
    public static boolean enterCommand() {
        if (EXECUTING.get()) {
            int depth = DEPTH.get();
            if (depth >= MAX_DEPTH) {
                Bukkit.getLogger().warning(
                    "CoolStuffLib: Detected command recursion (depth " + depth + "), breaking loop"
                );
                return false;
            }
            DEPTH.set(depth + 1);
        } else {
            EXECUTING.set(true);
            DEPTH.set(0);
        }
        return true;
    }

    /**
     * <p>Exits the command context.</p>
     */
    public static void exitCommand() {
        int depth = DEPTH.get();
        if (depth == 0) {
            EXECUTING.set(false);
        } else {
            DEPTH.set(depth - 1);
        }
    }

    /**
     * <p>Checks if the execution is in a Cardboard Brigadier context.</p>
     *
     * @return {@code true} if in Brigadier context
     */
    public static boolean isCardboardBrigadierContext() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        int brigadierCount = 0;

        for (StackTraceElement element : stack) {
            if (element.getClassName().contains("BukkitCommandWrapper")) {
                brigadierCount++;
                if (brigadierCount > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * <p>Runs a deliberately nested command dispatch (e.g. a command alias) in a fresh guard scope, so it
     * is not mistaken for Brigadier recursion. The caller must limit its own nesting.</p>
     */
    public static <T> T isolated(Supplier<T> action) {
        boolean executing = EXECUTING.get();
        int depth = DEPTH.get();
        EXECUTING.set(false);
        DEPTH.set(0);
        try {
            return action.get();
        } finally {
            EXECUTING.set(executing);
            DEPTH.set(depth);
        }
    }

    /**
     * <p>Cleans up thread-local variables.</p>
     */
    public static void cleanup() {
        EXECUTING.remove();
        DEPTH.remove();
    }
}

