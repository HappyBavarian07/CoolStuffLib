package de.happybavarian07.coolstufflib.jpa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Maps a many-to-one relationship between two entity classes.</p>
 *
 * <p>This annotation is used on the "many" side of the relationship, where multiple entities
 * reference a single related entity. It establishes that many instances of the owning entity
 * are associated with one instance of the target entity.</p>
 *
 * <h3>Key Concepts:</h3>
 * <ul>
 *   <li><strong>Owning side</strong>: The ManyToOne side owns the relationship in the database
 *       (holds the foreign key column)</li>
 *   <li><strong>Join column</strong>: By default uses {@code target_table_id} as foreign key column name</li>
 *   <li><strong>Bidirectional relationships</strong>: Pairs with {@link OneToMany} on the inverse side</li>
 * </ul>
 *
 * <pre>{@code
 * // Many-to-one relationship: many orders belong to one customer
 * @Entity(name = "Order")
 * public class Order {
 *     @Id
 *     private Long id;
 *     
 *     // This order belongs to exactly one customer
 *     @ManyToOne(fetch = FetchType.LAZY)
 *     @JoinColumn(name = "customer_id", referencedColumnName = "id")
 *     private Customer customer;
 * }
 * 
 * @Entity(name = "Customer")
 * public class Customer {
 *     @Id
 *     private Long id;
 *     
 *     // One customer can have many orders (inverse side)
 *     @OneToMany(mappedBy = "customer")
 *     private List<Order> orders;
 * }
 * }</pre>
 *
 * <h3>Cascade Operations:</h3>
 * <p>SPECIFY cascade types via {@link #cascade()} to propagate operations from the owning
 * entity (many side) to the target entity (one side). Note that cascading REMOVE from
 * many-to-one is less common - deleting a child shouldn't typically delete its parent.</p>
 *
 * @param targetEntity The class type of the related entity. Required when the field's generic
 *                     type cannot be determined from declaration. Default is void.class.
 *
 * @param cascade Cascade operations to propagate to the related (one) entity. See {@link CascadeType}
 *                for options: PERSIST, MERGE, REMOVE, REFRESH, DETACH, ALL.
 *                Default is empty array (no cascading).
 *
 * @param fetch The fetch type for loading the related entity. {@link FetchType#EAGER} loads
 *              the reference immediately when accessing the owning entity. {@link FetchType#LAZY}
 *              defers loading until the field is actually accessed. Default is EAGER.
 *
 * @param optional Indicates whether this relationship can be null (nullable foreign key).
 *                 When true, the database column allows NULL values and the reference
 *                 can legitimately be unset. Default is true.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ManyToOne {
    /**
     * <p>The target entity class for this many-to-one relationship.</p>
     *
     * <p>Specify when the field type cannot be inferred. This is typically needed
     * when using raw types without generics, or in certain edge cases.</p>
     *
     * <pre>{@code
     * // Generic type can be inferred - no need for targetEntity
     * @ManyToOne(fetch = FetchType.LAZY)
     * private Customer customer;
     *
     * // Raw type without generics - must specify targetEntity
     * @ManyToOne(targetEntity = Customer.class)
     * private Object customer;  // Not recommended but sometimes necessary
     * }</pre>
     *
     * <p>The target entity must be a valid JPA entity class annotated with {@link Entity}.</p>
     *
     * @return The Class object representing the target entity type,
     *         or void.class if inference should be used (default)
     */
    Class<?> targetEntity() default void.class;

    /**
     * <p>Specifies which operations cascade from this entity to the related entity.</p>
     *
     * <p>Cascade types control how persistence operations on the owning entity propagate
     * to the referenced target entity:</p>
     *
     * <ul>
     *   <li><strong>PERSIST</strong>: Saving the owning entity also persists a new target entity</li>
     *   <li><strong>MERGE</strong>: Merging the owning entity also merges a detached target entity</li>
     *   <li><strong>REMOVE</strong>: Deleting the owning entity also deletes the referenced entity
     *       (typically NOT used with many-to-one - opposite of cascade delete)</li>
     *   <li><strong>REFRESH</strong>: Refreshing the owning entity also refreshes the target</li>
     *   <li><strong>DETACH</strong>: Detaching the owning entity also detaches the target</li>
     *   <li><strong>ALL</strong>: All cascade types are enabled</li>
     * </ul>
     *
     * <pre>{@code
     * // Cascade PERSIST - if order has a new Customer, it gets saved automatically
     * @ManyToOne(cascade = CascadeType.PERSIST)
     * private Customer customer;
     *
     * // Rare: cascade REMOVE would delete the customer when deleting an order
     * // Generally not recommended for many-to-one relationships
     * }</pre>
     *
     * <p><strong>Note:</strong> Cascading REMOVE from ManyToOne is unusual - typically you want
     * CASCADE DELETE on the database foreign key, not cascade=REMOVE in JPA.</p>
     *
     * @return Array of cascade types to enable. Default is empty array (no cascading).
     */
    CascadeType[] cascade() default {};

    /**
     * <p>Specifies when the related entity should be loaded from the database.</p>
     *
     * <p><strong>{@link FetchType#EAGER}</strong>: The referenced entity is loaded automatically
     * when the owning entity is accessed. The foreign key relationship is joined immediately.</p>
     *
     * <ul>
     *   <li>Pros: Related entity available immediately, convenient for frequently accessed references</li>
     *   <li>Cons: Always performs JOIN even if related entity not needed</li>
     * </ul>
     *
     * <p><strong>{@link FetchType#LAZY}</strong>: The referenced entity is loaded only when
     * the field is first accessed. Default behavior for OneToMany, but EAGER default here.</p>
     *
     * <ul>
     *   <li>Pros: Better performance when relationship not always needed</li>
     *   <li>Cons: May cause LazyInitializationException if accessed outside transaction</li>
     * </ul>
     *
     * <pre>{@code
     * // EAGER (default for ManyToOne) - customer loaded with order
     * @ManyToOne
     * private Customer customer;
     *
     * Order order = repository.findById(123L);
     * System.out.println(order.getCustomer().getName());  // No extra query needed
     *
     * // LAZY - customer loaded only when accessed
     * @ManyToOne(fetch = FetchType.LAZY)
     * private Customer customer;
     *
     * Order order = repository.findById(123L);
     * System.out.println(order.getCustomer().getName());  // Triggers additional query
     * }</pre>
     *
     * <p><strong>Recommendation:</strong> Use EAGER for many-to-one when the relationship is
     * frequently accessed. Use LAZY when access is infrequent or the related entity is large.</p>
     *
     * @return The fetch type strategy. Default is EAGER.
     */
    FetchType fetch() default FetchType.EAGER;

    /**
     * <p>Indicates whether this relationship allows null values (nullable foreign key).</p>
     *
     * <p>When set to {@code true}, the corresponding foreign key column in the database
     * is created with NULL allowed, and the field can be legitimately null in Java.</p>
     *
     * <pre>{@code
     * // optional=true (default) - order can exist without being assigned to a customer
     * @ManyToOne(optional = true)
     * private Customer customer;
     * 
     * // Database: CREATE TABLE orders (... customer_id INT NULL ...)
     *
     * // optional=false - every order must be associated with a customer
     * @ManyToOne(optional = false)
     * private Customer customer;
     * 
     * // Database: CREATE TABLE orders (... customer_id INT NOT NULL ...)
     * }</pre>
     *
     * <h3>Common Use Cases:</h3>
     * <ul>
     *   <li><strong>{@code optional = true}</strong>: Optional relationships - e.g.,
     *       Order may not have an assigned customer yet, Comment without an Author</li>
     *   <li><strong>{@code optional = false}</strong>: Mandatory relationships - e.g.,
     *       Every Employee must belong to a Department, Every Product has a Category</li>
     * </ul>
     *
     * @return True if the relationship can be null (default), false if it must always
     *         reference a valid entity.
     */
    boolean optional() default true;
}
