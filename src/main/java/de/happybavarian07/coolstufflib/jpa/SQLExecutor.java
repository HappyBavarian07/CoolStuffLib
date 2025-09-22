package de.happybavarian07.coolstufflib.jpa;

import de.happybavarian07.coolstufflib.jpa.annotations.*;
import de.happybavarian07.coolstufflib.jpa.exceptions.MySQLSystemExceptions;
import de.happybavarian07.coolstufflib.jpa.interfaces.ResultSetValueConverter;
import de.happybavarian07.coolstufflib.jpa.utils.DatabaseProperties;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
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
                ResultSet rs = stmt.executeQuery();
                InvocationHandler handler = (proxy, method, args) -> {
                    String name = method.getName();
                    if ("close".equals(name)) {
                        try {
                            rs.close();
                        } catch (SQLException ignored) {
                        }
                        try {
                            stmt.close();
                        } catch (SQLException ignored) {
                        }
                        releaseConnection(defaultConnection, conn);
                        return null;
                    }
                    try {
                        return method.invoke(rs, args);
                    } catch (Throwable t) {
                        Throwable cause = t instanceof java.lang.reflect.InvocationTargetException ite ? ite.getCause() : t;
                        if (cause instanceof SQLException se) {
                            StringBuilder paramStr = new StringBuilder();
                            if (params != null) {
                                for (Object p : params) paramStr.append(p).append(", ");
                            }
                            throw new SQLException("Error executing query method '" + name + "'. SQL: " + sql + ", Params: [" + paramStr + "]", se);
                        }
                        throw t;
                    }
                };
                return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class[]{ResultSet.class}, handler);
            } catch (SQLException e) {
                StringBuilder paramStr = new StringBuilder();
                if (params != null) {
                    for (Object param : params) paramStr.append(param).append(", ");
                }
                try {
                    stmt.close();
                } catch (SQLException ignored) {
                }
                releaseConnection(defaultConnection, conn);
                throw new SQLException("Error executing query. SQL: " + sql + ", Params: [" + paramStr + "]", e);
            }
        } catch (SQLException e) {
            try {
                releaseConnection(defaultConnection, conn);
            } catch (Exception ignored) {
            }
            throw new SQLException("Error preparing statement. SQL: " + sql, e);
        }
    }

    private void bindParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
            Object p = params[i];
            int idx = i + 1;
            if (p == null) {
                stmt.setObject(idx, null);
            } else if (p instanceof UUID u) {
                stmt.setString(idx, u.toString());
            } else if (p instanceof Date d) {
                stmt.setTimestamp(idx, new Timestamp(d.getTime()));
            } else if (p instanceof java.time.Instant inst) {
                stmt.setTimestamp(idx, Timestamp.from(inst));
            } else if (p instanceof java.time.LocalDateTime ldt) {
                stmt.setTimestamp(idx, Timestamp.valueOf(ldt));
            } else if (p instanceof java.time.LocalDate ld) {
                stmt.setDate(idx, java.sql.Date.valueOf(ld));
            } else if (p instanceof java.time.LocalTime lt) {
                stmt.setTime(idx, java.sql.Time.valueOf(lt));
            } else if (p instanceof Enum<?> e) {
                stmt.setString(idx, e.name());
            } else if (p instanceof Boolean b) {
                stmt.setBoolean(idx, b);
            } else if (p instanceof byte[] ba) {
                stmt.setBytes(idx, ba);
            } else {
                stmt.setObject(idx, p);
            }
        }
    }

    private String getColumnDefinition(Field field) {
        if (!field.isAnnotationPresent(Column.class)) {
            throw new IllegalArgumentException("Field " + field.getName() + " has no Column annotation");
        }
        if (field.isAnnotationPresent(ElementCollection.class)) {
            return null;
        }
        Column column = field.getAnnotation(Column.class);
        String columnName = column.name().isEmpty() ? field.getName() : column.name();
        String sqlType = getSQLType(field);
        StringBuilder definition = new StringBuilder();
        definition.append(columnName).append(" ").append(sqlType);
        if (!column.nullable()) definition.append(" NOT NULL");
        if (column.unique()) definition.append(" UNIQUE");
        if (column.primaryKey()) definition.append(" PRIMARY KEY");
        if (column.autoIncrement()) definition.append(" AUTO_INCREMENT");
        return definition.toString();
    }

    private String generateCreateTableSQL(Class<?> entityClass, String tableName, String schema) {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        if (!schema.isEmpty()) sql.append(schema).append(".");
        sql.append(dbProperties.getDatabasePrefix()).append(tableName).append(" (");
        Field[] fields = entityClass.getDeclaredFields();
        List<String> columnDefinitions = new ArrayList<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class) && !field.isAnnotationPresent(ElementCollection.class)) {
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
        String schemaParam = schema.isEmpty() ? null : schema;
        String fullTableName = (!schema.isEmpty() ? schema + "." : "") + dbProperties.getDatabasePrefix() + tableName;
        String createTableSQL = generateCreateTableSQL(entityClass, tableName, schema);

        Connection conn = getConnection(defaultConnection);
        try {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(conn.getCatalog(), schemaParam, dbProperties.getDatabasePrefix() + tableName, new String[]{"TABLE"});
            if (tables.next()) {
                Field[] fields = entityClass.getDeclaredFields();
                for (Field field : fields) {
                    if (!field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(ElementCollection.class))
                        continue;
                    Column colAnn = field.getAnnotation(Column.class);
                    String columnName = colAnn.name().isEmpty() ? field.getName() : colAnn.name();
                    ResultSet cols = meta.getColumns(conn.getCatalog(), schemaParam, dbProperties.getDatabasePrefix() + tableName, columnName);
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
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(createTableSQL);
                } catch (SQLException e) {
                    throw new SQLException("Error executing CREATE TABLE. SQL: " + createTableSQL, e);
                }
            }
            tables.close();
            for (Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(ElementCollection.class)) {
                    createElementCollectionTable(entityClass, field, tableName, schema);
                }
            }
        } finally {
            releaseConnection(defaultConnection, conn);
        }
    }

    public void createElementCollectionTable(Class<?> entityClass, Field field, String entityTableName, String schema) throws SQLException {
        ElementCollection ec = field.getAnnotation(ElementCollection.class);
        String collectionTable = (ec != null && !ec.tableName().isEmpty()) ? ec.tableName() : entityTableName + "_" + field.getName();
        String fkColumn = resolveIdColumnName(entityClass);
        String valueColumn;
        if (ec != null && !ec.columnName().isEmpty()) {
            valueColumn = ec.columnName();
        } else {
            Column colAnn = field.getAnnotation(Column.class);
            valueColumn = (colAnn != null && !colAnn.name().isEmpty()) ? colAnn.name() : "element";
        }
        Class<?> elementType = resolveCollectionElementType(field);
        String ownerSqlType = resolveIdSqlType(entityClass);
        String elementSqlType;
        try {
            elementSqlType = getSQLTypeForClass(elementType);
        } catch (IllegalArgumentException ex) {
            elementSqlType = "VARCHAR(255)";
        }
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        if (!schema.isEmpty()) sql.append(schema).append('.');
        sql.append(dbProperties.getDatabasePrefix()).append(collectionTable)
                .append(" (")
                .append(fkColumn).append(" ").append(ownerSqlType).append(" NOT NULL, ")
                .append(valueColumn).append(" ").append(elementSqlType).append(", ")
                .append("FOREIGN KEY(").append(fkColumn).append(") REFERENCES ")
                .append(dbProperties.getDatabasePrefix()).append(entityTableName)
                .append("(").append(fkColumn).append(") ON DELETE CASCADE")
                .append(")");
        Connection conn = getConnection(defaultConnection);
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql.toString());
        } catch (SQLException e) {
            throw new SQLException("Error creating @ElementCollection table. SQL: " + sql, e);
        } finally {
            releaseConnection(defaultConnection, conn);
        }
    }

    private String resolveIdColumnName(Class<?> entityClass) {
        for (Field f : entityClass.getDeclaredFields()) {
            if (f.isAnnotationPresent(Id.class)) {
                Column col = f.getAnnotation(Column.class);
                if (col != null && !col.name().isEmpty()) return col.name();
                return f.getName();
            }
        }
        throw new IllegalStateException("No @Id field found in entity class: " + entityClass.getName());
    }

    private String resolveIdSqlType(Class<?> entityClass) {
        for (Field f : entityClass.getDeclaredFields()) {
            if (f.isAnnotationPresent(Id.class)) {
                return getSQLTypeForClass(f.getType());
            }
        }
        throw new IllegalStateException("No @Id field found in entity class: " + entityClass.getName());
    }

    private Class<?> resolveCollectionElementType(Field field) {
        try {
            java.lang.reflect.Type gt = field.getGenericType();
            if (gt instanceof ParameterizedType pt) {
                java.lang.reflect.Type t = pt.getActualTypeArguments()[0];
                if (t instanceof Class<?> c) return c;
            }
        } catch (Exception ignored) {
        }
        return String.class;
    }

    private String getSQLType(Field field) {
        if (field.isAnnotationPresent(ElementCollection.class)) {
            throw new IllegalArgumentException("ElementCollection fields are not supported in main table: " + field.getName());
        }
        Class<?> type = field.getType();
        return getSQLTypeForClass(type);
    }

    public String getSQLTypeForClass(Class<?> type) {
        if (type == int.class || type == Integer.class) return "INT";
        else if (type == long.class || type == Long.class) return "BIGINT";
        else if (type == double.class || type == Double.class) return "DOUBLE";
        else if (type == float.class || type == Float.class) return "FLOAT";
        else if (type == boolean.class || type == Boolean.class) return "BOOLEAN";
        else if (type == String.class) return "VARCHAR(255)";
        else if (type == Date.class) return "DATETIME";
        else if (type == UUID.class) return "VARCHAR(36)";
        else if (type.isEnum()) return "VARCHAR(255)";
        else if (type == byte[].class) return "BLOB";
        else if (type == short.class || type == Short.class) return "SMALLINT";
        else if (type == byte.class || type == Byte.class) return "TINYINT";
        throw new IllegalArgumentException("Unsupported field type: " + type);
    }

    public String getDefaultConnection() {
        return defaultConnection;
    }

    public void setDefaultConnection(String name) {
        this.defaultConnection = name;
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
            throw e;
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
                    if (value instanceof String) return UUID.fromString((String) value);
                    break;
                case "Date":
                    if (value instanceof Timestamp) return new Date(((Timestamp) value).getTime());
                    if (value instanceof String) {
                        try {
                            return new Date(Long.parseLong((String) value));
                        } catch (NumberFormatException e) {
                            throw new MySQLSystemExceptions.OutputConversionException("Failed to convert value to Date: " + value, e);
                        }
                    }
                    break;
                case "Boolean":
                case "boolean":
                    if (value instanceof Number) return ((Number) value).intValue() != 0;
                    if (value instanceof String) return Boolean.parseBoolean((String) value);
                    break;
                case "Integer":
                case "int":
                    if (value instanceof Number) return ((Number) value).intValue();
                    if (value instanceof String) return Integer.parseInt((String) value);
                    break;
                case "Long":
                case "long":
                    if (value instanceof Number) return ((Number) value).longValue();
                    if (value instanceof String) return Long.parseLong((String) value);
                    break;
                case "Double":
                case "double":
                    if (value instanceof Number) return ((Number) value).doubleValue();
                    if (value instanceof String) return Double.parseDouble((String) value);
                    break;
                default:
                    if (converter.isPresent()) return converter.get().convert(field, value);
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
