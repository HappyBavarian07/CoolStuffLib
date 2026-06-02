package de.happybavarian07.coolstufflib.jpa.transaction;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Utility class for propagating {@link TransactionManager.TransactionContext} across thread boundaries.
 * 
 * <p>Because TransactionManager uses {@link java.lang.ThreadLocal}, transaction state is lost
 * when moving to an asynchronous thread (e.g., via Bukkit scheduler). Use this bridge to
 * capture the context from the parent thread and inject it into the worker thread.</p>
 * 
 * <p>Example usage:
 * <pre>
 * TransactionManager tm = repository.getTransactionManager();
 * Bukkit.getScheduler().runTaskAsynchronously(plugin, TransactionBridge.wrap(tm, () -> {
 *     // This code now runs inside the same transaction as the parent thread
 *     repository.save(entity);
 * }));
 * </pre>
 * </p>
 */
public final class TransactionBridge {

    private TransactionBridge() {
        // Utility class
    }

    /**
     * Wraps a Runnable task to carry the current transaction context to the execution thread.
     *
     * @param tm   The TransactionManager associated with the repository.
     * @param task The task to execute.
     * @return A wrapped Runnable that propagates the transaction context.
     */
    public static Runnable wrap(TransactionManager tm, Runnable task) {
        TransactionManager.TransactionContext context = tm.getTransactionContext();
        return () -> {
            try {
                tm.setTransactionContext(context);
                task.run();
            } finally {
                tm.setTransactionContext(null);
            }
        };
    }

    /**
     * Wraps a Callable task to carry the current transaction context to the execution thread.
     *
     * @param tm   The TransactionManager associated with the repository.
     * @param task The task to execute.
     * @param <T>  The return type of the task.
     * @return A wrapped Callable that propagates the transaction context.
     */
    public static <T> Callable<T> wrap(TransactionManager tm, Callable<T> task) {
        TransactionManager.TransactionContext context = tm.getTransactionContext();
        return () -> {
            try {
                tm.setTransactionContext(context);
                return task.call();
            } finally {
                tm.setTransactionContext(null);
            }
        };
    }

    /**
     * Wraps a Supplier task to carry the current transaction context to the execution thread.
     *
     * @param tm   The TransactionManager associated with the repository.
     * @param task The task to execute.
     * @param <T>  The return type of the task.
     * @return A wrapped Supplier that propagates the transaction context.
     */
    public static <T> Supplier<T> wrap(TransactionManager tm, Supplier<T> task) {
        TransactionManager.TransactionContext context = tm.getTransactionContext();
        return () -> {
            try {
                tm.setTransactionContext(context);
                return task.get();
            } finally {
                tm.setTransactionContext(null);
            }
        };
    }
}
