package de.happybavarian07.coolstufflib.jpa.repository;

import de.happybavarian07.coolstufflib.jpa.utils.EntityQueryBuilder;

import java.util.Optional;

public interface Repository<T, ID> {
    /**
     * <p>Saves the provided entity.</p>
     *
     * <pre><code>repo.save(entity);</code></pre>
     *
     * @param entity The entity to save
     * @param <S>    Entity type
     * @return The saved entity
     */
    <S extends T> S save(S entity);

    /**
     * <p>Saves all provided entities.</p>
     *
     * <pre><code>repo.saveAll(entities);</code></pre>
     *
     * @param entities The entities to save
     * @param <S>      Entity type
     * @return The saved entities
     */
    <S extends T> Iterable<S> saveAll(Iterable<S> entities);

    /**
     * <p>Finds an entity by its ID.</p>
     *
     * <pre><code>Optional<T> entity = repo.findById(id);</code></pre>
     *
     * @param id The ID
     * @return An Optional containing the entity
     */
    Optional<T> findById(ID id);

    /**
     * <p>Checks if an entity exists by ID.</p>
     *
     * <pre><code>boolean exists = repo.existsById(id);</code></pre>
     *
     * @param id The ID
     * @return {@code true} if exists
     */
    boolean existsById(ID id);

    /**
     * <p>Retrieves all entities.</p>
     *
     * <pre><code>Iterable<T> all = repo.findAll();</code></pre>
     *
     * @return All entities
     */
    Iterable<T> findAll();

    /**
     * <p>Retrieves entities by IDs.</p>
     *
     * <pre><code>Iterable<T> results = repo.findAllById(ids);</code></pre>
     *
     * @param ids The IDs
     * @return Found entities
     */
    Iterable<T> findAllById(Iterable<ID> ids);

    /**
     * <p>Counts total entities.</p>
     *
     * <pre><code>long count = repo.count();</code></pre>
     *
     * @return Entity count
     */
    long count();

    /**
     * <p>Deletes an entity by ID.</p>
     *
     * <pre><code>repo.deleteById(id);</code></pre>
     *
     * @param id The ID
     */
    void deleteById(ID id);

    /**
     * <p>Deletes an entity.</p>
     *
     * <pre><code>repo.delete(entity);</code></pre>
     *
     * @param entity The entity
     */
    void delete(T entity);

    /**
     * <p>Deletes entities by IDs.</p>
     *
     * @param ids The IDs
     */
    void deleteAllById(Iterable<? extends ID> ids);

    /**
     * <p>Deletes multiple entities.</p>
     *
     * @param entities The entities
     */
    void deleteAll(Iterable<? extends T> entities);

    /**
     * <p>Deletes all entities.</p>
     */
    void deleteAll();

    /**
     * <p>Checks if the database is ready.</p>
     *
     * @return {@code true} if ready
     */
    boolean isDatabaseReady();

    /**
     * <p>Inserts an entity without checking for existence.</p>
     *
     * @param entity The entity
     */
    void insert(T entity);

    /**
     * <p>Updates an entity.</p>
     *
     * @param entity The entity
     */
    void update(T entity);

    /**
     * <p>Creates a query builder for this entity type.</p>
     *
     * <pre><code>repo.query().where("...", ...).execute();</code></pre>
     *
     * @return A query builder
     */
    EntityQueryBuilder<T> query();
}
