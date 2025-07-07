package de.happybavarian07.coolstufflib.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CacheManagerTest {

    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager = new CacheManager();
    }

    @Test
    void testRegisterAndGetCache() {
        Cache<String, String> cache = new InMemoryCache<>();
        cacheManager.registerCache("test_cache", cache);

        Cache<String, String> retrieved = cacheManager.getCache("test_cache");
        assertSame(cache, retrieved);
    }

    @Test
    void testGetNonExistentCache() {
        assertNull(cacheManager.getCache("nonexistent"));
    }

    @Test
    void testGetCacheNames() {
        cacheManager.registerCache("cache1", new InMemoryCache<>());
        cacheManager.registerCache("cache2", new InMemoryCache<>());

        Set<String> names = cacheManager.getCacheNames();
        assertEquals(2, names.size());
        assertTrue(names.contains("cache1"));
        assertTrue(names.contains("cache2"));
    }

    @Test
    void testGetCacheNamesEmpty() {
        Set<String> names = cacheManager.getCacheNames();
        assertTrue(names.isEmpty());
    }

    @Test
    void testOverwriteCache() {
        Cache<String, String> cache1 = new InMemoryCache<>();
        Cache<String, String> cache2 = new InMemoryCache<>();

        cacheManager.registerCache("test_cache", cache1);
        cacheManager.registerCache("test_cache", cache2);

        Cache<String, String> retrieved = cacheManager.getCache("test_cache");
        assertSame(cache2, retrieved);
        assertNotSame(cache1, retrieved);
    }

    @Test
    void testRegisterNullCache() {
        assertThrows(IllegalArgumentException.class, () -> cacheManager.registerCache("null_cache", null));
    }

    @Test
    void testRegisterNullName() {
        Cache<String, String> cache = new InMemoryCache<>();
        assertThrows(IllegalArgumentException.class, () -> cacheManager.registerCache(null, cache));
    }

    @Test
    void testMultipleCacheTypes() {
        Cache<String, Integer> intCache = new InMemoryCache<>();
        Cache<Integer, String> stringCache = new InMemoryCache<>();

        cacheManager.registerCache("int_cache", intCache);
        cacheManager.registerCache("string_cache", stringCache);

        assertSame(intCache, cacheManager.getCache("int_cache"));
        assertSame(stringCache, cacheManager.getCache("string_cache"));
    }

    @Test
    void testCacheManagerIsolation() {
        CacheManager manager1 = new CacheManager();
        CacheManager manager2 = new CacheManager();
        manager1.registerCache("shared_name", new InMemoryCache<>());

        assertNotNull(manager1.getCache("shared_name"));
        assertNull(manager2.getCache("shared_name"));
    }
}
