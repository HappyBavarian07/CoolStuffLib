package de.happybavarian07.coolstufflib.cache;

import java.util.LinkedHashMap;
import java.util.Map;

public class InMemoryCache<K, V> implements Cache<K, V> {
    private final Map<K, V> map;
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
        this.map = new LinkedHashMap<K, V>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > InMemoryCache.this.maxSize;
            }
        };
    }

    @Override
    public V get(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key must not be null");
        }
        synchronized (lock) {
            return map.get(key);
        }
    }

    @Override
    public void put(K key, V value, boolean overwrite) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value must not be null");
        }
        synchronized (lock) {
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
            if (key != null) remove(key);
            throw new IllegalArgumentException("Key and value must not be null");
        }
        synchronized (lock) {
            map.put(key, value);
        }
    }

    @Override
    public void remove(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key must not be null");
        }
        synchronized (lock) {
            map.remove(key);
        }
    }

    @Override
    public void clear() {
        synchronized (lock) {
            map.clear();
        }
    }

    @Override
    public boolean containsKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key must not be null");
        }
        synchronized (lock) {
            return map.containsKey(key);
        }
    }
}
