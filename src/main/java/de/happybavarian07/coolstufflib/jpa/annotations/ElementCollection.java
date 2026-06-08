package de.happybavarian07.coolstufflib.jpa.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>Maps a collection of non-entity types or embeddable classes.</p>
 *
 * <p>This annotation is used when an entity has a collection field whose element type is
 * neither a JPA {@link Entity} nor a basic Java type (String, Integer, etc.). Common use
 * cases include collections of {@link Embeddable} objects or custom value types.</p>
 *
 * <h3>When to Use ElementCollection:</h3>
 * <ul>
 *   <li>Collection of embeddable/value classes - e.g., Address, Money, Range</li>
 *   <li>Collection of Java enum types</li>
 *   <li>Collection of basic types where column mapping is needed (use {@link Column} instead for simple cases)</li>
 * </ul>
 *
 * <h3>Key Characteristics:</h3>
 * <ul>
 *   <li><strong>No entity relationship</strong>: Unlike {@link OneToMany}, the collection elements
 *       are not separate entities with their own primary keys</li>
 *   <li><strong>Child table storage</strong>: Elements are stored in a separate table linked to the parent</li>
 *   <li><strong>No independent lifecycle</strong>: Collection elements cannot exist without their parent entity</li>
 * </ul>
 *
 * <pre>{@code
 * // Example: Entity with collection of embeddable address objects
 * 
 * @Embeddable
 * public class Address {
 *     private String street;
 *     private String city;
 *     private String postalCode;
 *     private String country;
 * }
 * 
 * @Entity(name = "Customer")
 * public class Customer {
 *     @Id
 *     private Long id;
 *     private String name;
 *     
 *     // Customer can have multiple addresses (shipping, billing, etc.)
 *     @ElementCollection(fetch = FetchType.LAZY)
 *     @CollectionTable(
 *         name = "customer_addresses",
 *         joinColumns = @JoinColumn(name = "customer_id")
 *     )
 *     private Collection<Address> addresses;
 * }
 * }</pre>
 *
 * <pre>{@code
 * // Example: Entity with collection of enum values
 * 
 * public enum OrderStatus {
 *     PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
 * }
 * 
 * @Entity(name = "Order")
 * public class Order {
 *     @Id
 *     private Long id;
 *     
 *     // Track all statuses this order has been in (audit trail)
 *     @ElementCollection
 *     @CollectionTable(name = "order_status_history")
 *     @Enumerated(EnumType.STRING)
 *     private List<OrderStatus> statusHistory;
 * }</pre>
 *
 * <h3>Table Structure:</h3>
 * <p>The element collection is stored in a child table with:</p>
 * <ul>
 *   <li>A foreign key column linking to the parent entity (configurable via {@link CollectionTable})</li>
 *   <li>Columns for each field of the embeddable or enum value</li>
 *   <li>No primary key on the collection element itself - only the combination with parent FK</li>
 * </ul>
 *
 * @param tableName The name of the table where collection elements are stored.
 *                  Default is auto-generated based on entity and field names
 *                  (e.g., "parententity_fieldname").
 * 
 * @param columnName When storing basic types directly (not embeddables), specifies
 *                   the column name for the collection values. Use {@link CollectionTable}
 *                   with {@link JoinColumn} for more control over mapping.
 * 
 * @param fetch The fetch type for loading collection data. {@link FetchType#EAGER}
 *              loads all collection elements immediately when accessing the parent entity.
 *              {@link FetchType#LAZY} defers loading until the collection is accessed.
 *              Default is EAGER.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ElementCollection {
    /**
     * <p>The name of the table where element collection data is stored.</p>
     *
     * <p>When specified, this overrides the default auto-generated table name. The
     * default naming convention combines the entity class name and field name:
     * {@code ENTITYNAME_FIELDNAME} (e.g., "customer_addresses").</p>
     *
     * <pre>{@code
     * // Default: table named "CUSTOMER_addRESSES" or similar
     * @ElementCollection
     * private Collection<Address> addresses;
     *
     * // Explicit: table named exactly "customer_shipping_addresses"
     * @ElementCollection(tableName = "customer_shipping_addresses")
     * private Collection<Address> shippingAddresses;
     * }</pre>
     *
     * <p>The table will contain:</p>
     * <ul>
     *   <li>A foreign key column to the parent entity (typically {@code parent_id})</li>
     *   <li>Columns for each field of the embedded type, or a single column for basic types</li>
     * </ul>
     *
     * @return The collection table name, or empty string for auto-generated name (default)
     */
    String tableName() default "";

    /**
     * <p>The column name for storing collection values when using basic types directly.</p>
     *
     * <p>This is used for simple collections of basic Java types where no embeddable
     * class is involved. For embeddables or more complex mappings, use {@link CollectionTable}
     * with {@link JoinColumn} annotations instead.</p>
     *
     * <pre>{@code
     * // Simple collection of tags - each tag stored in a single column
     * @ElementCollection(columnName = "tag_name")
     * private Set<String> tags;
     *
     * // Resulting table structure:
     * // Table: PRODUCT_tags (auto-generated)
     * // Columns: product_id (FK), tag_name (VARCHAR)
     *
     * // For embeddables, use CollectionTable instead:
     * @ElementCollection
     * @CollectionTable(name = "product_attributes",
     *                  joinColumns = @JoinColumn(name = "product_id"))
     * private Collection<ProductAttribute> attributes;  // ProductAttribute is @Embeddable
     * }</pre>
     *
     * @return The column name for collection values, or empty string for default (default)
     */
    String columnName() default "";

    /**
     * <p>Specifies when the collection should be loaded from the database.</p>
     *
     * <p><strong>{@link FetchType#EAGER}</strong>: The entire collection is loaded automatically
     * when the parent entity is accessed. All collection elements are fetched immediately.</p>
     *
     * <ul>
     *   <li>Pros: Collection available immediately, no N+1 query issues</li>
     *   <li>Cons: Always loads all collection elements even if only checking for existence
     *       or counting; can be expensive for large collections</li>
     * </ul>
     *
     * <p><strong>{@link FetchType#LAZY}</strong>: The collection is loaded only when first accessed.
     * A separate query fetches all elements from the child table on demand.</p>
     *
     * <ul>
     *   <li>Pros: Better performance when collection is rarely accessed or very large</li>
     *   <li>Cons: May cause LazyInitializationException if accessed outside transaction;
     *       additional query overhead on first access</li>
     * </ul>
     *
     * <pre>{@code
     * // EAGER (default for ElementCollection) - addresses loaded with Customer
     * @ElementCollection(fetch = FetchType.EAGER)
     * private Collection<Address> addresses;
     *
     * Customer customer = repository.findById(123L);
     * for (Address addr : customer.getAddresses()) {
     *     System.out.println(addr.getCity());  // No extra query - already loaded
     * }
     *
     * // LAZY - addresses loaded only when accessed
     * @ElementCollection(fetch = FetchType.LAZY)
     * private Collection<Address> addresses;
     *
     * Customer customer = repository.findById(123L);
     * for (Address addr : customer.getAddresses()) {
     *     System.out.println(addr.getCity());  // Triggers additional query
     * }
     * }</pre>
     *
     * <p><strong>Recommendation:</strong> Default is EAGER since element collections are often
     * small and frequently accessed. Use LAZY for potentially large collections.</p>
     *
     * @return The fetch type strategy. Default is EAGER.
     */
    FetchType fetch() default FetchType.EAGER;
}
