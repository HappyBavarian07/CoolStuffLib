package de.happybavarian07.coolstufflib.jpa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Designates a class as a persistent entity in the JPA system.</p>
 *
 * <p>An entity represents a table in the database and maps object-oriented concepts
 * to relational database structures. Classes annotated with {@code @Entity} are managed
 * by the persistence context and can be stored, retrieved, updated, and deleted.</p>
 *
 * <pre><code>
 * @Entity(name = "PlayerData")
 * @Table(name = "players")
 * public class PlayerData {
 *     @Id
 *     @GeneratedValue(strategy = GenerationType.IDENTITY)
 *     private Long id;
 *     
 *     @Column(name = "player_name")
 *     private String name;
 *     
 *     @Column(name = "score")
 *     private int score;
 * }
 * </code></pre>
 *
 * <p>Entities can have relationships with other entities through annotations like
 * {@link OneToMany}, {@link ManyToOne}, {@link ManyToMany}, and {@link ElementCollection}.</p>
 *
 * @param name the entity name for repository references; defaults to class simple name if empty
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Entity {
    /**
     * Gets the entity name used for repository queries and references.
     *
     * <pre><code>
 * // Using custom entity name
 * @Entity(name = "CustomerData")
 * public class Customer { ... }
 *
 * // Repository can reference by entity name
 * var repo = entityManager.getRepository("CustomerData", Customer.class);
 * </code></pre>
     *
     * <p>If empty or not specified, defaults to the simple class name.</p>
     *
     * @return the entity name for repository references; defaults to "" (uses class name)
     */
    String name() default "";
}
