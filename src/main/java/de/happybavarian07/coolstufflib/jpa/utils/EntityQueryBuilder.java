package de.happybavarian07.coolstufflib.jpa.utils;

import de.happybavarian07.coolstufflib.jpa.SQLExecutor;
import de.happybavarian07.coolstufflib.jpa.annotations.Column;
import de.happybavarian07.coolstufflib.jpa.annotations.Table;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class EntityQueryBuilder<T> {

    private final Class<T> entityClass;
    private final SQLExecutor sqlExecutor;
    private final String databasePrefix;
    private final String tableName;

    private final List<Object> parameters = new ArrayList<>();
    private final List<String> conditions = new ArrayList<>();
    private final List<String> orderByColumns = new ArrayList<>();
    private int limitValue = -1;
    private int offsetValue = -1;

    public EntityQueryBuilder(Class<T> entityClass, SQLExecutor sqlExecutor, String databasePrefix) {
        this.entityClass = entityClass;
        this.sqlExecutor = sqlExecutor;
        this.databasePrefix = databasePrefix;
        this.tableName = getTableName(entityClass);
    }

    private EntityQueryBuilder<T> addCondition(String operator, String fieldName, String condition, Object... values) {
        if (!conditions.isEmpty()) {
            conditions.add(operator);
        }
        conditions.add(getColumnName(fieldName) + " " + condition);
        parameters.addAll(Arrays.asList(values));
        return this;
    }

    public EntityQueryBuilder<T> where(String fieldName, String op, Object value) {
        return addCondition("AND", fieldName, op + " ?", value);
    }

    public EntityQueryBuilder<T> and(String fieldName, String op, Object value) {
        return addCondition("AND", fieldName, op + " ?", value);
    }

    public EntityQueryBuilder<T> or(String fieldName, String op, Object value) {
        return addCondition("OR", fieldName, op + " ?", value);
    }

    public EntityQueryBuilder<T> and(Consumer<EntityQueryBuilder<T>> group) {
        return addGroup("AND", group);
    }

    public EntityQueryBuilder<T> or(Consumer<EntityQueryBuilder<T>> group) {
        return addGroup("OR", group);
    }

    private EntityQueryBuilder<T> addGroup(String operator, Consumer<EntityQueryBuilder<T>> group) {
        if (!conditions.isEmpty()) {
            conditions.add(operator);
        }
        EntityQueryBuilder<T> groupBuilder = new EntityQueryBuilder<>(entityClass, sqlExecutor, databasePrefix);
        group.accept(groupBuilder);

        if (!groupBuilder.conditions.isEmpty()) {
            conditions.add("(" + String.join(" ", groupBuilder.conditions) + ")");
            parameters.addAll(groupBuilder.parameters);
        }
        return this;
    }

    public EntityQueryBuilder<T> orderBy(String fieldName) {
        orderByColumns.add(getColumnName(fieldName) + " ASC");
        return this;
    }

    public EntityQueryBuilder<T> orderByDesc(String fieldName) {
        orderByColumns.add(getColumnName(fieldName) + " DESC");
        return this;
    }

    public EntityQueryBuilder<T> limit(int limit) {
        this.limitValue = limit;
        return this;
    }

    public EntityQueryBuilder<T> offset(int offset) {
        this.offsetValue = offset;
        return this;
    }

    public List<T> findAll() {
        String sql = buildSelectQuery();
        List<T> results = new ArrayList<>();
        try (ResultSet rs = sqlExecutor.executeQuery(sql, parameters.toArray())) {
            while (rs.next()) {
                results.add(mapResultSetToEntity(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error executing query", e);
        }
        return results;
    }

    public Optional<T> findFirst() {
        limit(1);
        List<T> results = findAll();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public long count() {
        String sql = buildCountQuery();
        try (ResultSet rs = sqlExecutor.executeQuery(sql, parameters.toArray())) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error executing count query", e);
        }
        return 0L;
    }

    public boolean exists() {
        return count() > 0;
    }

    public int delete() {
        String sql = buildDeleteQuery();
        try {
            return sqlExecutor.executeUpdate(sql, parameters.toArray());
        } catch (Exception e) {
            throw new RuntimeException("Error executing delete query", e);
        }
    }

    private String buildSelectQuery() {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(databasePrefix).append(tableName);
        appendClauses(sql);
        return sql.toString();
    }

    private String buildCountQuery() {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ").append(databasePrefix).append(tableName);
        appendClauses(sql, false); // No ORDER BY or LIMIT for count
        return sql.toString();
    }

    private String buildDeleteQuery() {
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(databasePrefix).append(tableName);
        appendClauses(sql, false); // No ORDER BY or LIMIT for delete
        return sql.toString();
    }

    private void appendClauses(StringBuilder sql) {
        appendClauses(sql, true);
    }

    private void appendClauses(StringBuilder sql, boolean includeOrderByAndLimit) {
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" ", conditions));
        }
        if (includeOrderByAndLimit) {
            if (!orderByColumns.isEmpty()) {
                sql.append(" ORDER BY ").append(String.join(", ", orderByColumns));
            }
            if (limitValue > 0) {
                sql.append(" LIMIT ").append(limitValue);
            }
            if (offsetValue > 0) {
                sql.append(" OFFSET ").append(offsetValue);
            }
        }
    }

    private String getTableName(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(Table.class)) {
            return entityClass.getAnnotation(Table.class).name();
        }
        return entityClass.getSimpleName().toLowerCase();
    }

    private String getColumnName(String fieldName) {
        try {
            Field field = entityClass.getDeclaredField(fieldName);
            if (field.isAnnotationPresent(Column.class)) {
                return field.getAnnotation(Column.class).name();
            }
        } catch (NoSuchFieldException e) {
            // Fallback to fieldName if not found, allows for raw column names
        }
        return fieldName;
    }

    private T mapResultSetToEntity(ResultSet rs) {
        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            for (Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Column.class)) {
                    field.setAccessible(true);
                    String columnName = field.getAnnotation(Column.class).name();
                    Object value = rs.getObject(columnName);
                    if (value != null) {
                        field.set(entity, value);
                    }
                }
            }
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Error mapping ResultSet to entity", e);
        }
    }
}

