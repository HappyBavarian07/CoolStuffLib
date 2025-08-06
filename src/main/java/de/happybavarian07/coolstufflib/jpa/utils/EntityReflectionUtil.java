package de.happybavarian07.coolstufflib.jpa.utils;

import de.happybavarian07.coolstufflib.jpa.annotations.Id;
import de.happybavarian07.coolstufflib.jpa.annotations.Table;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class EntityReflectionUtil {
    private EntityReflectionUtil() {}

    public static String getTableName(Class<?> entityClass) {
        Table table = entityClass.getAnnotation(Table.class);
        if (table != null && table.name() != null && !table.name().isEmpty()) {
            return table.name();
        }
        return entityClass.getSimpleName().toLowerCase();
    }

    public static String getIdColumnName(Class<?> entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
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
                    throw new RuntimeException("Unable to access @Id field", e);
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
}
