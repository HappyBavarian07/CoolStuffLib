package de.happybavarian07.coolstufflib.jpa;

import de.happybavarian07.coolstufflib.jpa.annotations.Transactional;
import de.happybavarian07.coolstufflib.jpa.transaction.TransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransactionTest {
    private RepositoryController controller;
    private SQLExecutor executor;
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;

    interface Repo {
        @Transactional
        void work();
    }

    @BeforeEach
    void setUp() throws SQLException {
        controller = mock(RepositoryController.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);
        when(controller.getConnection("default")).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(connection.setSavepoint()).thenReturn(mock(Savepoint.class));
        when(statement.executeQuery()).thenReturn(resultSet);
        executor = new SQLExecutor(controller, null);
        executor.setDefaultConnection("default");
    }

    @Test
    void queryReleasesConnectionEvenWhenMapperFails() {
        assertThrows(SQLException.class, () -> executor.query("SELECT 1", rs -> {
            throw new SQLException("mapper failed");
        }));
        verify(controller).releaseConnection("default", connection);
    }

    @Test
    void queryReturnsMappedValue() throws SQLException {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(42);
        int value = executor.query("SELECT 1", rs -> rs.next() ? rs.getInt(1) : 0);
        assertEquals(42, value);
        verify(controller).releaseConnection("default", connection);
    }

    @Test
    void boundConnectionIsUsedAndNotReleased() throws SQLException {
        Connection bound = mock(Connection.class);
        when(bound.prepareStatement(anyString())).thenReturn(statement);
        executor.bindTransactionConnection(bound);
        try {
            executor.executeUpdate("UPDATE t SET a = 1");
            verify(controller, never()).getConnection(anyString());
            verify(controller, never()).releaseConnection(anyString(), any());
        } finally {
            executor.bindTransactionConnection(null);
        }
        assertNull(executor.getTransactionConnection());
    }

    @Test
    void transactionUsesOneConnectionCommitsAndReleases() throws Throwable {
        TransactionManager tm = new TransactionManager(executor);
        tm.executeInTransaction(work(), null, () -> {
            executor.executeUpdate("UPDATE t SET a = 1");
            executor.executeUpdate("UPDATE t SET b = 2");
            return null;
        });
        verify(controller, times(1)).getConnection("default");
        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection).setAutoCommit(true);
        verify(controller, times(1)).releaseConnection("default", connection);
        assertNull(executor.getTransactionConnection());
    }

    @Test
    void failedTransactionRollsBackAndReleases() throws Throwable {
        TransactionManager tm = new TransactionManager(executor);
        assertThrows(IllegalStateException.class, () -> tm.executeInTransaction(work(), null, () -> {
            executor.executeUpdate("UPDATE t SET a = 1");
            throw new IllegalStateException("boom");
        }));
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(controller).releaseConnection("default", connection);
    }

    @Test
    void transactionAcrossRepositoriesSharesConnectionViaSavepoint() throws Throwable {
        TransactionManager outer = new TransactionManager(executor);
        TransactionManager inner = new TransactionManager(executor);
        outer.executeInTransaction(work(), null, () ->
                inner.executeInTransaction(work(), null, () -> {
                    executor.executeUpdate("UPDATE t SET a = 1");
                    return null;
                }));
        verify(controller, times(1)).getConnection("default");
        verify(connection).setSavepoint();
        verify(connection, times(1)).commit();
        verify(controller, times(1)).releaseConnection("default", connection);
    }

    @Test
    void executeTransactionJoinsBoundTransaction() throws SQLException {
        Connection bound = mock(Connection.class);
        when(bound.prepareStatement(anyString())).thenReturn(statement);
        executor.bindTransactionConnection(bound);
        try {
            executor.executeTransaction(java.util.List.of("UPDATE t SET a = 1"));
            verify(bound, never()).commit();
            verify(bound, never()).setAutoCommit(anyBoolean());
        } finally {
            executor.bindTransactionConnection(null);
        }
    }

    private static Method work() throws NoSuchMethodException {
        return Repo.class.getMethod("work");
    }

    @Test
    void runnableTransactionBindsItsConnectionAndRollsBack() throws SQLException {
        Connection[] seen = new Connection[1];
        assertThrows(IllegalStateException.class, () -> executor.executeTransaction((Runnable) () -> {
            seen[0] = executor.getTransactionConnection();
            throw new IllegalStateException("fail");
        }));
        assertSame(connection, seen[0]);
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(controller).releaseConnection("default", connection);
        assertNull(executor.getTransactionConnection());
    }

    @Test
    void preparedStatementTransactionRunsOnTheStatementsConnection() throws SQLException {
        Connection own = mock(Connection.class);
        when(own.getAutoCommit()).thenReturn(true);
        PreparedStatement first = mock(PreparedStatement.class);
        PreparedStatement second = mock(PreparedStatement.class);
        when(first.getConnection()).thenReturn(own);
        when(second.getConnection()).thenReturn(own);

        executor.executeTransaction(first, second);

        verify(own).commit();
        verify(controller, never()).getConnection(anyString());
    }

    @Test
    void preparedStatementsFromDifferentConnectionsAreRejected() throws SQLException {
        PreparedStatement first = mock(PreparedStatement.class);
        PreparedStatement second = mock(PreparedStatement.class);
        when(first.getConnection()).thenReturn(mock(Connection.class));
        when(second.getConnection()).thenReturn(mock(Connection.class));
        assertThrows(SQLException.class, () -> executor.executeTransaction(first, second));
    }
}
