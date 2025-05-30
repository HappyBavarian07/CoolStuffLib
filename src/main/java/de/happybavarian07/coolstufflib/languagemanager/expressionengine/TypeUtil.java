package de.happybavarian07.coolstufflib.languagemanager.expressionengine;

public class TypeUtil {
    public static Object convert(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType == null || targetType.isInstance(value)) return value;
        String str = value.toString();
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(str);
        } else if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(str);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(str);
        } else if (targetType == String.class) {
            return str;
        }
        throw new IllegalArgumentException("Cannot convert " + value + " to " + targetType.getSimpleName());
    }
}
