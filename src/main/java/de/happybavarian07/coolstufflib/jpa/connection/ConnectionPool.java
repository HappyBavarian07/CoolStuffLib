package de.happybavarian07.coolstufflib.jpa.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ConnectionPool {
    private static final int SQLITE_BUSY_TIMEOUT_MILLIS = 5000;

    private final ConnectionFactory factory;
    private final int maxPoolSize;
    private final long acquireTimeoutMillis;
    private final Semaphore permits;
    private final LinkedBlockingQueue<Connection> idleConnections = new LinkedBlockingQueue<>();
    private final Set<Connection> usedConnections = ConcurrentHashMap.newKeySet();

    public ConnectionPool(String url, String user, String password, int initialPoolSize, int maxPoolSize) throws SQLException {
        this(() -> openConnection(url, user, password), initialPoolSize, maxPoolSize, Duration.ofSeconds(5));
    }

    public ConnectionPool(ConnectionFactory factory, int initialPoolSize, int maxPoolSize, Duration acquireTimeout) throws SQLException {
        this.factory = factory;
        this.maxPoolSize = maxPoolSize;
        this.acquireTimeoutMillis = acquireTimeout.toMillis();
        this.permits = new Semaphore(maxPoolSize, true);
        for (int i = 0; i < Math.min(initialPoolSize, maxPoolSize); i++) {
            idleConnections.offer(factory.create());
        }
    }

    public Connection getConnection() throws SQLException {
        try {
            if (!permits.tryAcquire(acquireTimeoutMillis, TimeUnit.MILLISECONDS)) {
                throw new SQLException("Connection pool exhausted. Used: " + usedConnections.size() + ", Max: " + maxPoolSize);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for a connection.", e);
        }
        try {
            Connection connection = idleConnections.poll();
            while (connection != null && connection.isClosed()) {
                connection = idleConnections.poll();
            }
            if (connection == null) connection = factory.create();
            usedConnections.add(connection);
            return connection;
        } catch (SQLException | RuntimeException e) {
            permits.release();
            throw e;
        }
    }

    public void releaseConnection(Connection connection) {
        if (connection != null && usedConnections.remove(connection)) {
            idleConnections.offer(connection);
            permits.release();
        }
    }

    public void closeAllConnections() throws SQLException {
        SQLException failure = null;
        for (Connection connection : usedConnections) failure = close(connection, failure);
        for (Connection connection : idleConnections) failure = close(connection, failure);
        usedConnections.clear();
        idleConnections.clear();
        if (failure != null) throw failure;
    }

    public int getUsedConnectionsCount() {
        return usedConnections.size();
    }

    public int getFreeConnectionsCount() {
        return idleConnections.size();
    }

    private static SQLException close(Connection connection, SQLException failure) {
        try {
            connection.close();
        } catch (SQLException e) {
            if (failure == null) return e;
            failure.addSuppressed(e);
        }
        return failure;
    }

    private static Connection openConnection(String url, String user, String password) throws SQLException {
        Connection connection = DriverManager.getConnection(url, user, password);
        if (url.startsWith("jdbc:sqlite:")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA busy_timeout = " + SQLITE_BUSY_TIMEOUT_MILLIS);
            }
        }
        return connection;
    }

    @FunctionalInterface
    public interface ConnectionFactory {
        Connection create() throws SQLException;
    }
}
