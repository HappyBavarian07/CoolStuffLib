package de.happybavarian07.coolstufflib.jpa.utils;

import de.happybavarian07.coolstufflib.jpa.SQLExecutor;
import de.happybavarian07.coolstufflib.jpa.annotations.Column;
import de.happybavarian07.coolstufflib.jpa.annotations.Id;
import de.happybavarian07.coolstufflib.jpa.annotations.PostLoad;
import de.happybavarian07.coolstufflib.jpa.annotations.Table;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        SqlSafe.validateOperator(op);
        return addCondition("AND", fieldName, op + " ?", value);
    }

    public EntityQueryBuilder<T> and(String fieldName, String op, Object value) {
        SqlSafe.validateOperator(op);
        return addCondition("AND", fieldName, op + " ?", value);
    }

    public EntityQueryBuilder<T> or(String fieldName, String op, Object value) {
        SqlSafe.validateOperator(op);
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
        return selectAll(limitValue, offsetValue);
    }

    public Optional<T> findFirst() {
        List<T> results = selectAll(1, offsetValue);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    private List<T> selectAll(int limit, int offset) {
        String sql = buildSelectQuery(limit, offset);
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

    /** Counts every row the conditions match; the paging set by {@link #limit(int)} and {@link #offset(int)}
     * is deliberately ignored, because a count describes the whole result set, not one page of it. */
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

    private String buildSelectQuery(int limit, int offset) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(databasePrefix).append(tableName);
        appendClauses(sql, true, limit, offset);
        return sql.toString();
    }

    private String buildCountQuery() {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ").append(databasePrefix).append(tableName);
        appendClauses(sql, false); // No ORDER BY or LIMIT for count
        return sql.toString();
    }

    private String buildDeleteQuery() {
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(databasePrefix).append(tableName);
        if (limitValue > 0 || offsetValue > 0) {
            return buildBoundedDeleteQuery(sql);
        }
        appendClauses(sql, false); // No ORDER BY or LIMIT for a delete the caller did not bound
        return sql.toString();
    }

    /**
     * DELETE ... LIMIT is not portable, so a paged delete deletes the rows of a bounded subquery instead.
     * The extra derived table is what MySQL needs before it allows the target table in the subquery.
     * Without an id column the rows cannot be bounded, and deleting them all is not what the caller asked for.
     */
    private String buildBoundedDeleteQuery(StringBuilder sql) {
        String idColumn = getIdColumnName();
        if (idColumn == null) {
            throw new IllegalStateException("Cannot apply limit/offset to a delete on " + entityClass.getName()
                    + " because it has no @Id field to bound the deleted rows with");
        }
        StringBuilder bounded = new StringBuilder("SELECT ").append(idColumn).append(" FROM (SELECT ")
                .append(idColumn).append(" FROM ").append(databasePrefix).append(tableName);
        appendClauses(bounded, true);
        bounded.append(") AS bounded_rows");
        return sql.append(" WHERE ").append(idColumn).append(" IN (").append(bounded).append(")").toString();
    }

    private void appendClauses(StringBuilder sql, boolean includeOrderByAndLimit) {
        appendClauses(sql, includeOrderByAndLimit, limitValue, offsetValue);
    }

    private void appendClauses(StringBuilder sql, boolean includeOrderByAndLimit, int limit, int offset) {
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" ", conditions));
        }
        if (includeOrderByAndLimit) {
            if (!orderByColumns.isEmpty()) {
                sql.append(" ORDER BY ").append(String.join(", ", orderByColumns));
            }
            if (limit > 0) {
                sql.append(" LIMIT ").append(limit);
            }
            if (offset > 0) {
                sql.append(" OFFSET ").append(offset);
            }
        }
    }

    private String getTableName(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(Table.class)) {
            String annotated = entityClass.getAnnotation(Table.class).name();
            if (annotated != null && !annotated.isEmpty()) {
                return annotated;
            }
        }
        return entityClass.getSimpleName().toLowerCase();
    }

    private String getIdColumnName() {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return resolveColumnName(field);
            }
        }
        return null;
    }

    private String getColumnName(String fieldName) {
        Field field = findDeclaredField(fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Entity " + entityClass.getName() + " has no field '" + fieldName
                    + "'; queryable fields: " + getDeclaredFieldNames());
        }
        String result = resolveColumnName(field);
        SqlSafe.validateIdentifier(result);
        return result;
    }

    private String resolveColumnName(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            String annotated = field.getAnnotation(Column.class).name();
            if (annotated != null && !annotated.isEmpty()) {
                return annotated;
            }
        }
        return field.getName();
    }

    private Field findDeclaredField(String fieldName) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    private String getDeclaredFieldNames() {
        List<String> names = new ArrayList<>();
        for (Field field : entityClass.getDeclaredFields()) {
            names.add(field.getName());
        }
        return names.toString();
    }

    /** Mirrors the mapping {@code RepositoryProxy} does, so both paths return equally populated entities. */
    private T mapResultSetToEntity(ResultSet rs) {
        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            for (Field field : entityClass.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Column.class) && !field.isAnnotationPresent(Id.class)) {
                    continue;
                }
                field.setAccessible(true);
                Object value = readColumn(rs, field);
                if (value != null) {
                    field.set(entity, FieldTypeCaster.castToFieldType(field.getType(), value));
                }
            }
            invokeLifecycleMethod(entity, PostLoad.class);
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Error mapping ResultSet to entity", e);
        }
    }

    /** First value found under any column name this field may be stored as, so an id declared only with
     * {@link Id} and a snake_case column both resolve. */
    private Object readColumn(ResultSet rs, Field field) {
        for (String columnName : getPossibleColumnNames(field)) {
            try {
                Object value = rs.getObject(columnName);
                if (value != null) return value;
            } catch (SQLException ignored) {
            }
        }
        return null;
    }

    private List<String> getPossibleColumnNames(Field field) {
        List<String> names = new ArrayList<>();
        if (field.isAnnotationPresent(Column.class)) {
            String annotated = field.getAnnotation(Column.class).name();
            if (annotated != null && !annotated.isEmpty()) names.add(annotated);
        }
        String camel = field.getName();
        names.add(camel);
        StringBuilder snake = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                snake.append('_').append(Character.toLowerCase(c));
            } else {
                snake.append(c);
            }
        }
        String snakeStr = snake.toString();
        if (!snakeStr.equals(camel)) names.add(snakeStr);
        return names;
    }

    private void invokeLifecycleMethod(Object entity, Class<? extends Annotation> lifecycleAnnotation) {
        for (Method method : entity.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(lifecycleAnnotation)) {
                try {
                    method.setAccessible(true);
                    method.invoke(entity);
                } catch (Exception e) {
                    throw new RuntimeException("Error invoking lifecycle method: " + method.getName(), e);
                }
            }
        }
    }
}

