package de.happybavarian07.coolstufflib.jpa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Defines a custom query to be executed by a repository method.</p>
 *
 * <p>This annotation allows you to specify SQL or JPQL (Java Persistence Queries Language)
 * queries directly on repository methods, providing fine-grained control over data
 * access beyond the standard CRUD operations provided by repositories.</p>
 *
 * <h3>Query Types:</h3>
 * <ul>
 *   <li><strong>JPQL</strong>: Object-oriented query language operating on entity classes.
 *       Default when nativeQuery=false. Portable across databases.</li>
 *   <li><strong>Native SQL</strong>: Database-specific SQL queries. Use when nativeQuery=true.
 *       Provides full SQL capabilities but reduces portability.</li>
 * </ul>
 *
 * <h3>JPQL Example:</h3>
 * <pre>{@code
 * public interface OrderRepository extends JpaRepository<Order, Long> {
 * 
 *     // JPQL - uses entity and field names
 *     @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId AND o.status = 'PENDING'")
 *     List<Order> findPendingOrdersByCustomer(Long customerId);
 *
 *     // JPQL with JOIN - traverse relationships
 *     @Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.product.id = :productId")
 *     List<Order> findOrdersContainingProduct(Long productId);
 *
 *     // JPQL aggregation query
 *     @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED'")
 *     BigDecimal getTotalRevenue();
 * }
 * }</pre>
 *
 * <h3>Native SQL Example:</h3>
 * <pre>{@code
 * public interface OrderRepository extends JpaRepository<Order, Long> {
 * 
 *     // Native SQL - uses table and column names
 *     @Query(value = "SELECT * FROM orders WHERE customer_id = ?1 AND status = 'PENDING'",
 *            nativeQuery = true)
 *     List<Order> findPendingOrdersByCustomerNative(Long customerId);
 *
 *     // Native SQL with named parameters
 *     @Query(value = "SELECT * FROM orders WHERE customer_id = :customerId ORDER BY created_at DESC",
 *            nativeQuery = true)
 *     List<Order> findByCustomerWithSorting(@Param("customerId") Long customerId);
 * }
 * }</pre>
 *
 * <h3>Key Differences: JPQL vs Native SQL:</h3>
 * <ul>
 *   <li><strong>JPQL</strong>: Uses entity names (Order), field names (customer.id)
 *       - Portable across databases
 *       - Works with entity relationships automatically
 *       - Cannot use database-specific functions/features</li>
 *   <li><strong>Native SQL</strong>: Uses table names (orders), column names (customer_id)
 *       - Full access to database features and functions
 *       - Can leverage database-specific optimizations
 *       - Not portable - query may need changes for different databases</li>
 * </ul>
 *
 * <h3>Parameter Binding:</h3>
 * <p>Queries support two parameter binding styles:</p>
 * <ul>
 *   <li><strong>Named parameters</strong>: {@code :parameterName} - Explicit, readable
 *       <pre>{@code @Query("SELECT o FROM Order o WHERE o.customer.id = :custId")}</pre></li>
 *   <li><strong>Positional parameters</strong>: {@code ?1, ?2, ...} - Position-based (1-indexed)
 *       <pre>{@code @Query("SELECT o FROM Order o WHERE o.status = ?1 AND o.id IN ?2")}</pre></li>
 * </ul>
 *
 * <h3>When to Use Each Type:</h3>
 * <ul>
 *   <li><strong>Use JPQL when</strong>: You need portability, working with relationships,
 *       or using standard CRUD-like queries</li>
 *   <li><strong>Use Native SQL when</strong>: You need database-specific features (full-text search,
 *       JSON functions, window functions), complex aggregations, or query optimization</li>
 * </ul>
 *
 * @param value The query string - either JPQL (default) or native SQL.
 *               For JPQL: Uses entity and field names. For native SQL:
 *               uses table and column names. Supports named parameters (:name)
 *               and positional parameters (?1, ?2, ...).
 *
 * @param nativeQuery When true, the value is treated as a native SQL query.
 *                    When false (default), the value is treated as JPQL.
 *                    Set to true when using database-specific SQL syntax or features.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Query {
    /**
     * <p>The query string to execute.</p>
     *
     * <p>This can be either:</p>
     * <ul>
     *   <li><strong>JPQL</strong>: Object-oriented query using entity names
     *       <pre>{@code @Query("SELECT o FROM Order o WHERE o.status = 'PENDING'")}</pre></li>
     *   <li><strong>Native SQL</strong>: Database-specific query when {@link #nativeQuery()} is true
     *       <pre>{@code @Query(value = "SELECT * FROM orders WHERE status = 'PENDING'", nativeQuery = true)}</pre></li>
     * </ul>
     *
     * <p>Parameter binding options:</p>
     * <ul>
     *   <li><strong>Named parameters</strong>: Use {@code :parameterName} in query,
     *       match with method parameter using {@link Param} annotation</li> (not yet)
     *   <li><strong>Positional parameters</strong>: Use {@code ?1, ?2, ...} (1-indexed),
     *       matched by position with method parameters</li>
     * </ul>
     *
     * @return The query string
     */
    String value();

    /**
     * <p>Indicates whether the query is native SQL or JPQL.</p>
     *
     * <p><strong>false (default)</strong>: The {@link #value()} contains a JPQL query.
     * JPQL operates on entity classes and their relationships, providing database portability.</p>
     *
     * <pre>{@code
     * // JPQL - default behavior
     * @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId")
     * List<Order> findByCustomer(Long customerId);
     * }</pre>
     *
     * <p><strong>true</strong>: The {@link #value()} contains a native SQL query.
     * Use this when you need database-specific features or syntax.</p>
     *
     * <pre>{@code
     * // Native SQL - uses table/column names, database functions
     * @Query(value = "SELECT * FROM orders WHERE customer_id = ?1 AND TIMESTAMPDIFF(YEAR, created_at, NOW()) < 1",
     *        nativeQuery = true)
     * List<Order> findRecentOrders(Long customerId);
     *
     * // Native SQL with PostgreSQL JSON support
     * @Query(value = "SELECT * FROM products WHERE metadata::jsonb ->> 'category' = :cat",
     *        nativeQuery = true)
     * List<Product> findByMetadataCategory(String cat);
     * }</pre>
     *
     * <h3>Considerations:</h3>
     * <ul>
     *   <li><strong>Portability</strong>: Native SQL queries may not work on different database types
     *       (MySQL syntax differs from PostgreSQL, Oracle, etc.)</li>
     *   <li><strong>Entity mapping</strong>: For native queries returning entities, column names
     *       must match entity field mappings</li>
     *   <li><strong>Relationships</strong>: Native SQL doesn't automatically handle entity relationships;
     *       you may need to manually fetch related entities</li>
     * </ul>
     *
     * @return True for native SQL query, false for JPQL (default is false)
     */
    boolean nativeQuery() default false;
}
