package de.happybavarian07.coolstufflib.cache.expression;

import de.happybavarian07.coolstufflib.cache.Cache;
import de.happybavarian07.coolstufflib.cache.InMemoryCache;

public class ExpressionCache<V> {
    private final Cache<ExpressionCacheKey, V> cache;

    public ExpressionCache() {
        this.cache = new InMemoryCache<>();
    }

    public ExpressionCache(Cache<ExpressionCacheKey, V> cache) {
        this.cache = cache;
    }

    /**
     * <p>Retrieves a cached value.</p>
     *
     * @param key The cache key
     * @return The cached value
     */
    public V get(ExpressionCacheKey key) {
        return cache.get(key);
    }

    /**
     * <p>Stores a value in the cache.</p>
     *
     * @param key   The cache key
     * @param value The value
     */
    public void put(ExpressionCacheKey key, V value) {
        cache.put(key, value);
    }

    public void remove(ExpressionCacheKey key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }

    public boolean containsKey(ExpressionCacheKey key) {
        return cache.containsKey(key);
    }
}
