package de.happybavarian07.coolstufflib.jpa.transaction;

import de.happybavarian07.coolstufflib.jpa.SQLExecutor;
import de.happybavarian07.coolstufflib.jpa.annotations.Transactional;

import java.lang.ThreadLocal;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionManager {
    private final SQLExecutor sqlExecutor;
    private final ThreadLocal<TransactionContext> currentTransaction = new ThreadLocal<>();
    private final ConcurrentHashMap<String, Connection> connections = new ConcurrentHashMap<>();

    public TransactionManager(SQLExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }

    public <T> T executeInTransaction(Method method, Object[] args, TransactionalOperation<T> operation) throws Throwable {
        Transactional annotation = method.getAnnotation(Transactional.class);
        if (annotation == null) {
            return operation.execute();
        }

        if (currentTransaction.get() != null || sqlExecutor.getTransactionConnection() != null) {
            return executeNestedTransaction(annotation, operation);
        }

        return executeNewTransaction(annotation, operation);
    }

    private <T> T executeNewTransaction(Transactional annotation, TransactionalOperation<T> operation) throws Throwable {
        String pool = sqlExecutor.getDefaultConnection();
        Connection connection = sqlExecutor.getConnection(pool);
        if (connection == null) {
            throw new SQLException("No database connection available");
        }

        TransactionContext context = new TransactionContext(connection, annotation.readOnly());
        currentTransaction.set(context);
        sqlExecutor.bindTransactionConnection(connection);

        try {
            connection.setAutoCommit(false);
            if (annotation.readOnly()) {
                connection.setReadOnly(true);
            }

            T result = operation.execute();

            if (!context.isRollbackOnly()) {
                connection.commit();
            } else {
                connection.rollback();
            }

            return result;
        } catch (Throwable e) {
            try {
                if (shouldRollback(annotation, e)) {
                    connection.rollback();
                } else {
                    connection.commit();
                }
            } catch (SQLException completionFailure) {
                e.addSuppressed(completionFailure);
            }
            throw e;
        } finally {
            try {
                connection.setAutoCommit(true);
                connection.setReadOnly(false);
            } catch (SQLException ignored) {}
            currentTransaction.remove();
            sqlExecutor.bindTransactionConnection(null);
            sqlExecutor.releaseConnection(pool, connection);
        }
    }

    private <T> T executeNestedTransaction(Transactional annotation, TransactionalOperation<T> operation) throws Throwable {
        TransactionContext context = currentTransaction.get();
        Connection connection = context != null ? context.getConnection() : sqlExecutor.getTransactionConnection();
        Savepoint savepoint = null;

        try {
            savepoint = connection.setSavepoint();
            T result = operation.execute();

            if (context != null && context.isRollbackOnly()) {
                connection.rollback(savepoint);
            }

            return result;
        } catch (Throwable e) {
            if (savepoint != null && shouldRollback(annotation, e)) {
                connection.rollback(savepoint);
            }
            throw e;
        }
    }

    private boolean shouldRollback(Transactional annotation, Throwable throwable) {
        for (Class<? extends Throwable> noRollbackClass : annotation.noRollbackFor()) {
            if (noRollbackClass.isAssignableFrom(throwable.getClass())) {
                return false;
            }
        }

        for (Class<? extends Throwable> rollbackClass : annotation.rollbackFor()) {
            if (rollbackClass.isAssignableFrom(throwable.getClass())) {
                return true;
            }
        }

        return throwable instanceof RuntimeException || throwable instanceof Error;
    }

    public void setRollbackOnly() {
        TransactionContext context = currentTransaction.get();
        if (context != null) {
            context.setRollbackOnly(true);
        }
    }

    public boolean isTransactionActive() {
        return currentTransaction.get() != null;
    }

    public Connection getCurrentConnection() {
        TransactionContext context = currentTransaction.get();
        return context != null ? context.getConnection() : null;
    }

    public TransactionContext getTransactionContext() {
        return currentTransaction.get();
    }

    public void setTransactionContext(TransactionContext context) {
        if (context == null) {
            currentTransaction.remove();
            sqlExecutor.bindTransactionConnection(null);
        } else {
            currentTransaction.set(context);
            sqlExecutor.bindTransactionConnection(context.getConnection());
        }
    }

    @FunctionalInterface
    public interface TransactionalOperation<T> {
        T execute() throws Throwable;
    }

    public static class TransactionContext {
        private final Connection connection;
        private final boolean readOnly;
        private boolean rollbackOnly;

        public TransactionContext(Connection connection, boolean readOnly) {
            this.connection = connection;
            this.readOnly = readOnly;
            this.rollbackOnly = false;
        }

        public Connection getConnection() {
            return connection;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        public boolean isRollbackOnly() {
            return rollbackOnly;
        }

        public void setRollbackOnly(boolean rollbackOnly) {
            this.rollbackOnly = rollbackOnly;
        }
    }
}
