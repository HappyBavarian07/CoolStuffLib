package de.happybavarian07.coolstufflib.jpa;

import de.happybavarian07.coolstufflib.cache.CacheManager;
import de.happybavarian07.coolstufflib.jpa.annotations.CacheConfig;
import de.happybavarian07.coolstufflib.jpa.annotations.Column;
import de.happybavarian07.coolstufflib.jpa.annotations.Entity;
import de.happybavarian07.coolstufflib.jpa.annotations.Id;
import de.happybavarian07.coolstufflib.jpa.annotations.Table;
import de.happybavarian07.coolstufflib.jpa.annotations.Transactional;
import de.happybavarian07.coolstufflib.jpa.cache.EntityCache;
import de.happybavarian07.coolstufflib.jpa.repository.Repository;
import de.happybavarian07.coolstufflib.jpa.utils.RepositoryProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class EntityCacheTest {
    private RepositoryController controller;
    private SQLExecutor executor;
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;
    private CacheManager cacheManager;
    private CachedRepo repo;
    private SecondCachedRepo secondRepo;

    @Entity
    @Table(name = "cached_probe")
    @CacheConfig(enabled = true, maxSize = 8)
    public static class CachedEntity {
        @Id
        @Column(name = "id")
        private String id;
        @Column(name = "name")
        private String name;

        public CachedEntity() {
        }

        CachedEntity(String id, String name) {
            this.id = id;
            this.name = name;
        }

        String id() {
            return id;
        }
    }

    @Entity
    @Table(name = "plain_probe")
    public static class PlainEntity {
        @Id
        @Column(name = "id")
        private String id;

        public PlainEntity() {
        }
    }

    public interface CachedRepo extends Repository<CachedEntity, String> {
        void setName(String id, String name);

        /** Writes, reads the uncommitted row into the cache, then abandons the transaction. */
        @Transactional
        default void updateThenFail(CachedEntity entity) {
            update(entity);
            findById(entity.id());
            throw new IllegalStateException("boom");
        }
    }

    public interface SecondCachedRepo extends Repository<CachedEntity, String> {
    }

    public interface PlainRepo extends Repository<PlainEntity, String> {
    }

    @BeforeEach
    void setUp() throws SQLException {
        controller = mock(RepositoryController.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);
        when(controller.getConnection("default")).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        executor = new SQLExecutor(controller, null);
        executor.setDefaultConnection("default");
        cacheManager = new CacheManager();
        EntityCache.invalidate(CachedEntity.class);
        repo = RepositoryProxy.create(CachedRepo.class, "", executor, null, cacheManager);
        secondRepo = RepositoryProxy.create(SecondCachedRepo.class, "", executor, null, cacheManager);
    }

    /** Serves the given rows, one per query, in order. */
    private void rows(String... idThenName) throws SQLException {
        int count = idThenName.length / 2;
        Boolean[] next = new Boolean[count * 2];
        String[] ids = new String[count];
        String[] names = new String[count];
        for (int i = 0; i < count; i++) {
            next[i * 2] = true;
            next[i * 2 + 1] = false;
            ids[i] = idThenName[i * 2];
            names[i] = idThenName[i * 2 + 1];
        }
        if (count == 0) {
            when(resultSet.next()).thenReturn(false);
            return;
        }
        when(resultSet.next()).thenReturn(next[0], Arrays.copyOfRange(next, 1, next.length));
        when(resultSet.getObject("id")).thenReturn(ids[0], Arrays.copyOfRange(ids, 1, ids.length));
        when(resultSet.getObject("name")).thenReturn(names[0], Arrays.copyOfRange(names, 1, names.length));
    }

    private boolean cached(String id) {
        return EntityCache.get(CachedEntity.class).containsKey(id);
    }

    @Test
    void readThroughPopulatesTheCache() throws SQLException {
        rows("1", "old");
        assertEquals("old", repo.findById("1").orElseThrow().name);
        assertTrue(cached("1"));
    }

    @Test
    void updateEvictsTheCachedEntitySoTheNextReadReloads() throws SQLException {
        rows("1", "old");
        assertEquals("old", repo.findById("1").orElseThrow().name);

        repo.update(new CachedEntity("1", "new"));
        assertFalse(cached("1"));

        rows("1", "new");
        assertEquals("new", repo.findById("1").orElseThrow().name);
        verify(statement, times(2)).executeQuery();
    }

    @Test
    void insertEvictsTheCachedEntity() throws SQLException {
        rows("1", "old");
        repo.findById("1");
        assertTrue(cached("1"));

        repo.insert(new CachedEntity("1", "inserted"));
        assertFalse(cached("1"));
    }

    @Test
    void setEvictsTheCachedEntity() throws SQLException {
        rows("1", "old");
        repo.findById("1");
        assertTrue(cached("1"));

        repo.setName("1", "set");
        assertFalse(cached("1"));
    }

    @Test
    void saveEvictsTheCachedEntity() throws SQLException {
        rows("1", "old");
        repo.findById("1");
        assertTrue(cached("1"));

        repo.save(new CachedEntity("1", "saved"));
        assertFalse(cached("1"));
    }

    @Test
    void saveAllEvictsEveryCachedEntity() throws SQLException {
        rows("1", "old1", "2", "old2");

        repo.saveAll(List.of(new CachedEntity("1", "saved"), new CachedEntity("2", "saved")));
        assertFalse(cached("1"));
        assertFalse(cached("2"));
    }

    @Test
    void deleteEvictsTheCachedEntity() throws SQLException {
        rows("1", "old");
        repo.findById("1");
        assertTrue(cached("1"));

        repo.deleteById("1");
        assertFalse(cached("1"));
    }

    @Test
    void entityWithoutCacheConfigIsNeverCached() throws SQLException {
        PlainRepo plainRepo = RepositoryProxy.create(PlainRepo.class, "", executor, null, cacheManager);
        rows("1", "old");

        assertTrue(plainRepo.findById("1").isPresent());
        assertNull(EntityCache.get(PlainEntity.class));
        assertTrue(cacheManager.getCacheNames().stream().noneMatch(name -> name.contains(PlainEntity.class.getName())));
    }

    @Test
    void twoProxiesOverTheSameEntityShareOneCache() throws SQLException {
        rows("1", "old");
        assertTrue(repo.findById("1").isPresent());

        rows("1", "old");
        assertTrue(secondRepo.findById("1").isPresent());
        verify(statement, times(1)).executeQuery();
    }

    @Test
    void writeThroughOneProxyIsSeenByTheOther() throws SQLException {
        rows("1", "old");
        secondRepo.findById("1");
        rows("1", "old");
        repo.findById("1");

        repo.update(new CachedEntity("1", "new"));
        rows("1", "new");
        assertEquals("new", secondRepo.findById("1").orElseThrow().name);
    }

    @Test
    void cacheIsRegisteredWithTheCacheManager() {
        assertTrue(cacheManager.getCacheNames().contains(EntityCache.cacheName(CachedEntity.class)));
        assertSame(EntityCache.get(CachedEntity.class), cacheManager.getCache(EntityCache.cacheName(CachedEntity.class)));
    }

    @Test
    void cacheManagerClearAllDropsTheEntityCache() throws SQLException {
        rows("1", "old");
        repo.findById("1");
        assertTrue(cached("1"));

        cacheManager.clearAll();
        assertFalse(cached("1"));
    }

    @Test
    void cacheManagerCanRemoveTheEntityCache() {
        cacheManager.removeCache(EntityCache.cacheName(CachedEntity.class));
        assertNull(cacheManager.getCache(EntityCache.cacheName(CachedEntity.class)));
    }

    @Test
    void rolledBackTransactionInvalidatesTheEntityCache() throws SQLException {
        rows("1", "old");

        assertThrows(IllegalStateException.class, () -> repo.updateThenFail(new CachedEntity("1", "new")));
        verify(connection).rollback();
        verify(connection, never()).commit();
        assertFalse(cached("1"));
    }
}
