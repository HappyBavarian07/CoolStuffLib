package de.happybavarian07.coolstufflib.jpa.utils;

import de.happybavarian07.coolstufflib.jpa.annotations.Column;
import de.happybavarian07.coolstufflib.jpa.annotations.Id;
import de.happybavarian07.coolstufflib.jpa.annotations.Table;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class EntityReflectionUtil {
    private static final java.util.concurrent.ConcurrentHashMap<Class<?>, EntityMetadata> METADATA_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private EntityReflectionUtil() {}

    /**
     * <p>Gets cached metadata for an entity class.</p>
     *
     * @param entityClass The entity class
     * @return The entity metadata
     */
    public static EntityMetadata getMetadata(Class<?> entityClass) {
        return METADATA_CACHE.computeIfAbsent(entityClass, clazz -> {
            String tableName = getTableNameInternal(clazz);
            String idColumnName = null;
            Field idField = null;
            List<ColumnMapping> mappings = new ArrayList<>();

            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Id.class)) {
                    idField = field;
                    Column col = field.getAnnotation(Column.class);
                    idColumnName = (col != null && col.name() != null && !col.name().isEmpty()) ? col.name() : field.getName();
                }

                if (field.isAnnotationPresent(Column.class)) {
                    Column col = field.getAnnotation(Column.class);
                    String colName = (col != null && col.name() != null && !col.name().isEmpty()) ? col.name() : field.getName();
                    mappings.add(new ColumnMapping(field, colName));
                } else if (field.isAnnotationPresent(Id.class)) {
                    mappings.add(new ColumnMapping(field, idColumnName));
                }
            }

            if (idField == null) {
                throw new IllegalStateException("No @Id field found in entity class: " + clazz.getName());
            }

            return new EntityMetadata(tableName, idColumnName, idField, java.util.Collections.unmodifiableList(mappings));
        });
    }

    /**
     * <p>Gets the ID column name for an entity class.</p>
     *
     * @param entityClass The entity class
     * @return The ID column name
     */
    public static String getIdColumnName(Class<?> entityClass) {
        return getMetadata(entityClass).idColumnName();
    }

    /**
     * <p>Gets the unique ID of an entity instance.</p>
     *
     * @param entity The entity instance
     * @return The entity ID
     */
    public static Object getEntityId(Object entity) {
        EntityMetadata metadata = getMetadata(entity.getClass());
        try {
            return metadata.idField().get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> getGenericTypeFromField(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType paramType) {
            return (Class<?>) paramType.getActualTypeArguments()[0];
        }
        return Object.class;
    }

    public static String getTableName(Class<?> entityClass) {
        return getMetadata(entityClass).tableName();
    }

    private static String getTableNameInternal(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(Table.class)) {
            String n = entityClass.getAnnotation(Table.class).name();
            if (n != null && !n.isEmpty()) return n;
        }
        return entityClass.getSimpleName().toLowerCase();
    }
}
