package de.happybavarian07.coolstufflib.jpa.annotations;

/**
 * <p>Defines when related entities or collections should be loaded from the database.</p>
 *
 * <p>The fetch type determines whether relationship data is loaded automatically with the
     * owning entity (EAGER) or deferred until explicitly accessed (LAZY). This is a critical
     * performance consideration that affects query generation and memory usage.</p>
 *
 * <h3>Fetch Type Availability:</h3>
 * <ul>
     * <li><strong>{@link OneToMany}</strong>: Both EAGER and LAZY supported (default: LAZY)</li>
     * <li><strong>{@link ManyToOne}</strong>: Both EAGER and LAZY supported (default: EAGER)</li>
     * <li><strong>{@link ManyToMany}</strong>: Both EAGER and LAZY supported (default: LAZY)</li>
     * <li><strong>{@link ElementCollection}</strong>: Both EAGER and LAZY supported (default: EAGER)</li>
     * </ul>
 *
 * <h3>Strategy Comparison:</h3>
 *
 * | Aspect | EAGER | LAZY |
 * \|----------\|--------\|------|
     * | Loading time | With parent entity | On first access |
     * | Query pattern | JOIN or subquery | Additional SELECT |
     * | Memory usage | Higher (loads everything) | Lower (loads only what needed) |
     * | N+1 query risk | Yes for collections | Can be avoided with proper use |
     * | Default for OneToMany/ManyToMany | No | **Yes** |
     * | Default for ManyToOne | **Yes** | No |
 *
 * <pre>{@code
     * @Entity(name = "Order")
     * public class Order {
         *     @Id
         *     private Long id;
     *
     *     // EAGER - customer loaded with every order query
     *     @ManyToOne(fetch = FetchType.EAGER)
     *     private Customer customer;
     *
     *     // LAZY (default) - items loaded only when getOrderItems() called
         *     @OneToMany(mappedBy = "order")
         *     private List<OrderItem> orderItems;
     * }
     *
     * Order order = orderRepository.findById(123L);
     * System.out.println(order.getCustomer().getName());  // No extra query - EAGER
     *
     * for (OrderItem item : order.getOrderItems()) {     // Extra query triggered - LAZY
         *     System.out.println(item.getProduct().getName());
     * }
     * }</pre>
 */
public enum FetchType {
    /**
     * <p>Related data is loaded automatically when the owning entity is accessed.</p>
     *
     * <p>EAGER fetching means relationship data is retrieved immediately as part of
         * loading the parent entity. For single-entity relationships (ManyToOne), this
         * typically uses a JOIN in the same query. For collection relationships
         * (OneToMany, ManyToMany), it loads all related entities at once.</p>
     *
     * <pre>{@code
     * @Entity(name = "Comment")
     * public class Comment {
         *     private Long id;
     *     private String text;
     *
     *     // EAGER (default for ManyToOne) - author loaded with every comment
         *     @ManyToOne(fetch = FetchType.EAGER)
         *     private User author;
     * }
     *
     * // Query for comments automatically includes user data via JOIN:
     * // SELECT c.*, a.* FROM comments c 
         * // INNER JOIN users a ON c.author_id = a.id
     *
     * List<Comment> comments = commentRepository.findAll();
     * System.out.println(comments.get(0).getAuthor().getName());  // No extra query needed
     * }</pre>
     *
     * <h3>Pros of EAGER Fetching:</h3>
     * <ul>
         * <li><strong>Data available immediately</strong>: Related entities ready for use without additional queries</li>
         * <li><strong>No LazyInitializationException</strong>: Can safely access relationships outside original transaction</li>
         * <li><strong>Convenient for frequently accessed relationships</strong>: No need to manage eager loading separately</li>
         * </ul>
     *
     * <h3>Cons of EAGER Fetching:</h3>
     * <ul>
         * <li><strong>Always loads related data</strong>: Even when you only need the parent entity
         *       (wastes database and network resources)</li>
         * <li><strong>N+1 query problem for collections</strong>: Iterating over lazy collections
         *       triggers individual queries; eager fetch may load huge datasets unnecessarily</li>
         * <li><strong>Memory overhead</strong>: All related entities loaded into memory regardless of actual need</li>
         * </ul>
     *
     * <h3>When to Use EAGER:</h3>
     * <ul>
         * <li><strong>ManyToOne relationships</strong>: Single entity reference, minimal overhead
         *       (this is the default for ManyToOne for this reason)</li>
         * <li><strong>Frequently accessed small collections</strong>: When the collection is always
         *       needed and typically contains few elements</li>
         * <li><strong>Simple domain models</strong>: When relationships are always navigated together</li>
         * </ul>
     *
     * <pre>{@code
     * // Good use of EAGER - Order always needs its Customer reference
     * @Entity(name = "Order")
     * public class Order {
         *     @ManyToOne(fetch = FetchType.EAGER)  // Default, but explicit for clarity
         *     private Customer customer;  // Always need to know who placed the order
     * }
     *
     * // Bad use of EAGER - Large collection rarely fully traversed
     * @Entity(name = "User")
     * public class User {
         *     @OneToMany(fetch = FetchType.EAGER)  // Problematic!
         *     private List<Order> orders;  // User may have thousands of orders
         * }
     * }</pre>
     */
    EAGER,

