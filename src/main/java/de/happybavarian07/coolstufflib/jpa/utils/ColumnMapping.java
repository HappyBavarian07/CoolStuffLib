package de.happybavarian07.coolstufflib.jpa.utils;

import java.lang.reflect.Field;

/**
 * Represents the mapping between a Java class field and a database column.
 */
public record ColumnMapping(Field field, String columnName) {}
