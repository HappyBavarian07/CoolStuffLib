package de.happybavarian07.coolstufflib.cache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CacheManager {
    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    public <K, V> void registerCache(String name, Cache<K, V> cache) {
        if (name == null) {
            throw new IllegalArgumentException("Cache name must not be null");
        }
        if (cache == null) {
            throw new IllegalArgumentException("Cache instance must not be null");
        }
        caches.put(name, cache);
    }

    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name) {
        return (Cache<K, V>) caches.get(name);
    }

    public void removeCache(String name) {
        caches.remove(name);
    }

    public void clearAll() {
        caches.values().forEach(Cache::clear);
    }

    public Set<String> getCacheNames() {
        return caches.keySet();
    }
}
