package de.happybavarian07.coolstufflib.utils;

import org.bukkit.Bukkit;

public class CardboardCommandGuard {
    private static final ThreadLocal<Boolean> EXECUTING = ThreadLocal.withInitial(() -> false);
    private static final int MAX_DEPTH = 3;
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

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

    public static void exitCommand() {
        int depth = DEPTH.get();
        if (depth == 0) {
            EXECUTING.set(false);
        } else {
            DEPTH.set(depth - 1);
        }
    }

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

    public static void cleanup() {
        EXECUTING.remove();
        DEPTH.remove();
    }
}

