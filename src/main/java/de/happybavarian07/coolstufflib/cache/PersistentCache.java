package de.happybavarian07.coolstufflib.cache;

public interface PersistentCache<K, V> extends Cache<K, V> {
    void save();
    void load();
}

