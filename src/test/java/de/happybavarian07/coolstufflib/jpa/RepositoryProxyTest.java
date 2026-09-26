package de.happybavarian07.coolstufflib.jpa;

import de.happybavarian07.coolstufflib.jpa.annotations.Transactional;
import de.happybavarian07.coolstufflib.jpa.repository.Repository;
import de.happybavarian07.coolstufflib.jpa.utils.RepositoryProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RepositoryProxyTest {
    private Connection connection;
    private TestRepo repo;

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

    @BeforeEach
    void setUp() throws SQLException {
        RepositoryController controller = mock(RepositoryController.class);
        connection = mock(Connection.class);
        when(controller.getConnection("default")).thenReturn(connection);
        SQLExecutor executor = new SQLExecutor(controller, null);
        executor.setDefaultConnection("default");
        repo = RepositoryProxy.create(TestRepo.class, "", executor, null);
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
}
