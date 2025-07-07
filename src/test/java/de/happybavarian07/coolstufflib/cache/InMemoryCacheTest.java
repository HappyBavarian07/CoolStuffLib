package de.happybavarian07.coolstufflib.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryCacheTest {

    private InMemoryCache<String, String> cache;

    @BeforeEach
    void setUp() {
        cache = new InMemoryCache<>();
    }

    @Test
    void testPutAndGet() {
        cache.put("key1", "value1");
        assertEquals("value1", cache.get("key1"));
    }

    @Test
    void testGetNonExistentKey() {
        assertNull(cache.get("nonexistent"));
    }

    @Test
    void testContainsKey() {
        cache.put("key1", "value1");
        assertTrue(cache.containsKey("key1"));
        assertFalse(cache.containsKey("key2"));
    }

    @Test
    void testRemove() {
        cache.put("key1", "value1");
        cache.remove("key1");
        assertNull(cache.get("key1"));
        assertFalse(cache.containsKey("key1"));
    }

    @Test
    void testClear() {
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.clear();
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
        assertFalse(cache.containsKey("key1"));
        assertFalse(cache.containsKey("key2"));
    }

    @Test
    void testOverwriteValue() {
        cache.put("key1", "value1");
        cache.put("key1", "value2");
        assertEquals("value2", cache.get("key1"));
    }

    @Test
    void testNullKeyHandling() {
        assertThrows(IllegalArgumentException.class, () -> cache.put(null, "value"));
        assertThrows(IllegalArgumentException.class, () -> cache.get(null));
        assertThrows(IllegalArgumentException.class, () -> cache.containsKey(null));
    }

    @Test
    @Timeout(5)
    void testConcurrentAccess() throws InterruptedException {
        final int threadCount = 10;
        final int operationsPerThread = 1000;
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
    @Timeout(5)
    void testConcurrentReadWrite() throws InterruptedException {
        final int readerCount = 5;
        final int writerCount = 5;
        final int operations = 500;
        final CountDownLatch latch = new CountDownLatch(readerCount + writerCount);

        ExecutorService executor = Executors.newFixedThreadPool(readerCount + writerCount);

        for (int i = 0; i < writerCount; i++) {
            final int writerId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operations; j++) {
                        cache.put("shared_key_" + (j % 10), "writer" + writerId + "_value" + j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        for (int i = 0; i < readerCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operations; j++) {
                        cache.get("shared_key_" + (j % 10));
                        cache.containsKey("shared_key_" + (j % 10));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertDoesNotThrow(() -> latch.await());
        executor.shutdown();
    }

    @Test
    void testMultipleDataTypes() {
        InMemoryCache<Object, Object> multiTypeCache = new InMemoryCache<>();

        multiTypeCache.put("string_key", "string_value");
        multiTypeCache.put(123, "integer_key_value");
        multiTypeCache.put("list_key", java.util.Arrays.asList(1, 2, 3));

        assertEquals("string_value", multiTypeCache.get("string_key"));
        assertEquals("integer_key_value", multiTypeCache.get(123));
        assertEquals(java.util.Arrays.asList(1, 2, 3), multiTypeCache.get("list_key"));
    }
}
