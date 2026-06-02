package de.happybavarian07.coolstufflib.cache;

public interface Cache<K, V> {
    /**
     * <p>Retrieves a value by key.</p>
     *
     * @param key The key
     * @return The value, or null if not found
     */
    V get(K key);

    /**
     * <p>Stores a value.</p>
     *
     * @param key       The key
     * @param value     The value
     * @param overwrite Whether to overwrite existing
     */
    void put(K key, V value, boolean overwrite);

    /**
     * <p>Stores a value (defaulting to overwrite).</p>
     *
     * @param key   The key
     * @param value The value
     */
    void put(K key, V value);

    /**
     * <p>Removes a value.</p>
     *
     * @param key The key
     */
    void remove(K key);

    /**
     * <p>Clears the cache.</p>
     */
    void clear();

    /**
     * <p>Checks if a key exists.</p>
     *
     * @param key The key
     * @return {@code true} if key exists
     */
    boolean containsKey(K key);
}

