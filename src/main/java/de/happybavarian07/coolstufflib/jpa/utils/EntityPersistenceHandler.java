package de.happybavarian07.coolstufflib.jpa.utils;

import de.happybavarian07.coolstufflib.jpa.SQLExecutor;
import de.happybavarian07.coolstufflib.jpa.annotations.Column;
import de.happybavarian07.coolstufflib.jpa.annotations.Id;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class EntityPersistenceHandler {
    private final SQLExecutor sqlExecutor;
    private final String databasePrefix;
    private final ElementCollectionHandler elementCollectionHandler;

    EntityPersistenceHandler(SQLExecutor sqlExecutor, String databasePrefix, ElementCollectionHandler elementCollectionHandler) {
        this.sqlExecutor = sqlExecutor;
        this.databasePrefix = databasePrefix;
        this.elementCollectionHandler = elementCollectionHandler;
    }

    Object insertEntity(Class<?> entityClass, Object entity) {
        String tableName = entityClass.getSimpleName().toLowerCase();
        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);
                columns.add(field.getName());
                try {
                    values.add(field.get(entity));
                } catch (IllegalAccessException ignored) {}
            }
        }
        String sql = "INSERT INTO " + databasePrefix + tableName + " (" +
                String.join(", ", columns) + ") VALUES (" +
                String.join(", ", Collections.nCopies(columns.size(), "?")) + ")";
        try {
            sqlExecutor.executeUpdate(sql, values.toArray());
        } catch (SQLException e) {
            throw new RuntimeException("Error executing insertEntity SQL", e);
        }
        elementCollectionHandler.persistCollections(entityClass, entity, true);
        return entity;
    }

    Object updateEntity(Class<?> entityClass, Object entity) {
        String tableName = entityClass.getSimpleName().toLowerCase();
        String idColumn = getIdColumnName(entityClass);
        Object id = getEntityId(entity);
        List<String> setClauses = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class) &&
                    !field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                setClauses.add(field.getName() + " = ?");
                try {
                    values.add(field.get(entity));
                } catch (IllegalAccessException ignored) {}
            }
        }
        values.add(id);
        String sql = "UPDATE " + databasePrefix + tableName + " SET " +
                String.join(", ", setClauses) + " WHERE " + idColumn + " = ?";
        try {
            sqlExecutor.executeUpdate(sql, values.toArray());
        } catch (SQLException e) {
            throw new RuntimeException("Error executing updateEntity SQL", e);
        }
        elementCollectionHandler.persistCollections(entityClass, entity, false);
        return entity;
    }

    static Object getEntityId(Object entity) {
        for (Field field : entity.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                try {
                    return field.get(entity);
                } catch (IllegalAccessException ignored) {}
            }
        }
        return null;
    }

    static String getIdColumnName(Class<?> entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return field.getName();
            }
        }
        throw new IllegalStateException("No @Id field found in entity class: " + entityClass.getName());
    }

    static Class<?> getGenericTypeFromField(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType paramType) {
            return (Class<?>) paramType.getActualTypeArguments()[0];
        }
        return Object.class;
    }

    public Object deleteEntity(Class<?> entityClass, Object entity) {
        String tableName = entityClass.getSimpleName().toLowerCase();
        String idColumn = getIdColumnName(entityClass);
        Object id = getEntityId(entity);
        String sql = "DELETE FROM " + databasePrefix + tableName + " WHERE " + idColumn + " = ?";
        try {
            sqlExecutor.executeUpdate(sql, id);
        } catch (SQLException e) {
            throw new RuntimeException("Error executing deleteEntity SQL", e);
        }
        elementCollectionHandler.persistCollections(entityClass, entity, false);
        return entity;
    }
}
