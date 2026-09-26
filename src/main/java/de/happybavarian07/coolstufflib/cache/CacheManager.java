package de.happybavarian07.coolstufflib.cache;

import de.happybavarian07.coolstufflib.service.api.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class CacheManager implements Service {
    private final UUID serviceId = UUID.randomUUID();
    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    @Override
    public UUID id() {
        return serviceId;
    }

    @Override
    public String serviceName() {
        return "cache-manager";
    }

    @Override
    public CompletableFuture<Void> init() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> caches.values().forEach(cache -> {
            if (cache instanceof FilePersistentCache<?, ?> persistent) persistent.close();
            else cache.clear();
        }));
    }

    /**
     * <p>Registers a new cache instance.</p>
     *
     * <pre><code>cacheManager.registerCache("myCache", new InMemoryCache<>());</code></pre>
     *
     * @param name  Cache name
     * @param cache Cache instance
     */
    public <K, V> void registerCache(String name, Cache<K, V> cache) {
        if(name == null || name.isBlank()) throw new IllegalArgumentException("Cache name cannot be null or blank");
        if (cache == null) throw new IllegalArgumentException("Cache instance cannot be null");
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
