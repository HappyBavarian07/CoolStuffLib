package de.happybavarian07.coolstufflib.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryCache<K, V> implements Cache<K, V> {
    private final ConcurrentMap<K, V> map = new ConcurrentHashMap<>();
    private final int maxSize;
    private final Object lock = new Object();

    public InMemoryCache() {
        this(Integer.MAX_VALUE);
    }

    public InMemoryCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Max size must be positive");
        }
        this.maxSize = maxSize;
    }

    @Override
    public V get(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key must not be null");
        }
        return map.get(key);
    }

    @Override
    public void put(K key, V value, boolean overwrite) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value must not be null");
        }
        synchronized (lock) {
            if (map.size() >= maxSize && !map.containsKey(key)) {
                return;
            }
            if (overwrite) {
                map.put(key, value);
            } else {
                map.putIfAbsent(key, value);
            }
        }
    }

    @Override
    public void put(K key, V value) {
        if (key == null || value == null) {
            if (key != null) {
                map.remove(key);
            }
            throw new IllegalArgumentException("Key and value must not be null");
        }
        synchronized (lock) {
            if (map.size() >= maxSize && !map.containsKey(key)) {
                return;
            }
            map.put(key, value);
        }
    }

    @Override
    public void remove(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key must not be null");
        }
        map.remove(key);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public boolean containsKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key must not be null");
        }
        return map.containsKey(key);
    }
}
