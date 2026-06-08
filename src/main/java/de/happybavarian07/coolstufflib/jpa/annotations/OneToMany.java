package de.happybavarian07.coolstufflib.jpa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Maps a one-to-many relationship between two entity classes.</p>
 *
 * <p>This annotation is used on the "one" side of the relationship, where a single entity
 * has a collection (List, Set, Map values) of related entities. It establishes that one
 * instance of the owning entity can be associated with many instances of the target entity.</p>
 *
 * <h3>Key Concepts:</h3>
 * <ul>
 *   <li><strong>Bidirectional relationships</strong>: Requires {@link #mappedBy()} to reference the corresponding
 *       {@link ManyToOne} field on the target entity</li>
 *   <li><strong>Unidirectional relationships</strong>: Uses a join table with {@link JoinColumn}s when mappedBy is not set</li>
 *   <li><strong>Collection types</strong>: Supports List, Set, Map (values only), and other Collection types</li>
 * </ul>
 *
 * <pre>{@code
 * // Bidirectional one-to-many relationship
 * @Entity(name = "Player")
 * public class Player {
 *     @Id
 *     private Long id;
 *     
 *     // One player has many inventory items
 *     @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
 *     private List<InventoryItem> inventoryItems;
 * }
 * 
 * @Entity(name = "InventoryItem")
 * public class InventoryItem {
 *     @Id
 *     private Long id;
 *     
 *     // Each item belongs to one player (many-to-one)
 *     @ManyToOne(fetch = FetchType.LAZY)
 *     @JoinColumn(name = "player_id")
 *     private Player player;
 * }
 * }</pre>
 *
 * <pre>{@code
 * // Unidirectional one-to-many relationship (uses join table)
 * @Entity(name = "Department")
 * public class Department {
 *     @Id
 *     private Long id;
 *     
 *     @OneToMany(targetEntity = Employee.class)
 *     @JoinTable(
 *         name = "department_employees",
 *         joinColumns = @JoinColumn(name = "dept_id"),
 *         inverseJoinColumns = @JoinColumn(name = "emp_id")
 *     )
 *     private List<Employee> employees;
 * }
 * }</pre>
 *
 * <h3>Cascade Operations:</h3>
 * <p>SPECIFY cascade types via {@link #cascade()} to propagate operations from the parent
 * entity to related child entities. Common use case: removing a Player automatically removes
 * all their inventory items with {@code orphanRemoval = true}.</p>
 *
 * @param mappedBy The name of the ManyToOne field in the target entity that forms the
 *                 inverse side of the relationship. Required for bidirectional relationships.
 *                 Empty string indicates unidirectional (uses join table).
 * 
 * @param targetEntity The class type of entities in the collection. Required when the
 *                     collection's generic type cannot be determined from the field declaration.
 *                     Default is void.class (inferred from field type if possible).
 *
 * @param cascade Cascade operations to propagate to related entities. See {@link CascadeType}
 *                for available options: PERSIST, MERGE, REMOVE, REFRESH, DETACH, ALL.
 *                Default is empty array (no cascading).
 *
 * @param fetch The fetch type for loading related data. {@link FetchType#EAGER} loads
 *              the collection immediately when accessing the entity. {@link FetchType#LAZY}
 *              defers loading until the collection is actually accessed. Default is LAZY.
 *
 * @param orphanRemoval When true, entities that were previously associated but are no
 *                      longer in the collection will be automatically deleted on flush. Useful
 *                      for managing child entity lifecycle (e.g., removing items from inventory).
 *                      Default is false. Only applies when mappedBy is set.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OneToMany {
    /**
     * <p>The field name in the target entity that holds the inverse ManyToOne relationship.</p>
     *
     * <p>Required for bidirectional one-to-many relationships. The value must match the
     * exact field name (not column name) of the ManyToOne annotation on the target entity.</p>
     *
     * <pre>{@code
     * // In Player class - mappedBy references "inventoryItem.player" field
     * @OneToMany(mappedBy = "player")
     * private List<InventoryItem> inventory;
     * 
     * // In InventoryItem class - this is the inverse side
     * @ManyToOne
     * @JoinColumn(name = "player_id")
     * private Player player;  // <- field name is "player"
     * }</pre>
     *
     * <p>If empty (default), a unidirectional relationship is assumed and a join table
     * must be specified via {@link JoinTable} annotation.</p>
     *
     * @return The field name of the inverse side relationship
     */
    String mappedBy() default "";

    /**
     * <p>The target entity class for this one-to-many relationship.</p>
     *
     * <p>Specify when the collection's generic type cannot be inferred. This is typically
     * needed when using raw collection types without generics, or in certain edge cases.</p>
     *
     * <pre>{@code
     * // Generic type can be inferred - no need for targetEntity
     * @OneToMany(mappedBy = "category")
     * private List<Product> products;
     *
     * // Raw collection without generics - must specify targetEntity
     * @OneToMany(targetEntity = Product.class)
     * private Collection products;
     * }</pre>
     *
     * @return The Class object representing the target entity type,
     *         or void.class if inference should be used (default)
     */
    Class<?> targetEntity() default void.class;

    /**
     * <p>Specifies which operations cascade from the owning entity to related entities.</p>
     *
     * <p>Cascade types control how persistence operations on the parent entity propagate
     * to child entities in the collection:</p>
     *
     * <ul>
     *   <li><strong>PERSIST</strong>: Saving the parent also persists new child entities</li>
     *   <li><strong>MERGE</strong>: Merging the parent also merges detached child entities</li>
     *   <li><strong>REMOVE</strong>: Deleting the parent also deletes all child entities</li>
     *   <li><strong>REFRESH</strong>: Refreshing the parent also refreshes children</li>
     *   <li><strong>DETACH</strong>: Detaching the parent also detaches children</li>
     *   <li><strong>ALL</strong>: All cascade types are enabled</li>
     * </ul>
     *
     * <pre>{@code
     * // Cascade ALL - any operation on player propagates to inventory items
     * @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
     * private List<InventoryItem> inventory;
     *
     * // Only cascade PERSIST and MERGE
     * @OneToMany(mappedBy = "owner", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
     * private List<Pet> pets;
     * }</pre>
     *
     * <p><strong>Note:</strong> {@link #orphanRemoval()} provides a specialized form of
     * REMOVE cascading that only deletes entities removed from the collection.</p>
     *
     * @return Array of cascade types to enable. Default is empty array (no cascading).
     */
    CascadeType[] cascade() default {};

    /**
     * <p>Specifies when related data should be loaded from the database.</p>
     *
     * <p><strong>{@link FetchType#EAGER}</strong>: Related entities are loaded automatically
     * when the owning entity is accessed. The collection is initialized immediately.</p>
     *
     * <ul>
     *   <li>Pros: Data available immediately, no N+1 query issues for this relationship</li>
     *   <li>Cons: Always loads potentially large collections, even if not needed</li>
     * </ul>
     *
     * <p><strong>{@link FetchType#LAZY}</strong>: Related entities are loaded only when
     * the collection is first accessed. Default behavior for OneToMany.</p>
     *
     * <ul>
     *   <li>Pros: Better performance when collection is large or rarely accessed</li>
     *   <li>Cons: May cause LazyInitializationException if accessed outside transaction</li>
     * </ul>
     *
     * <pre>{@code
     * // EAGER - loads all orders immediately with Customer entity
     * @OneToMany(fetch = FetchType.EAGER)
     * private List<Order> orders;
     *
     * // LAZY (default) - orders loaded only when customer.getOrders() is called
     * @OneToMany(mappedBy = "customer")
     * private List<Order> orders;
     * }</pre>
     *
     * <p><strong>Recommendation:</strong> Use LAZY for one-to-many relationships as the default,
     * since loading large collections eagerly can significantly impact performance.</p>
     *
     * @return The fetch type strategy. Default is LAZY.
     */
    FetchType fetch() default FetchType.LAZY;

    /**
     * <p>Controls automatic deletion of orphaned entities removed from the collection.</p>
     *
     * <p>When set to {@code true}, any entity that was previously in the collection but is
     * subsequently removed will be automatically deleted from the database when the session
     * is flushed. This provides convenient lifecycle management for child entities.</p>
     *
     * <pre>{@code
     * @Entity(name = "ShoppingCart")
     * public class ShoppingCart {
     *     private Long id;
     *     
     *     // Removing an item from this list will delete it from database
     *     @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
     *     private List<CartItem> items;
     *
     *     public void removeItem(CartItem item) {
     *         this.items.remove(item);
     *         // Item will be automatically deleted on next flush
     *     }
     * }
     * 
     * // Usage:
     * cart.removeItem(cart.getItems().get(0));
     * entityManager.flush();  // Removed item is now deleted from database
     * }</pre>
     *
     * <h3>Key Considerations:</h3>
     * <ul>
     *   <li>Only effective when {@link #mappedBy()} is set (bidirectional relationship)</li>
     *   <li>Should typically be used with cascade=ALL or at least cascade=PERSIST</li>
     *   <li>Set {@code detach=false} on the inverse ManyToOne to prevent detached entities</li>
     * </ul>
     *
     * @return True to enable automatic deletion of orphaned entities, false otherwise.
     *         Default is false.
     */
    boolean orphanRemoval() default false;
}
