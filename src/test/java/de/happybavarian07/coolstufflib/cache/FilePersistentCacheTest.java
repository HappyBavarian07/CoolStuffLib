package de.happybavarian07.coolstufflib.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Timeout;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class FilePersistentCacheTest {

    private FilePersistentCache<String, String> cache;
    private String testCacheFile;

    @BeforeEach
    void setUp() {
        testCacheFile = "test_cache_" + System.currentTimeMillis() + ".dat";
        cache = new FilePersistentCache<>(testCacheFile, false, 0);
    }

    @AfterEach
    void tearDown() {
        if (cache != null) {
            cache.close();
        }
        try {
            Files.deleteIfExists(Paths.get(testCacheFile));
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    void testBasicOperations() {
        cache.put("key1", "value1");
        assertEquals("value1", cache.get("key1"));
        assertTrue(cache.containsKey("key1"));

        cache.remove("key1");
        assertNull(cache.get("key1"));
        assertFalse(cache.containsKey("key1"));
    }

    @Test
    void testPersistence() {
        cache.put("persistent_key", "persistent_value");
        cache.save();

        FilePersistentCache<String, String> newCache = new FilePersistentCache<>(testCacheFile, false, 0);
        assertEquals("persistent_value", newCache.get("persistent_key"));
    }

    @Test
    void testLoadNonExistentFile() {
        String nonExistentFile = "non_existent_cache.dat";
        FilePersistentCache<String, String> newCache = new FilePersistentCache<>(nonExistentFile, false, 0);
        assertEquals(0, newCache.size());
    }

    @Test
    void testClear() {
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.clear();

        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
        assertEquals(0, cache.size());
    }

    @Test
    void testSize() {
        assertEquals(0, cache.size());

        cache.put("key1", "value1");
        assertEquals(1, cache.size());

        cache.put("key2", "value2");
        assertEquals(2, cache.size());

        cache.remove("key1");
        assertEquals(1, cache.size());
    }

    @Test
    void testDefaultConstructor() {
        FilePersistentCache<String, String> defaultCache = new FilePersistentCache<>("default_cache.dat", false, 0);
        assertNotNull(defaultCache.getCacheFile());
        assertTrue(defaultCache.getCacheFile().startsWith("default_"));
        assertTrue(defaultCache.getCacheFile().endsWith(".dat"));
    }

    @Test
    void testNullKeyHandling() {
        assertThrows(IllegalArgumentException.class, () -> cache.put(null, "null_key_value"));
        assertThrows(IllegalArgumentException.class, () -> cache.get(null));
        assertThrows(IllegalArgumentException.class, () -> cache.containsKey(null));
        assertThrows(IllegalArgumentException.class, () -> cache.remove(null));
    }

    @Test
    @Timeout(5)
    void testConcurrentAccess() throws InterruptedException {
        final int threadCount = 5;
        final int operationsPerThread = 200;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "thread" + threadId + "key" + j;
                        String value = "thread" + threadId + "value" + j;

                        cache.put(key, value);
                        String retrieved = cache.get(key);

                        if (value.equals(retrieved)) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(threadCount * operationsPerThread, successCount.get());
    }

    @Test
    void testSaveAndLoadIntegrity() {
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");

        cache.save();

        FilePersistentCache<String, String> loadedCache = new FilePersistentCache<>(testCacheFile, false, 0);

        assertEquals("value1", loadedCache.get("key1"));
        assertEquals("value2", loadedCache.get("key2"));
        assertEquals("value3", loadedCache.get("key3"));
        assertEquals(3, loadedCache.size());
    }

    @Test
    void testOverwriteValues() {
        cache.put("key1", "original_value");
        cache.put("key1", "new_value");
        assertEquals("new_value", cache.get("key1"));

        cache.save();
        FilePersistentCache<String, String> loadedCache = new FilePersistentCache<>(testCacheFile, false, 0);
        assertEquals("new_value", loadedCache.get("key1"));
    }

    @Test
    void testComplexObjectTypes() {
        FilePersistentCache<String, Object> complexCache = new FilePersistentCache<>("complex_test.dat", false, 0);

        try {
            complexCache.put("string", "test_string");
            complexCache.put("integer", 42);
            complexCache.put("double", 3.14);
            complexCache.put("boolean", true);

            assertEquals("test_string", complexCache.get("string"));
            assertEquals(42, complexCache.get("integer"));
            assertEquals(3.14, complexCache.get("double"));
            assertEquals(true, complexCache.get("boolean"));

            complexCache.save();

            FilePersistentCache<String, Object> loadedCache = new FilePersistentCache<>("complex_test.dat", false, 0);
            assertEquals("test_string", loadedCache.get("string"));
            assertEquals(42, loadedCache.get("integer"));
            assertEquals(3.14, loadedCache.get("double"));
            assertEquals(true, loadedCache.get("boolean"));
        } finally {
            try {
                Files.deleteIfExists(Paths.get("complex_test.dat"));
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}
