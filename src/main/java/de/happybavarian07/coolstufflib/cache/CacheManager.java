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
        return CompletableFuture.runAsync(this::clearAll);
    }

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
