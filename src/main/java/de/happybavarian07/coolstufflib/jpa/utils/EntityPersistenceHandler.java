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
        EntityMetadata metadata = EntityReflectionUtil.getMetadata(entityClass);
        String tableName = metadata.tableName();
        Field idField = metadata.idField();
        boolean generateId = false;
        try {
            generateId = SQLExecutor.isGenerated(idField) && isUnset(idField.get(entity));
        } catch (IllegalAccessException ignored) {}
        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (ColumnMapping mapping : metadata.columnMappings()) {
            if (mapping.field().isAnnotationPresent(ElementCollection.class)) continue;
            if (generateId && mapping.field() == idField) continue;
            columns.add(mapping.columnName());
            try {
                values.add(mapping.field().get(entity));
            } catch (IllegalAccessException ignored) {}
        }
        String sql = "INSERT INTO " + databasePrefix + tableName + " (" +
                String.join(", ", columns) + ") VALUES (" +
                String.join(", ", Collections.nCopies(columns.size(), "?")) + ")";
        try {
            if (generateId) {
                Object key = sqlExecutor.executeInsert(sql, values.toArray());
                if (key != null) idField.set(entity, FieldTypeCaster.castToFieldType(idField.getType(), key));
            } else {
                sqlExecutor.executeUpdate(sql, values.toArray());
            }
        } catch (SQLException | IllegalAccessException e) {
            throw new RuntimeException("Error executing insertEntity SQL: " + sql, e);
        }
        elementCollectionHandler.persistCollections(entityClass, entity, true);
        return entity;
    }

    /** A generated id that has not been assigned yet: null, or 0 for number types. */
    private static boolean isUnset(Object id) {
        return id == null || (id instanceof Number number && number.longValue() == 0);
    }

    Object updateEntity(Class<?> entityClass, Object entity) {
        EntityMetadata metadata = EntityReflectionUtil.getMetadata(entityClass);
        String tableName = metadata.tableName();
        String idColumn = metadata.idColumnName();
        Object id = EntityReflectionUtil.getEntityId(entity);
        List<String> setClauses = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (ColumnMapping mapping : metadata.columnMappings()) {
            if (mapping.field() != metadata.idField() && !mapping.field().isAnnotationPresent(ElementCollection.class)) {
                setClauses.add(mapping.columnName() + " = ?");
                try {
                    values.add(mapping.field().get(entity));
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
