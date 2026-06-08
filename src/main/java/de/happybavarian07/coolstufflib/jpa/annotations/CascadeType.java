package de.happybavarian07.coolstufflib.jpa.annotations;

/**
 * <p>Defines the types of persistence operations that can cascade from a parent entity to related entities.</p>
 *
 * <p>Cascade types control how lifecycle operations on an owning entity propagate through
 * relationships to associated target entities. This enables automatic management of related
 * entities without explicit individual operations.</p>
 *
 * <h3>When Cascade Applies:</h3>
 * <ul>
     * <li><strong>PERSIST cascade</strong>: Operations cascade when persisting the owning entity</li>
     * <li><strong>MERGE cascade</strong>: Operations cascade when merging (detached to managed)</li>
     * <li><strong>REMOVE cascade</strong>: Operations cascade when deleting the owning entity</li>
     * <li><strong>REFRESH cascade</strong>: Operations cascade when refreshing from database</li>
     * <li><strong>DETACH cascade</strong>: Operations cascade when detaching from persistence context</li>
     * </ul>
 *
 * Cascade types are specified on relationship annotations:
 * {@link OneToMany}, {@link ManyToOne}, {@link ManyToMany}, and {@link ElementCollection}.
 *
 * <pre>{@code
 * @Entity(name = "Order")
 * public class Order {
     *     @Id
     *     private Long id;
     *
     *     // Cascade ALL - all operations on Order propagate to OrderItems
     *     @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
     *     private List<OrderItem> items;
     * }
     *
     * @Entity(name = "OrderItem")
     * public class OrderItem {
         *     @Id
         *     private Long id;
         *     
         *     @ManyToOne
         *     @JoinColumn(name = "order_id")
         *     private Order order;  // Inverse side
         * }
     *
     * // Usage:
     * Order order = new Order();
     * OrderItem item1 = new OrderItem();
     * order.getItems().add(item1);
     *
     * entityManager.persist(order);  // Order AND item1 both persisted (PERSIST cascade)
     * }
     *</pre>
 */
public enum CascadeType {
    /**
     * <p>Enables all cascade operations: PERSIST, MERGE, REMOVE, REFRESH, and DETACH.</p>
     *
     * <p>This is a convenience constant equivalent to specifying all individual cascade types:</p>
     *
     * <pre>{@code
     * // These two declarations are equivalent:
     * @OneToMany(cascade = CascadeType.ALL)
     * private List<Child> children;
     *
     * @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE,
     *                       CascadeType.REMOVE, CascadeType.REFRESH,
     *                       CascadeType.DETACH})
     * private List<Child> children;
     * }</pre>
     *
     * <h3>Common Use Cases:</h3>
     * <ul>
         * <li><strong>Parent-child relationships</strong>: When child entities should have
     *       the same lifecycle as their parent (e.g., Order/OrderItem, ShoppingCart/CartItem)</li>
         * <li><strong>Orphan removal scenarios</strong>: Often used with {@code orphanRemoval = true}
     *       on OneToMany to fully manage child entity lifecycle</li>
         * </ul>
     *
     * <pre>{@code
     * @Entity(name = "ShoppingCart")
     * public class ShoppingCart {
         *     private Long id;
     *
     *     // Full lifecycle management - items tied to cart's existence
     *     @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
     *     private List<CartItem> items;
         * }
         *
         * // Adding new item - PERSIST cascade saves it with the cart
         * cart.getItems().add(newItem);
         *
         * // Removing item - REMOVE cascade + orphanRemoval deletes it from database
         * cart.getItems().remove(oldItem);
     * }</pre>
     *
     * <p><strong>Warning:</strong> Use ALL cautiously on ManyToMany relationships. Cascading
     * REMOVE could delete entities that are referenced by multiple parents.</p>
     */
    ALL,

