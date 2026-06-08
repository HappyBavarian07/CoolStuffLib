package de.happybavarian07.coolstufflib.jpa.annotations;

import de.happybavarian07.coolstufflib.jpa.interfaces.ResultSetValueConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Maps an entity field to a database column with optional configuration.</p>
 *
 * <p>The {@code @Column} annotation specifies how a field should be persisted to the database,
 * including the column name, nullability constraints, uniqueness, and type conversion settings.</p>
 *
 * <pre><code>
 * public class PlayerData {
 *     @Id
 *     private Long id;
 *     
 *     @Column(name = "player_name", length = 100, nullable = false)
 *     private String name;
 *     
 *     @Column(name = "is_online")
 *     private boolean online;
 *     
 *     @Column(converter = CustomDateTimeConverter.class)
 *     private LocalDateTime lastLogin;
 * }
 * </code></pre>
 *
 * <p>Common use cases:</p>
 * <ul>
 *   <li><b>Custom column names:</b> Map Java field names to different database column names</li>
 *   <li><b>Constraints:</b> Define NOT NULL, UNIQUE constraints at the mapping level</li>
 *   <li><b>Type conversion:</b> Specify custom converters for complex types</li>
 *   <li><b>Auto-increment:</b> Mark integer fields as auto-incrementing identifiers</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {
    /**
     * Gets the database column name.
     *
     * <pre><code>
 * // Java field uses camelCase, database uses snake_case
 * @Column(name = "player_name")
 * private String playerName;
 * </code></pre>
     *
     * <p>If not specified or empty, defaults to the field name.</p>
     *
     * @return the database column name; defaults to field name if empty
     */
    String name();

    /**
     * Gets whether NULL values are allowed in this column.
     *
     * <pre><code>
 * // Required field - cannot be null
 * @Column(nullable = false)
 * private String email;
 * </code></pre>
     *
     * <p>Setting to {@code false} indicates the column should have a NOT NULL constraint.</p>
     *
     * @return {@code true} if NULL is allowed (default), {@code false} if NOT NULL
     */
    boolean nullable() default true;

    /**
     * Gets whether this column has a UNIQUE constraint.
     *
     * <pre><code>
 * // Username must be unique across all records
 * @Column(unique = true)
 * private String username;
 * </code></pre>
     *
     * <p>Useful for fields like usernames, email addresses, or other identifiers</p>
     *
     * @return {@code true} if column should be unique, {@code false} otherwise (default)
     */
    boolean unique() default false;

    /**
     * Gets whether this column is an auto-incrementing identifier.
     *
     * <pre><code>
 * // Auto-increment primary key (alternative to @GeneratedValue)
 * @Id
 * @Column(autoIncrement = true)
 * private Long id;
 * </code></pre>
     *
     * <p>Typically used with integer types for simple auto-increment scenarios.</p>
     *
     * @return {@code true} if column is auto-incrementing, {@code false} otherwise
     */
    boolean autoIncrement() default false;

    /**
     * Gets whether this column is part of the primary key.
     *
     * <pre><code>
 * // Composite primary key using @Column instead of @Id
 * public class OrderItem {
 *     @Column(primaryKey = true)
 *     private Long orderId;
 *     
 *     @Column(primaryKey = true)
 *     private String sku;
 * }
 * </code></pre>
     *
     * <p>Use when defining composite primary keys without {@link Id} annotation.</p>
     *
     * @return {@code true} if column is part of primary key, {@code false} otherwise
     */
    boolean primaryKey() default false;

    /**
     * Gets the converter class for custom type transformation.
     *
     * <pre><code>
 * // Custom conversion for complex types
 * @Column(converter = JsonStringConverter.class)
 * private Configuration settings;
 * </code></pre>
     *
     * <p>The converter must implement {@link ResultSetValueConverter} and handles
     * transformation between Java types and database column values.</p>
     *
     * @return the converter class; defaults to {@link ResultSetValueConverter} (no conversion)
     */
    Class<ResultSetValueConverter> converter() default ResultSetValueConverter.class;
}
