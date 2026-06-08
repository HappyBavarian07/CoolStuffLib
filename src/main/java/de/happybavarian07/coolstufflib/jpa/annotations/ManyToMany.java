package de.happybavarian07.coolstufflib.jpa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Maps a many-to-many relationship between two entity classes.</p>
 *
 * <p>This annotation is used when multiple instances of one entity can be associated with
 * multiple instances of another entity, forming a bidirectional many-to-many relationship.
 * Unlike {@link OneToMany} and {@link ManyToOne}, which use foreign key columns directly,
 * many-to-many relationships require an intermediate join table.</p>
 *
 * <h3>Key Concepts:</h3>
 * <ul>
 *   <li><strong>Join table</strong>: An automatically created or specified table that stores
 *       pairs of foreign keys linking the two entities</li>
 *   <li><strong>Bidirectional by nature</strong>: Both sides can access the collection of related entities</li>
 *   <li><strong>No direct foreign key</strong>: Neither entity holds a foreign key to the other directly</li>
 * </ul>
 *
 * <pre>{@code
 * // Many-to-many relationship: students enrolled in courses, courses have students
 * 
 * @Entity(name = "Student")
 * public class Student {
 *     @Id
 *     private Long id;
 *     private String name;
 *     
 *     // A student can enroll in multiple courses
 *     @ManyToMany(targetEntity = Course.class)
 *     @JoinTable(
 *         name = "student_course",
 *         joinColumns = @JoinColumn(name = "student_id"),
 *         inverseJoinColumns = @JoinColumn(name = "course_id")
 *     )
 *     private Set<Course> enrolledCourses;
 * }
 * 
 * @Entity(name = "Course")
 * public class Course {
 *     @Id
 *     private Long id;
 *     private String title;
 *     
 *     // A course can have multiple students enrolled
 *     @ManyToMany(mappedBy = "enrolledCourses")
 *     private Set<Student> students;
 * }</pre>
 *
 * <p><strong>Note:</strong> Many-to-many relationships should typically use {@link JoinTable}
 * to specify the join table configuration. Without it, a default naming convention is used.
 * See {@link JoinTable} for detailed configuration options.</p>
 *
 * <h3>Cascade Operations:</h3>
 * <p>Specify cascade types via {@link #cascade()} to propagate operations from one entity
 * to related entities through the join table relationship.</p>
 *
 * @param mappedBy The field name in the target entity that holds the inverse side of the
 *                 many-to-many relationship. When set, this entity is the "non-owning"
 *                 side and does not control the join table updates.
 * 
 * @param targetEntity The class type of entities in the collection. Required when the
 *                     collection's generic type cannot be determined from field declaration.
 *                     Default is void.class (inferred from field type if possible).
 *
 * @param cascade Cascade operations to propagate to related entities. See {@link CascadeType}
 *                for options: PERSIST, MERGE, REMOVE, REFRESH, DETACH, ALL.
 *                Default is empty array (no cascading).
 *
 * @param fetch The fetch type for loading related data. {@link FetchType#EAGER} loads
 *              the collection immediately when accessing the entity. {@link FetchType#LAZY}
 *              defers loading until the collection is actually accessed. Default is LAZY.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ManyToMany {
    /**
     * <p>The field name in the target entity that holds the inverse side of this many-to-many relationship.</p>
     *
     * <p>When set, this indicates the non-owning (inverse) side of the bidirectional
     * relationship. The owning side is the one without mappedBy and manages the join table.
     *</p>
     *
     * <pre>{@code
     * // Owning side - manages the student_course join table
     * @Entity(name = "Student")
     * public class Student {
     *     @Id private Long id;
     *     
     *     @ManyToMany(targetEntity = Course.class)
     *     @JoinTable(
     *         name = "student_course",
     *         joinColumns = @JoinColumn(name = "student_id"),
     *         inverseJoinColumns = @JoinColumn(name = "course_id")
     *     )
     *     private Set<Course> enrolledCourses;
     * }
     *
     * // Inverse side -(mappedBy references the field name "enrolledCourses" above)
     * @Entity(name = "Course")
     * public class Course {
     *     @Id private Long id;
     *     
     *     @ManyToMany(mappedBy = "enrolledCourses")
     *     private Set<Student> students;
     * }
     * }</pre>
     *
     * <p><strong>Key points:</strong></p>
     * <ul>
     *   <li>The mappedBy field name must match the exact Java field name (not property name)</li>
     *   <li>Only one side should have {@link JoinTable} - typically the owning side</li>
     *   <li><strong>Bidirectional synchronization:</strong> Modifying only one side of a
     *       bidirectional many-to-many relationship may not persist correctly. Always update
     *       both sides for consistency.</li>
     * </ul>
     *
     * <pre>{@code
     * // Correct: Update both sides
     * student.getEnrolledCourses().add(course);
     * course.getStudents().add(student);
     * 
     * // Incorrect: Only updating one side may cause inconsistencies
     * student.getEnrolledCourses().add(course);
     * }</pre>
     *
     * @return The field name of the inverse side relationship,
     *         or empty string if this is the owning side (default)
     */
    String mappedBy() default "";

    /**
     * <p>The target entity class for this many-to-many relationship.</p>
     *
     * <p>Specify when the collection's generic type cannot be inferred from the field declaration.
     * This is typically needed when using raw collection types without generics, or in certain
     * edge cases where type inference fails.</p>
     *
     * <pre>{@code
     * // Generic type can be inferred - no need for targetEntity
     * @ManyToMany
     * private Set<Role> roles;
     *
     * // Raw collection without generics - must specify targetEntity
     * @ManyToMany(targetEntity = Role.class)
     * private Collection roles;
     * }</pre>
     *
     * <p>The target entity must be a valid JPA entity class annotated with {@link Entity}.</p>
     *
     * @return The Class object representing the target entity type,
     *         or void.class if inference should be used (default)
     */
    Class<?> targetEntity() default void.class;

    /**
     * <p>Specifies which operations cascade from this entity to related entities.</p>
     *
     * <p>Cascade types control how persistence operations on the owning entity propagate
     * through the join table to the target entities:</p>
     *
     * <ul>
     *   <li><strong>PERSIST</strong>: Saving this entity also persists new related entities</li>
     *   <li><strong>MERGE</strong>: Merging this entity also merges detached related entities</li>
     *   <li><strong>REMOVE</strong>: Deleting this entity also deletes all related entities
     *       (caution: this can delete shared entities referenced by other entities)</li>
     *   <li><strong>REFRESH</strong>: Refreshing this entity also refreshes related entities</li>
     *   <li><strong>DETACH</strong>: Detaching this entity also detaches related entities</li>
     *   <li><strong>ALL</strong>: All cascade types are enabled</li>
     * </ul>
     *
     * <pre>{@code
     * // Cascade PERSIST - adding a new role to student's collection saves the role too
     * @ManyToMany(cascade = CascadeType.PERSIST)
     * private Set<Role> roles;
     *
     * // WARNING: Cascade REMOVE can be dangerous with many-to-many
     * // Deleting a student might delete courses that other students are also enrolled in!
     * }</pre>
     *
     * <p><strong>Caution:</strong> Cascading REMOVE on many-to-many relationships can have
     * unintended consequences, as related entities may be shared among multiple owning entities.</p>
     *
     * @return Array of cascade types to enable. Default is empty array (no cascading).
     */
    CascadeType[] cascade() default {};

    /**
     * <p>Specifies when related data should be loaded from the database.</p>
     *
     * <p><strong>{@link FetchType#EAGER}</strong>: Related entities are loaded automatically
     * when this entity is accessed. All join table entries are queried immediately.
     * Typically achieved through SQL JOINs or separate SELECT queries.</p>
     *
     * <ul>
     *   <li>Pros: Collection available immediately, no N+1 query issues for this relationship</li>
     *   <li>Cons: Always loads potentially large collections, even if not needed
     *       (performance concern)</li>
     * </ul>
     *
     * <p><strong>{@link FetchType#LAZY}</strong>: Related entities are loaded only when the
     * collection is first accessed. Default behavior for many-to-many relationships.</p>
     *
     * <ul>
     *   <li>Pros: Better performance when collection is large or rarely accessed</li>
     *   <li>Cons: May cause LazyInitializationException if accessed outside transaction
     *       context; requires additional query when first accessed</li>
     * </ul>
     *
     * <pre>{@code
     * // EAGER - all roles loaded immediately with User entity
     * @ManyToMany(fetch = FetchType.EAGER)
     * private Set<Role> roles;
     *
     * User user = userRepository.findById(123L);
     * for (Role role : user.getRoles()) {
     *     System.out.println(role.getName());  // No extra query - already loaded
     * }
     *
     * // LAZY (default) - roles loaded only when collection is accessed
     * @ManyToMany
     * private Set<Role> roles;
     *
     * User user = userRepository.findById(123L);
     * for (Role role : user.getRoles()) {
     *     System.out.println(role.getName());  // Triggers additional query via join table
     * }
     * }</pre>
     *
     * <p><strong>Recommendation:</strong> Use LAZY for many-to-many relationships as the default,
     * since collections can grow large and eager loading always incurs overhead.</p>
     *
     * @return The fetch type strategy. Default is LAZY.
     */
    FetchType fetch() default FetchType.LAZY;
}
