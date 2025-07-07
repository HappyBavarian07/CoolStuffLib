package de.happybavarian07.coolstufflib.cache;

public interface Cache<K, V> {
    V get(K key);

    void put(K key, V value, boolean overwrite);

    void put(K key, V value);
    void remove(K key);
    void clear();
    boolean containsKey(K key);
}

