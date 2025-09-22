package de.happybavarian07.coolstufflib.jpa.utils;

import de.happybavarian07.coolstufflib.jpa.annotations.Column;
import de.happybavarian07.coolstufflib.jpa.annotations.Id;
import de.happybavarian07.coolstufflib.jpa.annotations.Table;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class EntityReflectionUtil {
    private EntityReflectionUtil() {}

    public static String getIdColumnName(Class<?> entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                Column col = field.getAnnotation(Column.class);
                if (col != null && col.name() != null && !col.name().isEmpty()) {
                    return col.name();
                }
                return field.getName();
            }
        }
        throw new IllegalStateException("No @Id field found in entity class: " + entityClass.getName());
    }

    public static Object getEntityId(Object entity) {
        for (Field field : entity.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                try {
                    return field.get(entity);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    public static Class<?> getGenericTypeFromField(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType paramType) {
            return (Class<?>) paramType.getActualTypeArguments()[0];
        }
        return Object.class;
    }

    public static String getTableName(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(Table.class)) {
            String n = entityClass.getAnnotation(Table.class).name();
            if (n != null && !n.isEmpty()) return n;
        }
        return entityClass.getSimpleName().toLowerCase();
    }
}
