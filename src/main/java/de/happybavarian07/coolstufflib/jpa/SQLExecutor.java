package de.happybavarian07.coolstufflib.jpa;

import de.happybavarian07.coolstufflib.jpa.annotations.Column;
import de.happybavarian07.coolstufflib.jpa.annotations.ElementCollection;
import de.happybavarian07.coolstufflib.jpa.annotations.Entity;
import de.happybavarian07.coolstufflib.jpa.annotations.Table;
import de.happybavarian07.coolstufflib.jpa.exceptions.MySQLSystemExceptions;
import de.happybavarian07.coolstufflib.jpa.interfaces.ResultSetValueConverter;
import de.happybavarian07.coolstufflib.jpa.utils.DatabaseProperties;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class SQLExecutor {
    private final RepositoryController controller;
    private final DatabaseProperties dbProperties;
    private String defaultConnection;

    public SQLExecutor(RepositoryController controller, DatabaseProperties dbProperties) {
        this.controller = controller;
        this.dbProperties = dbProperties;
    }

    public Connection getConnection(String name) throws SQLException {
        return controller.getConnection(name);
    }

    public void releaseConnection(String poolName, Connection connection) {
        controller.releaseConnection(poolName, connection);
    }

    public int executeUpdate(String sql, Object... params) throws SQLException {
        Connection conn = getConnection(defaultConnection);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindParameters(stmt, params);
            try {
                return stmt.executeUpdate();
            } catch (SQLException e) {
                StringBuilder paramStr = new StringBuilder();
                for (Object param : params) {
                    paramStr.append(param).append(", ");
                }
                throw new SQLException("Error executing update. SQL: " + sql + ", Params: [" + paramStr + "]", e);
            }
        } catch (SQLException e) {
            throw new SQLException("Error preparing statement. SQL: " + sql, e);
        } finally {
            releaseConnection(defaultConnection, conn);
        }
    }

    public ResultSet executeQuery(String sql, Object... params) throws SQLException {
        Connection conn = getConnection(defaultConnection);
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            bindParameters(stmt, params);
            try {
                return stmt.executeQuery();
            } catch (SQLException e) {
                StringBuilder paramStr = new StringBuilder();
                if (params != null) {
                    for (Object param : params) {
                        paramStr.append(param).append(", ");
                    }
                }
                throw new SQLException("Error executing query. SQL: " + sql + ", Params: [" + paramStr + "]", e);
            }
        } catch (SQLException e) {
            throw new SQLException("Error preparing statement. SQL: " + sql, e);
        } finally {
            releaseConnection(defaultConnection, conn);
        }
    }

    private void bindParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    private String getColumnDefinition(Field field) {
        if (!field.isAnnotationPresent(Column.class)) {
            throw new IllegalArgumentException("Field " + field.getName() + " has no Column annotation");
        }
        if (field.isAnnotationPresent(de.happybavarian07.coolstufflib.jpa.annotations.ElementCollection.class)) {
            return null;
        }
        Column column = field.getAnnotation(Column.class);
        String columnName = column.name().isEmpty() ? field.getName() : column.name();
        String sqlType = getSQLType(field);
        StringBuilder definition = new StringBuilder();
        definition.append(columnName).append(" ").append(sqlType);
        if (!column.nullable()) {
            definition.append(" NOT NULL");
        }
        if (column.unique()) {
            definition.append(" UNIQUE");
        }
        if (column.primaryKey()) {
            definition.append(" PRIMARY KEY");
        }
        if (column.autoIncrement()) {
            definition.append(" AUTO_INCREMENT");
        }
        return definition.toString();
    }

    private String generateCreateTableSQL(Class<?> entityClass, String tableName, String schema) {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        if (!schema.isEmpty()) {
            sql.append(schema).append(".");
        }
        sql.append(dbProperties.getDatabasePrefix()).append(tableName).append(" (");
        Field[] fields = entityClass.getDeclaredFields();
        List<String> columnDefinitions = new ArrayList<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class) && !field.isAnnotationPresent(de.happybavarian07.coolstufflib.jpa.annotations.ElementCollection.class)) {
                String def = getColumnDefinition(field);
                if (def != null) columnDefinitions.add(def);
            }
        }
        sql.append(String.join(", ", columnDefinitions));
        sql.append(");");
        return sql.toString();
    }

    public void generateSchema(Class<?> entityClass) throws SQLException {
        if (!entityClass.isAnnotationPresent(Entity.class) || !entityClass.isAnnotationPresent(Table.class)) {
            throw new IllegalArgumentException("Class must be annotated with @Entity and @Table");
        }
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        String tableName = tableAnnotation.name();
        String schema = tableAnnotation.schema();
        String fullTableName = (!schema.isEmpty() ? schema + "." : "") + dbProperties.getDatabasePrefix() + tableName;
        Connection conn = getConnection(defaultConnection);
        try {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(conn.getCatalog(), schema, dbProperties.getDatabasePrefix() + tableName, new String[]{"TABLE"});
            if (tables.next()) {
                Field[] fields = entityClass.getDeclaredFields();
                for (Field field : fields) {
                    if (!field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(ElementCollection.class)) continue;
                    Column colAnn = field.getAnnotation(Column.class);
                    String columnName = colAnn.name();
                    ResultSet cols = meta.getColumns(conn.getCatalog(), schema, dbProperties.getDatabasePrefix() + tableName, columnName);
                    if (!cols.next()) {
                        String alterSQL = "ALTER TABLE " + fullTableName + " ADD COLUMN " + getColumnDefinition(field) + ";";
                        try (Statement stmt = conn.createStatement()) {
                            stmt.executeUpdate(alterSQL);
                        } catch (SQLException e) {
                            throw new SQLException("Error executing ALTER TABLE. SQL: " + alterSQL + ", Field: " + field.getName(), e);
                        }
                    }
                    cols.close();
                }
            } else {
                String createTableSQL = generateCreateTableSQL(entityClass, tableName, schema);
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(createTableSQL);
                } catch (SQLException e) {
                    throw new SQLException("Error executing CREATE TABLE. SQL: " + createTableSQL, e);
                }
            }
            tables.close();
        } finally {
            releaseConnection(defaultConnection, conn);
        }
    }

    private String getSQLType(Field field) {
        if (field.isAnnotationPresent(ElementCollection.class)) {
            throw new IllegalArgumentException("ElementCollection fields are not supported in main table: " + field.getName());
        }
        Class<?> type = field.getType();
        if (type == int.class || type == Integer.class) {
            return "INT";
        } else if (type == long.class || type == Long.class) {
            return "BIGINT";
        } else if (type == double.class || type == Double.class) {
            return "DOUBLE";
        } else if (type == float.class || type == Float.class) {
            return "FLOAT";
        } else if (type == boolean.class || type == Boolean.class) {
            return "BOOLEAN";
        } else if (type == String.class) {
            return "VARCHAR(255)";
        } else if (type == Date.class) {
            return "DATETIME";
        } else if (type == UUID.class) {
            return "VARCHAR(36)";
        } else if (type.isEnum()) {
            return "VARCHAR(255)";
        } else if (type == byte[].class) {
            return "BLOB";
        } else if (type == short.class || type == Short.class) {
            return "SMALLINT";
        } else if (type == byte.class || type == Byte.class) {
            return "TINYINT";
        }
        throw new IllegalArgumentException("Unsupported field type: " + type);
    }

    public void setDefaultConnection(String name) {
        this.defaultConnection = name;
    }

    public String getDefaultConnection() {
        return defaultConnection;
    }

    public void executeBatchUpdate(String sql, List<Object[]> paramsList) throws SQLException {
        Connection conn = getConnection(defaultConnection);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Object[] params : paramsList) {
                bindParameters(stmt, params);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } finally {
            releaseConnection(defaultConnection, conn);
        }
    }

    public void executeBatchUpdate(String sql, Object[][] paramsArray) throws SQLException {
        Connection conn = getConnection(defaultConnection);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Object[] params : paramsArray) {
                bindParameters(stmt, params);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } finally {
            releaseConnection(defaultConnection, conn);
        }
    }

    public void executeBatchQuery(String sql, List<Object[]> paramsList) throws SQLException {
        Connection conn = getConnection(defaultConnection);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Object[] params : paramsList) {
                bindParameters(stmt, params);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } finally {
            releaseConnection(defaultConnection, conn);
        }
    }

    public void executeBatchQuery(String sql, Object[][] paramsArray) throws SQLException {
        Connection conn = getConnection(defaultConnection);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Object[] params : paramsArray) {
                bindParameters(stmt, params);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } finally {
            releaseConnection(defaultConnection, conn);
        }
    }

    public void executeTransaction(List<String> sqlStatements) throws SQLException {
        Connection connection = getConnection(defaultConnection);
        try {
            connection.setAutoCommit(false);
            for (String sql : sqlStatements) {
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.executeUpdate();
                }
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
            releaseConnection(defaultConnection, connection);
        }
    }

    public void executeTransaction(String... sqlStatements) throws SQLException {
        executeTransaction(Arrays.asList(sqlStatements));
    }

    public void executeTransaction(PreparedStatement... preparedStatements) throws SQLException {
        Connection connection = getConnection(defaultConnection);
        try {
            connection.setAutoCommit(false);
            for (PreparedStatement stmt : preparedStatements) {
                stmt.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
            releaseConnection(defaultConnection, connection);
        }
    }

    public void executeTransaction(Runnable... operations) throws SQLException {
        Connection connection = getConnection(defaultConnection);
        try {
            connection.setAutoCommit(false);
            for (Runnable operation : operations) {
                operation.run();
            }
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw new SQLException("Transaction failed", e);
        } finally {
            connection.setAutoCommit(true);
            releaseConnection(defaultConnection, connection);
        }
    }

    public <T> List<T> mapResultSet(ResultSet resultSet, Class<T> type) throws SQLException {
        List<T> results = new ArrayList<>();
        try {
            while (resultSet.next()) {
                T instance = type.getDeclaredConstructor().newInstance();
                for (Field field : type.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Column.class)) {
                        Column column = field.getAnnotation(Column.class);
                        String columnName = column.name();
                        Object value = resultSet.getObject(columnName);
                        ResultSetValueConverter converter = null;
                        if (!column.converter().equals(ResultSetValueConverter.class)) {
                            converter = column.converter().getDeclaredConstructor().newInstance();
                        }
                        value = convertValue(field, value, Optional.ofNullable(converter));
                        field.setAccessible(true);
                        field.set(instance, value);
                    }
                }
                results.add(instance);
            }
        } catch (Exception e) {
            throw new SQLException("Error mapping ResultSet to object", e);
        }
        return results;
    }

    private Object convertValue(Field field, Object value, Optional<ResultSetValueConverter> converter) throws MySQLSystemExceptions.OutputConversionException {
        if (value != null) {
            Class<?> type = field.getType();
            switch (type.getSimpleName()) {
                case "UUID":
                    if (value instanceof String) {
                        return UUID.fromString((String) value);
                    }
                    break;
                case "Date":
                    if (value instanceof Timestamp) {
                        return new Date(((Timestamp) value).getTime());
                    }
                    if (value instanceof String) {
                        try {
                            return new Date(Long.parseLong((String) value));
                        } catch (NumberFormatException e) {
                            throw new MySQLSystemExceptions.OutputConversionException(
                                    "Failed to convert value to Date: " + value, e);
                        }
                    }
                    break;
                case "Boolean":
                case "boolean":
                    if (value instanceof Number) {
                        return ((Number) value).intValue() != 0;
                    }
                    if (value instanceof String) {
                        return Boolean.parseBoolean((String) value);
                    }
                    break;
                case "Integer":
                case "int":
                    if (value instanceof Number) {
                        return ((Number) value).intValue();
                    }
                    if (value instanceof String) {
                        return Integer.parseInt((String) value);
                    }
                    break;
                case "Long":
                case "long":
                    if (value instanceof Number) {
                        return ((Number) value).longValue();
                    }
                    if (value instanceof String) {
                        return Long.parseLong((String) value);
                    }
                    break;
                case "Double":
                case "double":
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    }
                    if (value instanceof String) {
                        return Double.parseDouble((String) value);
                    }
                    break;
                default:
                    if (converter.isPresent()) {
                        return converter.get().convert(field, value);
                    }
                    break;
            }
            return value;
        }
        return null;
    }

    public void setDatabasePrefix(String prefix) {
        dbProperties.setDatabasePrefix(prefix);
    }
}
