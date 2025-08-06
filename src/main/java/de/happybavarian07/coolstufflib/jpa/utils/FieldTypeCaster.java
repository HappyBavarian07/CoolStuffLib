package de.happybavarian07.coolstufflib.jpa.utils;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

public class FieldTypeCaster {
    public static Object castToFieldType(Class<?> fieldType, Object value) {
        if (value == null) {
            if (fieldType.isPrimitive()) {
                if (fieldType == boolean.class) return false;
                if (fieldType == char.class) return '\u0000';
                if (fieldType == byte.class) return (byte) 0;
                if (fieldType == short.class) return (short) 0;
                if (fieldType == int.class) return 0;
                if (fieldType == long.class) return 0L;
                if (fieldType == float.class) return 0f;
                if (fieldType == double.class) return 0d;
            }
            return null;
        }
        if (fieldType.isAssignableFrom(value.getClass())) return value;
        if (fieldType.equals(String.class)) {
            return value.toString();
        }
        if (fieldType.equals(int.class) || fieldType.equals(Integer.class)) {
            if (value instanceof Number) return ((Number) value).intValue();
            if (value instanceof String && ((String) value).isEmpty()) return 0;
            return Integer.parseInt(value.toString());
        }
        if (fieldType.equals(long.class) || fieldType.equals(Long.class)) {
            if (value instanceof Number) return ((Number) value).longValue();
            if (value instanceof String && ((String) value).isEmpty()) return 0L;
            return Long.parseLong(value.toString());
        }
        if (fieldType.equals(float.class) || fieldType.equals(Float.class)) {
            if (value instanceof Number) return ((Number) value).floatValue();
            if (value instanceof String && ((String) value).isEmpty()) return 0f;
            return Float.parseFloat(value.toString());
        }
        if (fieldType.equals(double.class) || fieldType.equals(Double.class)) {
            if (value instanceof Number) return ((Number) value).doubleValue();
            if (value instanceof String && ((String) value).isEmpty()) return 0d;
            return Double.parseDouble(value.toString());
        }
        if (fieldType.equals(short.class) || fieldType.equals(Short.class)) {
            if (value instanceof Number) return ((Number) value).shortValue();
            if (value instanceof String && ((String) value).isEmpty()) return (short) 0;
            return Short.parseShort(value.toString());
        }
        if (fieldType.equals(byte.class) || fieldType.equals(Byte.class)) {
            if (value instanceof Number) return ((Number) value).byteValue();
            if (value instanceof String && ((String) value).isEmpty()) return (byte) 0;
            return Byte.parseByte(value.toString());
        }
        if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) {
            if (value instanceof Boolean) return value;
            if (value instanceof Number) return ((Number) value).intValue() != 0;
            String s = value.toString().toLowerCase();
            return s.equals("true") || s.equals("1") || s.equals("yes") || s.equals("y");
        }
        if (fieldType.equals(char.class) || fieldType.equals(Character.class)) {
            String s = value.toString();
            if (!s.isEmpty()) return s.charAt(0);
            return '\u0000';
        }
        if (fieldType.equals(UUID.class)) {
            if (value instanceof UUID) return value;
            if (value instanceof String) return UUID.fromString((String) value);
            if (value instanceof byte[]) return UUID.nameUUIDFromBytes((byte[]) value);
        }
        if (fieldType.equals(Date.class)) {
            if (value instanceof Date) return value;
            if (value instanceof Long) return new Date((Long) value);
        }
        if (fieldType.equals(Timestamp.class)) {
            if (value instanceof Timestamp) return value;
            if (value instanceof Date) return new Timestamp(((Date) value).getTime());
            if (value instanceof Long) return new Timestamp((Long) value);
        }
        return value;
    }
}
