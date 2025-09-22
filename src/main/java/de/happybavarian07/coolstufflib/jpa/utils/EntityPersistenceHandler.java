package de.happybavarian07.coolstufflib.jpa.utils;

import de.happybavarian07.coolstufflib.jpa.SQLExecutor;
import de.happybavarian07.coolstufflib.jpa.annotations.Column;
import de.happybavarian07.coolstufflib.jpa.annotations.ElementCollection;
import de.happybavarian07.coolstufflib.jpa.annotations.Id;

import java.lang.reflect.Field;
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
        String tableName = EntityReflectionUtil.getTableName(entityClass);
        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class) && !field.isAnnotationPresent(ElementCollection.class)) {
                field.setAccessible(true);
                String colName = field.getAnnotation(Column.class).name();
                if (colName == null || colName.isEmpty()) colName = field.getName();
                columns.add(colName);
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
            throw new RuntimeException("Error executing insertEntity SQL: " + sql, e);
        }
        elementCollectionHandler.persistCollections(entityClass, entity, true);
        return entity;
    }

    Object updateEntity(Class<?> entityClass, Object entity) {
        String tableName = EntityReflectionUtil.getTableName(entityClass);
        String idColumn = EntityReflectionUtil.getIdColumnName(entityClass);
        Object id = EntityReflectionUtil.getEntityId(entity);
        List<String> setClauses = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class) &&
                    !field.isAnnotationPresent(Id.class) && !field.isAnnotationPresent(ElementCollection.class)) {
                field.setAccessible(true);
                String colName = field.getAnnotation(Column.class).name();
                if (colName == null || colName.isEmpty()) colName = field.getName();
                setClauses.add(colName + " = ?");
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
            throw new RuntimeException("Error executing updateEntity SQL: " + sql, e);
        }
        elementCollectionHandler.persistCollections(entityClass, entity, false);
        return entity;
    }

    public Object deleteEntity(Class<?> entityClass, Object entity) {
        String tableName = EntityReflectionUtil.getTableName(entityClass);
        String idColumn = EntityReflectionUtil.getIdColumnName(entityClass);
        Object id = EntityReflectionUtil.getEntityId(entity);
        String sql = "DELETE FROM " + databasePrefix + tableName + " WHERE " + idColumn + " = ?";
        try {
            sqlExecutor.executeUpdate(sql, id);
        } catch (SQLException e) {
            throw new RuntimeException("Error executing deleteEntity SQL: " + sql, e);
        }
        elementCollectionHandler.persistCollections(entityClass, entity, false);
        return entity;
    }
}
