package de.happybavarian07.coolstufflib.jpa;

import de.happybavarian07.coolstufflib.jpa.annotations.Column;
import de.happybavarian07.coolstufflib.jpa.annotations.Entity;
import de.happybavarian07.coolstufflib.jpa.annotations.Id;
import de.happybavarian07.coolstufflib.jpa.annotations.Table;
import de.happybavarian07.coolstufflib.jpa.annotations.Transactional;
import de.happybavarian07.coolstufflib.jpa.repository.Repository;
import de.happybavarian07.coolstufflib.jpa.utils.RepositoryProxy;
import de.happybavarian07.coolstufflib.jpa.utils.RepositoryProxy.UnsupportedQueryMethodException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class RepositoryProxyTest {
    private RepositoryController controller;
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;
    private TestRepo repo;
    private ProbeRepo probeRepo;

    public static class TestEntity {
    }

    public interface TestRepo extends Repository<TestEntity, String> {
        default String greet(String name) {
            return "hi " + name;
        }

        @Transactional
        default String inTransaction() {
            return "done";
        }

        String bogus();
    }

    @Entity
    @Table(name = "probe")
    public static class ProbeEntity {
        @Id
        @Column(name = "id")
        private String id;
        @Column(name = "name")
        private String name;
        @Column(name = "coins")
        private int coins;

        public ProbeEntity() {
        }
    }

    public interface ProbeRepo extends Repository<ProbeEntity, String> {
        List<ProbeEntity> findByName(String name);

        List<ProbeEntity> findByNameAndCoins(String name, int coins);

        List<ProbeEntity> findAllByNameAndCoins(String name, int coins);

        long countByName(String name);

        long countColumnsByName(String name);

        void deleteByName(String name);

        List<ProbeEntity> findByNameOrCoins(String name, int coins);

        List<ProbeEntity> findByCoinsGreaterThan(int coins);

        List<ProbeEntity> findByUnknown(String value);

        List<ProbeEntity> findFirstByName(String name);

        long countByNameOrCoins(String name, int coins);

        long countColumnsByNameOrCoins(String name, int coins);

        void deleteByNameOrCoins(String name, int coins);
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
        SQLExecutor executor = new SQLExecutor(controller, null);
        executor.setDefaultConnection("default");
        repo = RepositoryProxy.create(TestRepo.class, "", executor, null);
        probeRepo = RepositoryProxy.create(ProbeRepo.class, "", executor, null);
    }

    @Test
    void defaultMethodsAreInvoked() {
        assertEquals("hi Steve", repo.greet("Steve"));
    }

    @Test
    void transactionalDefaultMethodCommits() throws SQLException {
        assertEquals("done", repo.inTransaction());
        verify(connection).commit();
    }

    @Test
    void unknownMethodNameThrows() {
        assertThrows(UnsupportedOperationException.class, repo::bogus);
    }

    @Test
    void objectMethodsDoNotRecurse() {
        assertNotNull(repo.toString());
        assertEquals(repo, repo);
        assertEquals(System.identityHashCode(repo), repo.hashCode());
    }

    @Test
    void derivedQueryWithOrThrowsInsteadOfReturningNull() {
        UnsupportedQueryMethodException thrown = assertThrows(UnsupportedQueryMethodException.class,
                () -> probeRepo.findByNameOrCoins("Steve", 5));
        assertTrue(thrown.getMessage().contains("findByNameOrCoins"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("'Or'"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("And"), thrown.getMessage());
    }

    @Test
    void derivedQueryWithComparisonOperatorThrows() {
        UnsupportedQueryMethodException thrown = assertThrows(UnsupportedQueryMethodException.class,
                () -> probeRepo.findByCoinsGreaterThan(5));
        assertTrue(thrown.getMessage().contains("findByCoinsGreaterThan"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("'GreaterThan'"), thrown.getMessage());
    }

    @Test
    void derivedQueryOnAnUnknownFieldThrows() {
        UnsupportedQueryMethodException thrown = assertThrows(UnsupportedQueryMethodException.class,
                () -> probeRepo.findByUnknown("x"));
        assertTrue(thrown.getMessage().contains("findByUnknown"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("unknown"), thrown.getMessage());
    }

    @Test
    void findMethodThatIsNoDerivedQueryThrows() {
        assertThrows(UnsupportedQueryMethodException.class, () -> probeRepo.findFirstByName("Steve"));
    }

    @Test
    void countByWithOrThrows() {
        assertThrows(UnsupportedQueryMethodException.class, () -> probeRepo.countByNameOrCoins("Steve", 5));
    }

    @Test
    void countColumnsByWithOrThrows() {
        assertThrows(UnsupportedQueryMethodException.class, () -> probeRepo.countColumnsByNameOrCoins("Steve", 5));
    }

    @Test
    void deleteByWithOrThrows() {
        assertThrows(UnsupportedQueryMethodException.class, () -> probeRepo.deleteByNameOrCoins("Steve", 5));
    }

    @Test
    void andChainedDerivedQueriesStillWork() throws SQLException {
        row("1", "Steve", 5);
        assertEquals(1, probeRepo.findByName("Steve").size());

        row("1", "Steve", 5);
        assertEquals(1, probeRepo.findByNameAndCoins("Steve", 5).size());
        verify(connection).prepareStatement("SELECT * FROM probe WHERE name = ? AND coins = ?");

        row("1", "Steve", 5);
        assertEquals(1, probeRepo.findAllByNameAndCoins("Steve", 5).size());

        countRow(1L);
        assertEquals(1L, probeRepo.countByName("Steve"));
        countRow(1L);
        assertEquals(1L, probeRepo.countColumnsByName("Steve"));

        row("1", "Steve", 5);
        probeRepo.deleteByName("Steve");
        verify(connection).prepareStatement("DELETE FROM probe WHERE id = ?");
    }

    private void row(String id, String name, int coins) throws SQLException {
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject("id")).thenReturn(id);
        when(resultSet.getObject("name")).thenReturn(name);
        when(resultSet.getObject("coins")).thenReturn(coins);
    }

    private void countRow(long count) throws SQLException {
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong(1)).thenReturn(count);
    }
}
