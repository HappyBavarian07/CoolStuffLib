package de.happybavarian07.coolstufflib.jpa.cache;

import de.happybavarian07.coolstufflib.cache.CacheManager;
import de.happybavarian07.coolstufflib.cache.InMemoryCache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Entity cache of a single entity class. One instance is shared by every repository proxy over that entity
 * class, so a write through one proxy is seen by the reads of all of them and a {@link CacheManager} can clear
 * it by name.</p>
 */
public class EntityCache<ID, E> extends InMemoryCache<ID, E> {
    public static final String NAME_PREFIX = "entities:";
    private static final Map<Class<?>, EntityCache<Object, Object>> CACHES = new ConcurrentHashMap<>();

    public EntityCache(int maxSize) {
        super(maxSize);
    }

    @Override
    public void put(ID key, E value) {
        super.put(key, value);
    }

    public Optional<E> getOptional(ID key) {
        return Optional.ofNullable(super.get(key));
    }

    /** <p>The name the entity class' cache is registered under in a {@link CacheManager}.</p> */
    public static String cacheName(Class<?> entityClass) {
        return NAME_PREFIX + entityClass.getName();
    }

    public static EntityCache<Object, Object> getOrCreate(Class<?> entityClass, int maxSize) {
        return CACHES.computeIfAbsent(entityClass, key -> new EntityCache<>(maxSize));
    }

    public static EntityCache<Object, Object> get(Class<?> entityClass) {
        return CACHES.get(entityClass);
    }

    /** <p>Returns the entity class' cache and, when a manager is given, registers it under {@link #cacheName(Class)}
     * so {@code clearAll}, {@code removeCache} and {@code shutdown} reach it.</p> */
    public static EntityCache<Object, Object> register(Class<?> entityClass, int maxSize, CacheManager cacheManager) {
        EntityCache<Object, Object> cache = getOrCreate(entityClass, maxSize);
        if (cacheManager != null) cacheManager.registerCache(cacheName(entityClass), cache);
        return cache;
    }

    /** <p>Drops every entry of the entity class' cache, for writes that are not tied to a single id.</p> */
    public static void invalidate(Class<?> entityClass) {
        EntityCache<Object, Object> cache = CACHES.get(entityClass);
        if (cache != null) cache.clear();
    }

    /** <p>Drops one entry so the next read reloads it. An id the cache cannot address drops the whole cache,
     * because a stale entry is worse than a cache miss.</p> */
    public static void invalidate(Class<?> entityClass, Object id) {
        EntityCache<Object, Object> cache = CACHES.get(entityClass);
        if (cache == null) return;
        if (id == null) cache.clear();
        else cache.remove(id);
    }

    /** <p>Drops every entry of every entity cache, for writes that a rollback may have discarded.</p> */
    public static void invalidateAll() {
        CACHES.values().forEach(EntityCache::clear);
    }
}