    /**
     * <p>Cascades the PERSIST operation to related entities.</p>
     *
     * <p>When persisting the owning entity, any new (transient) target entities in the
     * relationship are also automatically persisted. This is useful for establishing
     * relationships with newly created child entities.</p>
     *
     * <pre>{@code
     * @Entity(name = "Department")
     * public class Department {
         *     private Long id;
     *     private String name;
     *
     *     // New employees added to department are auto-saved with the department
     *     @OneToMany(mappedBy = "department", cascade = CascadeType.PERSIST)
     *     private List<Employee> employees;
     * }
     *
     * // Usage:
     * Department dept = new Department();
     * dept.setName("Engineering");
     * 
     * Employee emp1 = new Employee();  // New/transient entity
     * emp1.setName("Alice");
     * dept.getEmployees().add(emp1);
     *
     * entityManager.persist(dept);  // Both Department AND Employee are persisted
     * }</pre>
     *
     * <h3>When to Use:</h3>
     * <ul>
         * <li><strong>New child creation</strong>: When you want to create parent and children
     *       in a single persist operation</li>
         * <li><strong>One-to-many relationships</strong>: Most common use case for PERSIST cascade</li>
         * </ul>
     *
     * <p><strong>Note:</strong> PERSIST cascade only applies to NEW (transient) entities.
     * Existing managed entities in the collection are not re-persisted.</p>
     */
    PERSIST,

    /**
     * <p>Cascades the MERGE operation to related entities.</p>
     *
     * <p>When merging a detached entity back into the persistence context, any detached
     * target entities in the relationship are also merged. This synchronizes the state
     * of related entities with the database.</p>
     *
     * <pre>{@code
     * @Entity(name = "Project")
     * public class Project {
         *     private Long id;
     *
     *     // Merging a detached project also merges its tasks
     *     @OneToMany(mappedBy = "project", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
     *     private List<Task> tasks;
     * }
     *
     * // Usage:
     * // 1. Load project with tasks
     * Project proj = projectRepository.findById(123L);
     *
     * // 2. Modify in detached state (e.g., different session/transaction)
     * Task newTask = new Task();
     * newTask.setName("New Feature");
     * proj.getTasks().add(newTask);
     *
     * // 3. Merge back - both project and new task are synchronized
     * Project mergedProj = entityManager.merge(proj);  // Project AND new task merged
     * }</pre>
     *
     * <h3>When to Use:</h3>
     * <ul>
         * <li><strong>Detached entity patterns</strong>: When entities may be modified outside
     *       the persistence context and need to be reattached</li>
         * <li><strong>Multi-tier applications</strong>: Common in web applications where entities
     *       are detached between request processing</li>
         * </ul>
     *
     * <p><strong>Note:</strong> MERGE cascade only applies to DETACHED entities. Managed or
     * persistent entities in the collection are not re-merged.</p>
     */
    MERGE,

    /**
     * <p>Cascades the REMOVE operation to related entities.</p>
     *
     * <p>When deleting the owning entity, all target entities in the relationship are also
     * deleted. This implements a "cascade delete" behavior where child entities cannot exist
     * without their parent.</p>
     *
     * <pre>{@code
     * @Entity(name = "BlogPost")
     * public class BlogPost {
         *     private Long id;
     *     private String title;
     *
     *     // Deleting a post also deletes all its comments
     *     @OneToMany(mappedBy = "post", cascade = CascadeType.REMOVE)
     *     private List<Comment> comments;
     * }
     *
     * // Usage:
     * BlogPost post = postRepository.findById(456L);
     * entityManager.remove(post);  // Post AND all comments are deleted
     *
     * // Alternatively, with orphanRemoval on OneToMany:
     * @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
     * private List<Comment> comments;
     *
     * post.getComments().remove(commentToRemove);  // Removed comment auto-deleted on flush
     * }</pre>
     *
     * <h3>When to Use:</h3>
     * <ul>
         * <li><strong>Strict parent-child relationships</strong>: When children cannot meaningfully
     *       exist independently of their parent (e.g., OrderItem without Order)</li>
         * <li><strong>Data cleanup</strong>: Automatically removing related data prevents orphaned records</li>
         * </ul>
     *
     * <p><strong>Warning:</strong> Use REMOVE cascade with caution, especially on ManyToMany
     * relationships. Deleting a parent could delete shared children that are referenced by
     * other parents as well.</p>
     *
     * <pre>{@code
     * // DANGEROUS: Student-Course many-to-many with REMOVE cascade
     * @Entity(name = "Student")
     * public class Student {
         *     @ManyToMany(cascade = CascadeType.REMOVE)  // DON'T DO THIS!
     *     private Set<Course> enrolledCourses;
     * }
     *
     * // Deleting student "Alice" would delete Course "Math 101"
     * // Even though students Bob and Carol are also enrolled in it!
     * }</pre>
     */
    REMOVE,

