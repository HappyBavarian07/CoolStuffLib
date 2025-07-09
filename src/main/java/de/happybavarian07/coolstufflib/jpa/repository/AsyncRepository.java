package de.happybavarian07.coolstufflib.jpa.repository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AsyncRepository<T, ID> extends Repository<T, ID> {

    CompletableFuture<Optional<T>> findByIdAsync(ID id);

    CompletableFuture<Iterable<T>> findAllAsync();

    CompletableFuture<T> saveAsync(T entity);

    CompletableFuture<Void> deleteAsync(T entity);

    CompletableFuture<Void> deleteByIdAsync(ID id);

    CompletableFuture<Long> countAsync();

    CompletableFuture<Boolean> existsByIdAsync(ID id);
}