    /**
     * <p>Related data is loaded only when explicitly accessed.</p>
     *
     * <p>LAZY fetching defers loading relationship data until the field or collection is
     * actually accessed. The first access triggers a separate database query to fetch
     * the related entities. This is achieved through proxy objects (for single entities)
     * or lazy collections that intercept access attempts.</p>
     *
     * <pre>{@code
     * @Entity(name = "Product")
     * public class Product {
         *     private Long id;
         *     private String name;
     *
     *     // LAZY (default for OneToMany) - reviews loaded only when accessed
     *     @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
     *     private List<Review> reviews;
     * }
     *
     * Product product = productRepository.findById(456L);  // Only product loaded
     *
     * System.out.println(product.getName());               // No extra query - just field access
     *
     * int reviewCount = product.getReviews().size();       // Extra SELECT query triggered here!
     * // Queries only the reviews for this specific product
     *
     * // List<Review> is now populated and cached in memory
     * System.out.println(product.getReviews().get(0).getText());  // No extra query - already loaded
     * }</pre>
     *
     * <h3>Pros of LAZY Fetching:</h3>
     * <ul>
         * <li><strong>Better performance</strong>: Only loads data that is actually needed</li>
         * <li><strong>Lower memory usage</strong>: Unaccessed relationships don't consume memory</li>
         * <li><strong>Faster initial load</strong>: Parent entity loaded quickly without related data</li>
         * </ul>
     *
     * <h3>Cons of LAZY Fetching:</h3>
     * <ul>
         * <li><strong>LazyInitializationException</strong>: Accessing lazy relationships outside
         *       an open persistence context throws an exception (common bug source)</li>
         * <li><strong>N+1 query problem</strong>: Iterating over collections without proper handling
     *       triggers one query per collection access</li>
         * <li><strong>Additional queries on first access</strong>: Performance hit when relationship is finally accessed</li>
         * </ul>
     *
     * <h3>When to Use LAZY:</h3>
     * <ul>
         * <li><strong>OneToMany relationships</strong>: Collections can be large; default behavior for this reason</li>
         * <li><strong>ManyToMany relationships</strong>: Join table queries can be expensive</li>
         * <li><strong>Infrequently accessed relationships</strong>: When the relationship is rarely navigated</li>
         * <li><strong>Large datasets</strong>: When loading all related data would impact performance</li>
         * </ul>
     *
     * <pre>{@code
     * // Good use of LAZY - User may have many orders, not always needed
     * @Entity(name = "User")
     * public class User {
         *     private Long id;
     *     private String name;
     *
     *     @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)  // Default - appropriate!
     *     private List<Order> orders;  // Only load user's orders when actually needed
     * }
     *
     * // Displaying user list - only users loaded, not their orders
     * List<User> users = userRepository.findAll();
     * for (User user : users) {
         *     System.out.println(user.getName());  // No order queries triggered
     * }
     * }</pre>
     *
     * <h3>Avoiding LazyInitializationException:</h3>
     * <p>To safely access lazy relationships outside the original transaction, use fetch joins:
     *</p>
     *
     * <pre>{@code
     * // JPQL with JOIN FETCH - eagerly load specific relationship in query
     * @Query("SELECT u FROM User u JOIN FETCH u.orders WHERE u.id = :id")
     * Optional<User> findByIdWithOrders(Long id);
     *
     * User user = userRepository.findByIdWithOrders(123L);
     * // Now can safely access orders even outside the transaction
     * for (Order order : user.getOrders()) {
         *     processOrder(order);  // No LazyInitializationException
     * }
     * }</pre>
     */
    LAZY
}
