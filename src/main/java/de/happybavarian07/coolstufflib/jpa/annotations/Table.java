package de.happybavarian07.coolstufflib.jpa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Specifies the database table for an entity class.</p>
 *
 * <p>The {@code @Table} annotation maps an entity to a specific database table,
 * allowing customization of the table name and schema. It is typically used together
 * with {@link Entity} on entity classes.</p>
 *
 * <pre><code>
 * @Entity
 * @Table(name = "player_stats", schema = "game_data")
 * public class PlayerStats {
 *     @Id
 *     private Long playerId;
 *     
 *     private int score;
 *     private int rank;
 * }
 * </code></pre>
 *
 * <p>If {@code @Table} is not specified, the entity name (or class simple name) is
 * used as the default table name in the public/default schema.</p>
 *
 * @see Entity
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Table {
    /**
     * Gets the database table name.
     *
     * <pre><code>
 * // Custom table name different from class name
 * @Entity(name = "UserData")
 * @Table(name = "user_profiles")
 * public class UserProfile { ... }
 * </code></pre>
     *
     * <p>If empty or not specified, defaults to the entity name or class simple name.</p>
     *
     * @return the database table name; defaults to entity/class name if empty
     */
    String name();

    /**
     * Gets the database schema containing this table.
     *
     * <pre><code>
 * // Table in a specific schema
 * @Table(name = "players", schema = "game_db")
 * public class Player { ... }
 * </code></pre>
     *
     * <p>Useful for organizing entities into logical groups or when using multiple
 * schemas/databases. Empty string means the default schema.</p>
     *
     * @return the database schema; empty string ("") means default schema
     */
    String schema() default "";
}
