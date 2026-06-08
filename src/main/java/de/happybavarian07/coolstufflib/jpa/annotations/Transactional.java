package de.happybavarian07.coolstufflib.jpa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Configures transactional boundaries for method execution.</p>
 *
 * <p>This annotation marks methods that should execute within a database transaction.
     * All database operations performed by the annotated method are grouped into a single
     * transaction that follows ACID principles: Atomicity, Consistency, Isolation, and Durability.</p>
 *
 * <h3>Transaction Semantics:</h3>
 * <ul>
         * <li><strong>Atomicity</strong>: All operations succeed or all fail - no partial updates</li>
     * <li><strong>Consistency</strong>: Database remains in valid state before and after transaction</li>
         * <li><strong>Isolation</strong>: Concurrent transactions don't interfere with each other</li>
         * <li><strong>Durability</strong>: Committed changes persist even after system failure</li>
     * </ul>
 *
 * <pre>{@code
     * @Service
     * public class OrderService {
     *     
     *     // Entire method executes in a single transaction
         *     @Transactional
     *     public void placeOrder(Order order, List<OrderItem> items) {
         *         // Save order
         *         orderRepository.save(order);
         *
         *         // Add all items to the order
         *         for (OrderItem item : items) {
             *             item.setOrder(order);
     *                 orderRepository.save(item);
         *         }
         *
         *         // Update inventory
         *         updateInventory(items);
     *         
         *         // If any operation throws exception, ALL changes are rolled back
     *     }
     * }
     * }</pre>
 *
 * <h3>Transaction Lifecycle:</h3>
 * <ol>
         * <li>Method entry: Transaction begins (or existing transaction is joined)</li>
         * <li>Method execution: All database operations participate in the transaction</li>
         * <li>Successful completion: Transaction commits - changes are saved permanently</li>
     * <li>Exception thrown: Transaction rolls back - all changes are discarded</li>
     * </ol>
 *
 * @param readOnly Optimize for read-only operations. When true, the transaction is marked
     *              as read-only which may enable database-level optimizations. Default is false.
 *
 * @param timeout Maximum time in seconds that the transaction can run before automatically
     *             timing out and rolling back. Use -1 for no timeout (default).
 *
 * @param rollbackFor Specify exception types that should trigger a rollback. By default,
     *                 RuntimeException and Error cause rollback; checked exceptions do not.
     *                 Use this to add additional exception types that should trigger rollback.
 *
 * @param noRollbackFor Specify exception types that should NOT trigger a rollback, even if
     *                   they would normally do so. Useful for expected exceptions that don't
     *                   indicate transaction failure (e.g., custom business logic exceptions).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Transactional {
    /**
     * <p>Mark the transaction as read-only for potential optimization.</p>
     *
     * <p>When set to {@code true}, the transaction is flagged as reading data without
     * any modifications. This allows several optimizations:</p>
     *
     * <ul>
         * <li><strong>Database-level</strong>: Some databases can optimize read-only transactions,
             *       use different isolation levels, or route to read replicas in master-slave setups</li>
         * <li><strong>Cache-friendly</strong>: Read-only operations may be cached more aggressively</li>
     * <li><strong>Validation</strong>: If write operations are attempted, an exception is thrown</li>
     * </ul>
     *
     * <pre>{@code
     * @Service
     * public class ReportService {
         *     
         *     // Read-only transaction - optimized for queries, no writes allowed
         *     @Transactional(readOnly = true)
     *     public OrderSummary generateOrderSummary(Long orderId) {
         *         Order order = orderRepository.findById(orderId);
         *         List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
         *
         *         // Can only read - cannot modify or save
         *         return buildSummary(order, items);
     *       }
         *    
         *     // Read-write transaction for modifications
         *     @Transactional(readOnly = false)  // Default behavior
     *     public void updateOrderStatus(Long orderId, String status) {
             *         Order order = orderRepository.findById(orderId);
     *             order.setStatus(status);  // Write operation allowed
         *    }
     *     }
     * }</pre>
     *
     * <h3>When to Use readOnly=true:</h3>
     * <ul>
         * <li><strong>Query methods</strong>: SELECT operations that don't modify data</li>
     * <li><strong>Report generation</strong>: Aggregating and reading existing data</li>
     * <li><strong>Data export</strong>: Extracting data without changes</li>
     * </ul>
     *
     * @return True for read-only transactions, false for read-write (default is false)
     */
    boolean readOnly() default false;

    /**
     * <p>Specify the maximum execution time for the transaction in seconds.</p>
     *
     * <p>When a transaction exceeds the specified timeout duration, it is automatically
         * rolled back and a TimeoutException (or database-specific equivalent) is thrown.
     *</p>
     *
     * <pre>{@code
     * @Service
     * public class PaymentService {
     *     
     *     // Transaction must complete within 30 seconds or auto-roll back
         *     @Transactional(timeout = 30)
     *     public PaymentResult processPayment(PaymentRequest request) {
         *         // Complex payment processing with external API calls
             *         validatePayment(request);
     *             chargeCard(request.getAmount());
         *         updateOrderStatus(request.getOrderId(), "PAID");
         *        sendConfirmationEmail(request.getEmail());
         *
         *         // If this takes longer than 30 seconds, transaction rolls back
     *             return new PaymentResult("SUCCESS");
         *    }
     * }
     *
     * // No timeout - transaction can run indefinitely
     * @Transactional(timeout = -1)
     * public void longRunningBatchProcess() { ... }
     * }</pre>
     *
     * <h3>Common Timeout Values:</h3>
     * <ul>
         * <li><strong>-1</strong>: No timeout (default) - transaction can run indefinitely</li>
         * <li><strong>5-30 seconds</strong>: Short transactions, user-facing operations</li>
     * <li><strong>60-300 seconds</strong>: Batch processing, report generation</li>
     * </ul>
     *
     * <p><strong>Note:</strong> Timeout support depends on the underlying database and
         * connection pool configuration. Some databases may not fully support transaction
         * timeouts.</p>
     *
     * @return Maximum execution time in seconds, or -1 for no timeout (default is -1)
     */
    int timeout() default -1;

    /**
     * <p>Specify exception types that should trigger a transaction rollback.</p>
     *
     * <p>By default, only {@link RuntimeException} and {@link Error} subclasses cause
         * rollback. Checked exceptions (classes extending {@link Exception} but not
             * {@link RuntimeException}) do NOT trigger rollback by default - the transaction
         * commits even if a checked exception is thrown.</p>
     *
     * <p>Use this attribute to specify additional exception types that should trigger
         * rollback beyond RuntimeException and Error.</p>
     *
     * <pre>{@code
     * @Service
     * public class OrderService {
     *     
     *     // Rollback on both RuntimeException AND InsufficientStockException
         *     @Transactional(rollbackFor = InsufficientStockException.class)
     *     public void reserveInventory(Order order) throws InsufficientStockException {
             *         for (OrderItem item : order.getItems()) {
             *             Stock stock = stockRepository.findById(item.getProductId());
     *                if (stock.getQuantity() < item.getQuantity()) {
                     *                 throw new InsufficientStockException("Not enough stock!");
             *            }
             *            // Reserve the items
         *        }
         *
         *        // If InsufficientStockException is thrown, entire transaction rolls back
     *           } else {
             *       orderRepository.save(order);  // Commit if all checks pass
         *    }
     *       }
     * }
     *
     * // InsufficientStockException is a checked exception
     * public class InsufficientStockException extends Exception {
     *     public InsufficientStockException(String message) { super(message); }
     * }
     * }</pre>
     *
     * <h3>Default Rollback Behavior:</h3>
     * <ul>
         * <li><strong>RuntimeException</strong>: Causes rollback (default)</li>
     * <li><strong>Error</strong>: Causes rollback (default)</li>
     * <li><strong>Checked Exception</strong>: Does NOT cause rollback (transaction commits)</li>
     * </ul>
     *
     * @return Array of exception types that should trigger rollback in addition to
         *        RuntimeException and Error. Empty array by default.
     */
    Class<? extends Throwable>[] rollbackFor() default {};

    /**
     * <p>Specify exception types that should NOT trigger a transaction rollback.</p>
     *
     * <p>This provides an override for the default rollback behavior. Exceptions listed
         * here will cause the transaction to commit normally, even if they would otherwise
             * trigger a rollback (i.e., they extend RuntimeException).</p>
     *
     * <pre>{@code
     * @Service
     * public class UserService {
     *     
     *     // Continue and commit despite DuplicateEmailException
         *     @Transactional(noRollbackFor = DuplicateEmailException.class)
     *     public User createUser(String email, String name) throws DuplicateEmailException {
             *         if (userRepository.existsByEmail(email)) {
                     *                 throw new DuplicateEmailException("Email already registered");
             *        }
     *
     *            User user = new User(email, name);
             *    userRepository.save(user);
             *    return user;
         *    }
     * }
     *
     * // Usage:
     * try {
     *      userService.createUser("existing@email.com", "John");
         * } catch (DuplicateEmailException e) {
         *        logger.warn(e.getMessage());  // Transaction committed, user not created
             *    showErrorMessageToUser(e.getMessage());
         * }
     *
     * // DuplicateEmailException is a RuntimeException
     * public class DuplicateEmailException extends RuntimeException { ... }
     * }</pre>
     *
     * <h3>When to Use noRollbackFor:</h3>
     * <ul>
         * <li><strong>Expected business exceptions</strong>: Validation failures that don't
             *       indicate system errors (duplicate entries, missing optional data)</li>
         * <li><strong>Partial success scenarios</strong>: Some operations succeed but others fail,
             *       and you want to keep the successful ones</li>
     * </ul>
     *
     * <p><strong>Note:</strong> noRollbackFor takes precedence over rollbackFor. If an
         * exception type is specified in both, no rollback occurs.</p>
     *
     * @return Array of exception types that should NOT trigger rollback,
             *        overriding the default behavior. Empty array by default.
    */
    Class<? extends Throwable>[] noRollbackFor() default {};
}
