package de.happybavarian07.coolstufflib.jpa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Designates the primary key field of an entity.</p>
 *
 * <p>Every entity must have exactly one primary key field annotated with {@code @Id}.
 * The primary key uniquely identifies each record in the database table and is used for
 * entity lookup, updates, and deletes.</p>
 *
 * <pre><code>
 * public class PlayerData {
 *     @Id
 *     @GeneratedValue(strategy = GenerationType.IDENTITY)
 *     private Long id;
 *     
 *     private String playerName;
 * }
 * </code></pre>
 *
 * <p>Primary keys can be:</p>
 * <ul>
 *   <li><b>Simple types:</b> {@link Long}, {@link Integer}, {@link String}, {@link java.util.UUID}</li>
 *   <li><b>Generated:</b> Combined with {@link GeneratedValue} for auto-generation</li>
 *   <li><b>Composite:</b> Multiple fields using {@code @Embedded} with a value class</li>
 * </ul>
 *
 * <p>The primary key field should typically be final and have no setter method to maintain
 * entity identity integrity.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Id {
}
