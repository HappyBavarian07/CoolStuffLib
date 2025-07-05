package de.happybavarian07.coolstufflib.configstuff.advanced.core;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * <p>Thread-safe lock manager that provides fine-grained read-write locking for
 * configuration operations. Separates module operations from value operations
 * to minimize contention and maximize concurrent access performance.</p>
 *
 * <p>This manager provides:</p>
 * <ul>
 * <li>Separate read-write locks for modules and values</li>
 * <li>Convenience methods for safe lock-based operations</li>
 * <li>Automatic lock cleanup with try-with-resources pattern</li>
 * <li>Support for both manual and functional lock management</li>
 * </ul>
 *
 * <pre><code>
 * ConfigLockManager lockManager = new ConfigLockManager();
 * String result = lockManager.withValuesReadLock(() -> config.getString("key"));
 * </code></pre>
 */
public class ConfigLockManager {
    private final ReadWriteLock moduleLock = new ReentrantReadWriteLock();
    private final ReadWriteLock valuesLock = new ReentrantReadWriteLock();

    /**
     * <p>Acquires a read lock for module operations, allowing concurrent reads
     * but blocking write operations on modules.</p>
     *
     * <pre><code>
     * lockManager.lockModuleRead();
     * try {
     *     // Safe to read module state concurrently
     * } finally {
     *     lockManager.unlockModuleRead();
     * }
     * </code></pre>
     */
    public void lockModuleRead() { moduleLock.readLock().lock(); }

    /**
     * <p>Releases the read lock for module operations previously acquired
     * with lockModuleRead().</p>
     *
     * <pre><code>
     * lockManager.unlockModuleRead();
     * </code></pre>
     */
    public void unlockModuleRead() { moduleLock.readLock().unlock(); }

    /**
     * <p>Acquires an exclusive write lock for module operations, blocking
     * all other module read and write operations.</p>
     *
     * <pre><code>
     * lockManager.lockModuleWrite();
     * try {
     *     // Exclusive access to modify modules
     * } finally {
     *     lockManager.unlockModuleWrite();
     * }
     * </code></pre>
     */
    public void lockModuleWrite() { moduleLock.writeLock().lock(); }

    /**
     * <p>Releases the write lock for module operations previously acquired
     * with lockModuleWrite().</p>
     *
     * <pre><code>
     * lockManager.unlockModuleWrite();
     * </code></pre>
     */
    public void unlockModuleWrite() { moduleLock.writeLock().unlock(); }

    /**
     * <p>Acquires a read lock for value operations, allowing concurrent reads
     * but blocking write operations on configuration values.</p>
     *
     * <pre><code>
     * lockManager.lockValuesRead();
     * try {
     *     // Safe to read values concurrently
     * } finally {
     *     lockManager.unlockValuesRead();
     * }
     * </code></pre>
     */
    public void lockValuesRead() { valuesLock.readLock().lock(); }

    /**
     * <p>Releases the read lock for value operations previously acquired
     * with lockValuesRead().</p>
     *
     * <pre><code>
     * lockManager.unlockValuesRead();
     * </code></pre>
     */
    public void unlockValuesRead() { valuesLock.readLock().unlock(); }

    /**
     * <p>Acquires an exclusive write lock for value operations, blocking
     * all other value read and write operations.</p>
     *
     * <pre><code>
     * lockManager.lockValuesWrite();
     * try {
     *     // Exclusive access to modify values
     * } finally {
     *     lockManager.unlockValuesWrite();
     * }
     * </code></pre>
     */
    public void lockValuesWrite() { valuesLock.writeLock().lock(); }

    /**
     * <p>Releases the write lock for value operations previously acquired
     * with lockValuesWrite().</p>
     *
     * <pre><code>
     * lockManager.unlockValuesWrite();
     * </code></pre>
     */
    public void unlockValuesWrite() { valuesLock.writeLock().unlock(); }

    /**
     * <p>Executes an operation with automatic module read lock management,
     * ensuring proper cleanup even if exceptions occur.</p>
     *
     * <pre><code>
     * boolean hasModule = lockManager.withModuleReadLock(() ->
     *     config.hasModule("validation"));
     * </code></pre>
     *
     * @param op the operation to execute under read lock
     * @param <T> the return type of the operation
     * @return the result of the operation
     */
    public <T> T withModuleReadLock(Supplier<T> op) {
        lockModuleRead();
        try {
            return op.get();
        } finally {
            unlockModuleRead();
        }
    }

    /**
     * <p>Executes an operation with automatic value read lock management,
     * ensuring proper cleanup even if exceptions occur.</p>
     *
     * <pre><code>
     * String value = lockManager.withValuesReadLock(() ->
     *     config.getString("database.host"));
     * </code></pre>
     *
     * @param op the operation to execute under read lock
     * @param <T> the return type of the operation
     * @return the result of the operation
     */
    public <T> T withValuesReadLock(Supplier<T> op) {
        lockValuesRead();
        try {
            return op.get();
        } finally {
            unlockValuesRead();
        }
    }

    /**
     * <p>Executes an operation with automatic module write lock management,
     * ensuring proper cleanup even if exceptions occur.</p>
     *
     * <pre><code>
     * Boolean success = lockManager.withModulesWriteLock(() -> {
     *     config.registerModule(newModule);
     *     return true;
     * });
     * </code></pre>
     *
     * @param op the operation to execute under write lock
     * @param <T> the return type of the operation
     * @return the result of the operation
     */
    public <T> T withModulesWriteLock(Supplier<T> op) {
        lockModuleWrite();
        try {
            return op.get();
        } finally {
            unlockModuleWrite();
        }
    }

    /**
     * <p>Executes an operation with automatic value write lock management,
     * ensuring proper cleanup even if exceptions occur.</p>
     *
     * <pre><code>
     * Boolean updated = lockManager.withValuesWriteLock(() -> {
     *     config.set("server.port", 8080);
     *     return true;
     * });
     * </code></pre>
     *
     * @param op the operation to execute under write lock
     * @param <T> the return type of the operation
     * @return the result of the operation
     */
    public <T> T withValuesWriteLock(Supplier<T> op) {
        lockValuesWrite();
        try {
            return op.get();
        } finally {
            unlockValuesWrite();
        }
    }
}
