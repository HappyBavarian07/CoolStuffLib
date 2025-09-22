package de.happybavarian07.coolstufflib.jpa.utils;

import de.happybavarian07.coolstufflib.jpa.SQLExecutor;
import de.happybavarian07.coolstufflib.jpa.annotations.ElementCollection;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Collection;

class ElementCollectionHandler {
    private final SQLExecutor sqlExecutor;
    private final String databasePrefix;

    ElementCollectionHandler(SQLExecutor sqlExecutor, String databasePrefix) {
        this.sqlExecutor = sqlExecutor;
        this.databasePrefix = databasePrefix;
    }

    void persistCollections(Class<?> entityClass, Object entity, boolean isInsert) {
        Object entityId = EntityReflectionUtil.getEntityId(entity);
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(ElementCollection.class)) {
                field.setAccessible(true);
                ElementCollection ec = field.getAnnotation(ElementCollection.class);
                if (ec == null) {
                    continue;
                }
                String collectionTable = ec.tableName().isEmpty() ?
                        EntityReflectionUtil.getTableName(entityClass) + "_" + field.getName() : ec.tableName();
                String fkColumn = EntityReflectionUtil.getIdColumnName(entityClass);
                String valueColumn = ec.columnName().isEmpty() ? "element" : ec.columnName();
                Collection<?> collection = null;
                try {
                    Object val = field.get(entity);
                    if (val instanceof Collection<?>) {
                        collection = (Collection<?>) val;
                    }
                } catch (IllegalAccessException ignored) {}
                if (!isInsert) {
                    String delSql = "DELETE FROM " + databasePrefix + collectionTable + " WHERE " + fkColumn + " = ?";
                    try {
                        sqlExecutor.executeUpdate(delSql, entityId);
                    } catch (SQLException e) {
                        throw new RuntimeException("Error deleting old @ElementCollection rows. SQL: " + delSql, e);
                    }
                }
                if (collection != null && !collection.isEmpty()) {
                    for (Object element : collection) {
                        String insSql = "INSERT INTO " + databasePrefix + collectionTable + " (" + fkColumn + ", " + valueColumn + ") VALUES (?, ?)";
                        try {
                            sqlExecutor.executeUpdate(insSql, entityId, element);
                        } catch (SQLException e) {
                            throw new RuntimeException("Error inserting @ElementCollection row. SQL: " + insSql, e);
                        }
                    }
                }
            }
        }
    }

    void loadCollections(Object entity, Class<?> entityClass) {
        Object entityId = EntityReflectionUtil.getEntityId(entity);
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(ElementCollection.class)) {
                field.setAccessible(true);
                ElementCollection ec = field.getAnnotation(ElementCollection.class);
                if (ec == null) {
                    continue;
                }
                String collectionTable = ec.tableName().isEmpty() ?
                        EntityReflectionUtil.getTableName(entityClass) + "_" + field.getName() : ec.tableName();
                String fkColumn = EntityReflectionUtil.getIdColumnName(entityClass);
                String valueColumn = ec.columnName().isEmpty() ? "element" : ec.columnName();
                Collection<Object> collection;
                if (java.util.Set.class.isAssignableFrom(field.getType())) {
                    collection = new java.util.HashSet<>();
                } else {
                    collection = new java.util.ArrayList<>();
                }
                String sql = "SELECT " + valueColumn + " FROM " + databasePrefix + collectionTable + " WHERE " + fkColumn + " = ?";
                try (java.sql.ResultSet rs = sqlExecutor.executeQuery(sql, entityId)) {
                    while (rs.next()) {
                        Object val = rs.getObject(valueColumn);
                        collection.add(FieldTypeCaster.castToFieldType(EntityReflectionUtil.getGenericTypeFromField(field), val));
                    }
                } catch (java.sql.SQLException e) {
                    throw new RuntimeException("Error loading @ElementCollection. SQL: " + sql, e);
                }
                try {
                    field.set(entity, collection);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Error setting @ElementCollection field value", e);
                }
            }
        }
    }

    void createCollectionTables(Class<?> entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(ElementCollection.class)) {
                String entityTable = EntityReflectionUtil.getTableName(entityClass);
                try {
                    sqlExecutor.createElementCollectionTable(entityClass, field, entityTable, "");
                } catch (SQLException e) {
                    throw new RuntimeException("Error creating join table for @ElementCollection", e);
                }
            }
        }
    }
}