    /**
     * <p>Cascades the REFRESH operation to related entities.</p>
     *
     * <p>When refreshing an entity (reloading its state from the database), any target
     * entities in the relationship are also refreshed. This ensures all related data is
     * synchronized with the current database state.</p>
     *
     * <pre>{@code
     * @Entity(name = "Customer")
     * public class Customer {
         *     private Long id;
     *     
     *     // Refreshing customer also refreshes their address collection
     *     @ElementCollection(cascade = CascadeType.REFRESH)
     *     private Collection<Address> addresses;
     * }
     *
     * // Usage:
     * Customer cust = customerRepository.findById(789L);
     * 
     * // Another transaction modified the addresses in database
     * entityManager.refresh(cust);  // Customer AND addresses reloaded from database
     *
     * // Now cust.getAddresses() reflects current database state
     * }</pre>
     *
     * <h3>When to Use:</h3>
     * <ul>
         * <li><strong>State synchronization</strong>: When you need to reload entity state
     *       from database, including all related entities</li>
         * <li><strong>Concurrent modification scenarios</strong>: Resetting to database state
     *       when another process may have modified the data</li>
         * </ul>
     *
     * <p><strong>Note:</strong> REFRESH cascade is less commonly used than PERSIST, MERGE,
     * or REMOVE. It's primarily useful for specific synchronization scenarios.</p>
     */
    REFRESH,

    /**
     * <p>Cascades the DETACH operation to related entities.</p>
     *
     * <p>When detaching an entity from the persistence context, any target entities in the
     * relationship are also detached. This is useful when you need to work with entities
     * outside of a transaction or across session boundaries.</p>
     *
     * <pre>{@code
     * @Entity(name = "Order")
     * public class Order {
         *     private Long id;
     *
     *     // Detaching order also detaches all order items
     *     @OneToMany(mappedBy = "order", cascade = {CascadeType.PERSIST, CascadeType.DETACH})
     *     private List<OrderItem> items;
     * }
     *
     * // Usage:
     * Order order = orderRepository.findById(123L);
     *
     * // Detach for use outside transaction (e.g., pass to background task, cache)
     * entityManager.detach(order);  // Order AND all items are now detached
     *
     * // Can now modify detached order in non-transactional context
     * order.getItems().add(newItem);
     *
     * // Later: merge back into persistence context
     * Order mergedOrder = entityManager.merge(order);  // Everything reattached
     * }</pre>
     *
     * <h3>When to Use:</h3>
     * <ul>
         * <li><strong>Long-running processes</strong>: When entities need to be used outside
     *       the short-lived transaction scope</li>
         * <li><strong>Caching scenarios</strong>: Detaching entities for storage in application-level cache</li>
         * <li><strong>Distributed systems</strong>: Passing entity state between processes/nodes</li>
         * </ul>
     *
     * <p><strong>Note:</strong> DETACH cascade is often used together with MERGE cascade
     * to support round-trip detached entity patterns.</p>
     */
    DETACH
}
