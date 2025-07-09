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

        TransactionContext existingContext = currentTransaction.get();
        if (existingContext != null) {
            return executeNestedTransaction(annotation, operation);
        }

        return executeNewTransaction(annotation, operation);
    }

    private <T> T executeNewTransaction(Transactional annotation, TransactionalOperation<T> operation) throws Throwable {
        Connection connection = sqlExecutor.getConnection(sqlExecutor.getDefaultConnection());
        if (connection == null) {
            throw new SQLException("No database connection available");
        }

        TransactionContext context = new TransactionContext(connection, annotation.readOnly());
        currentTransaction.set(context);

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
            if (shouldRollback(annotation, e)) {
                connection.rollback();
            } else {
                connection.commit();
            }
            throw e;
        } finally {
            try {
                connection.setAutoCommit(true);
                connection.setReadOnly(false);
            } catch (SQLException ignored) {}
            currentTransaction.remove();
        }
    }

    private <T> T executeNestedTransaction(Transactional annotation, TransactionalOperation<T> operation) throws Throwable {
        TransactionContext context = currentTransaction.get();
        Savepoint savepoint = null;

        try {
            savepoint = context.getConnection().setSavepoint();
            T result = operation.execute();

            if (context.isRollbackOnly()) {
                context.getConnection().rollback(savepoint);
            }

            return result;
        } catch (Throwable e) {
            if (savepoint != null && shouldRollback(annotation, e)) {
                context.getConnection().rollback(savepoint);
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

    @FunctionalInterface
    public interface TransactionalOperation<T> {
        T execute() throws Throwable;
    }

    private static class TransactionContext {
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
