package de.happybavarian07.coolstufflib.jpa;

import de.happybavarian07.coolstufflib.jpa.connection.ConnectionPool;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConnectionPoolTest {
    private final AtomicInteger created = new AtomicInteger();

    private ConnectionPool pool(int initial, int max) throws SQLException {
        return new ConnectionPool(() -> {
            created.incrementAndGet();
            return mock(Connection.class);
        }, initial, max, Duration.ofMillis(100));
    }

    @Test
    void reusesReleasedConnections() throws SQLException {
        ConnectionPool pool = pool(1, 2);
        Connection first = pool.getConnection();
        pool.releaseConnection(first);
        assertSame(first, pool.getConnection());
        assertEquals(1, created.get());
    }

    @Test
    void exhaustedPoolTimesOut() throws SQLException {
        ConnectionPool pool = pool(0, 2);
        pool.getConnection();
        pool.getConnection();
        SQLException ex = assertThrows(SQLException.class, pool::getConnection);
        assertTrue(ex.getMessage().contains("exhausted"));
    }

    @Test
    void doubleReleaseDoesNotInflateCapacity() throws SQLException {
        ConnectionPool pool = pool(0, 1);
        Connection connection = pool.getConnection();
        pool.releaseConnection(connection);
        pool.releaseConnection(connection);
        pool.getConnection();
        assertThrows(SQLException.class, pool::getConnection);
    }

    @Test
    void closedIdleConnectionsAreReplaced() throws SQLException {
        ConnectionPool pool = pool(0, 1);
        Connection connection = pool.getConnection();
        pool.releaseConnection(connection);
        when(connection.isClosed()).thenReturn(true);
        assertNotSame(connection, pool.getConnection());
        assertEquals(2, created.get());
    }

    @Test
    void factoryFailureReturnsThePermit() throws SQLException {
        ConnectionPool pool = new ConnectionPool(() -> {
            throw new SQLException("down");
        }, 0, 1, Duration.ofMillis(100));
        assertThrows(SQLException.class, pool::getConnection);
        SQLException second = assertThrows(SQLException.class, pool::getConnection);
        assertEquals("down", second.getMessage());
    }

    @Test
    void closeAllClosesUsedAndIdle() throws SQLException {
        ConnectionPool pool = pool(1, 3);
        Connection used = pool.getConnection();
        Connection idle = pool.getConnection();
        pool.releaseConnection(idle);
        pool.closeAllConnections();
        verify(used).close();
        verify(idle).close();
        assertEquals(0, pool.getUsedConnectionsCount());
        assertEquals(0, pool.getFreeConnectionsCount());
    }

    @Test
    void countsStayConsistentUnderConcurrency() throws Exception {
        ConnectionPool pool = new ConnectionPool(() -> mock(Connection.class), 0, 4, Duration.ofSeconds(5));
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            futures.add(executor.submit(() -> {
                Connection connection = pool.getConnection();
                assertTrue(pool.getUsedConnectionsCount() <= 4);
                pool.releaseConnection(connection);
                return null;
            }));
        }
        for (Future<?> future : futures) future.get();
        executor.shutdown();
        assertEquals(0, pool.getUsedConnectionsCount());
        assertTrue(pool.getFreeConnectionsCount() <= 4);
    }
}
