package de.happybavarian07.coolstufflib.jpa.cache;

import de.happybavarian07.coolstufflib.cache.InMemoryCache;

import java.util.Optional;

public class EntityCache<ID, E> extends InMemoryCache<ID, E> {
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
}
