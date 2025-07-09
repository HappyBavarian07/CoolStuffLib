package de.happybavarian07.coolstufflib.jpa.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ConnectionPool {
    private final String url;
    private final String user;
    private final String password;
    private final int maxPoolSize;
    private final BlockingQueue<Connection> connectionQueue;
    private final List<Connection> usedConnections = new ArrayList<>();

    public ConnectionPool(String url, String user, String password, int initialPoolSize, int maxPoolSize) throws SQLException {
        this.url = url;
        this.user = user;
        this.password = password;
        this.maxPoolSize = maxPoolSize;
        this.connectionQueue = new LinkedBlockingQueue<>(maxPoolSize);

        for (int i = 0; i < initialPoolSize; i++) {
            connectionQueue.offer(createConnection());
        }
    }

    public Connection getConnection() throws SQLException {
        try {
            if (usedConnections.size() < maxPoolSize) {
                if (connectionQueue.isEmpty()) {
                    Connection newConnection = createConnection();
                    usedConnections.add(newConnection);
                    return newConnection;
                }
                Connection connection = connectionQueue.poll(1, TimeUnit.SECONDS);
                if (connection == null) {
                    throw new SQLException("Timeout waiting for connection from pool.");
                }
                usedConnections.add(connection);
                return connection;
            }
            return connectionQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for a connection.", e);
        }
    }

    public void releaseConnection(Connection connection) {
        if (connection != null) {
            usedConnections.remove(connection);
            connectionQueue.offer(connection);
        }
    }

    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void closeAllConnections() throws SQLException {
        usedConnections.forEach(this::releaseConnection);
        for (Connection connection : connectionQueue) {
            connection.close();
        }
        connectionQueue.clear();
    }

    public int getUsedConnectionsCount() {
        return usedConnections.size();
    }

    public int getFreeConnectionsCount() {
        return connectionQueue.size();
    }
}
