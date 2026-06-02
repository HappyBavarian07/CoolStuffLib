package de.happybavarian07.coolstufflib.jpa.utils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Stores cached reflection metadata for an entity class to avoid repeated lookups.
 */
public record EntityMetadata(
    String tableName,
    String idColumnName,
    Field idField,
    List<ColumnMapping> columnMappings
) {}
