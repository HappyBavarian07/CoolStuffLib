package de.happybavarian07.coolstufflib.jpa.repository;

import de.happybavarian07.coolstufflib.jpa.utils.EntityQueryBuilder;
import java.util.Optional;

public interface Repository<T, ID> {
    <S extends T> S save(S entity);

    <S extends T> Iterable<S> saveAll(Iterable<S> entities);

    Optional<T> findById(ID id);

    boolean existsById(ID id);

    Iterable<T> findAll();

    Iterable<T> findAllById(Iterable<ID> ids);

    long count();

    void deleteById(ID id);

    void delete(T entity);

    void deleteAllById(Iterable<? extends ID> ids);

    void deleteAll(Iterable<? extends T> entities);

    void deleteAll();

    boolean isDatabaseReady();

    EntityQueryBuilder<T> query();
}
