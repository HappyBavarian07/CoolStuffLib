package de.happybavarian07.coolstufflib.languagemanager.expressionengine;

import de.happybavarian07.coolstufflib.languagemanager.expressionengine.conditions.*;
import de.happybavarian07.coolstufflib.languagemanager.expressionengine.interfaces.Condition;
import de.happybavarian07.coolstufflib.languagemanager.expressionengine.interfaces.MaterialCondition;
import de.happybavarian07.coolstufflib.languagemanager.expressionengine.operations.MathOperation;
import de.happybavarian07.coolstufflib.languagemanager.expressionengine.operations.MathOperations;
import org.bukkit.Material;

/**
 * <p>A fluent builder class for constructing complex conditional expressions used in the language manager's
 * expression engine. This builder provides a convenient API for creating various types of conditions including
 * mathematical comparisons, logical operations, and material-based conditions.</p>
 *
 * <p>The ConditionBuilder follows the builder pattern, allowing method chaining to create complex conditional
 * expressions in a readable and maintainable way. Each method returns the builder instance, enabling fluent
 * construction of conditions.</p>
 *
 * <p>Supported condition types:</p>
 * <ul>
 * <li>Equality and inequality comparisons (equal, notEqual)</li>
 * <li>Relational comparisons (greaterThan, lessThan, greaterThanOrEqual, lessThanOrEqual)</li>
 * <li>Mathematical operations with comparisons (math)</li>
 * <li>Logical operations (and, or, not)</li>
 * <li>Ternary conditional expressions (ternary)</li>
 * <li>Material-based conditions with true/false materials</li>
 * </ul>
 *
 * <pre><code>
 * // Simple equality condition
 * ConditionBuilder builder = new ConditionBuilder()
 *     .equal("player_level", 10);
 *
 * // Complex mathematical condition
 * ConditionBuilder mathBuilder = new ConditionBuilder()
 *     .math(player.getLevel(), 5, "addition", ">", 15);
 *
 * // Logical combination
 * ConditionBuilder logicalBuilder = new ConditionBuilder()
 *     .and(condition1, condition2)
 *     .trueMaterial(Material.DIAMOND)
 *     .falseMaterial(Material.STONE);
 * </code></pre>
 */
public class ConditionBuilder {
    private MaterialCondition condition;

    /**
     * <p>Creates a new condition builder with no initial condition.</p>
     *
     * <pre><code>
     * ConditionBuilder builder = new ConditionBuilder();
     * builder.equal("health", 100);
     * </code></pre>
     */
    public ConditionBuilder() {
    }

    /**
     * <p>Creates a new condition builder with an initial condition.</p>
     *
     * <pre><code>
     * MaterialCondition existing = new EqualCondition("level", 5);
     * ConditionBuilder builder = new ConditionBuilder(existing);
     * </code></pre>
     *
     * @param condition the initial material condition to start with
     */
    public ConditionBuilder(MaterialCondition condition) {
        this.condition = condition;
    }

    /**
     * <p>Creates an equality condition that checks if two values are equal.</p>
     *
     * <pre><code>
     * builder.equal("player_name", "Steve");
     * builder.equal(player.getLevel(), 10);
     * </code></pre>
     *
     * @param value1 the first value to compare
     * @param value2 the second value to compare
     * @return this builder instance for method chaining
     */
    public ConditionBuilder equal(Object value1, Object value2) {
        condition = new EqualCondition(value1, value2);
        return this;
    }

    /**
     * <p>Creates an inequality condition that checks if two values are not equal.</p>
     *
     * <pre><code>
     * builder.notEqual("player_status", "banned");
     * builder.notEqual(player.getHealth(), 0);
     * </code></pre>
     *
     * @param value1 the first value to compare
     * @param value2 the second value to compare
     * @return this builder instance for method chaining
     */
    public ConditionBuilder notEqual(Object value1, Object value2) {
        condition = new NotEqualCondition(value1, value2);
        return this;
    }

    /**
     * <p>Creates a greater than condition that checks if the first value is greater than the second.</p>
     *
     * <pre><code>
     * builder.greaterThan(player.getLevel(), 5);
     * builder.greaterThan("score", 1000);
     * </code></pre>
     *
     * @param value1 the first value to compare
     * @param value2 the second value to compare
     * @return this builder instance for method chaining
     */
    public ConditionBuilder greaterThan(Object value1, Object value2) {
        condition = new GreaterThanCondition(value1, value2);
        return this;
    }

    /**
     * <p>Creates a less than condition that checks if the first value is less than the second.</p>
     *
     * <pre><code>
     * builder.lessThan(player.getHealth(), 20);
     * builder.lessThan("experience", 100);
     * </code></pre>
     *
     * @param value1 the first value to compare
     * @param value2 the second value to compare
     * @return this builder instance for method chaining
     */
    public ConditionBuilder lessThan(Object value1, Object value2) {
        condition = new LessThanCondition(value1, value2);
        return this;
    }

    /**
     * <p>Creates a greater than or equal condition that checks if the first value is greater than or equal to the second.</p>
     *
     * <pre><code>
     * builder.greaterThanOrEqual(player.getLevel(), 10);
     * builder.greaterThanOrEqual("money", 500);
     * </code></pre>
     *
     * @param value1 the first value to compare
     * @param value2 the second value to compare
     * @return this builder instance for method chaining
     */
    public ConditionBuilder greaterThanOrEqual(Object value1, Object value2) {
        condition = new GreaterThanOrEqualCondition(value1, value2);
        return this;
    }

    /**
     * <p>Creates a less than or equal condition that checks if the first value is less than or equal to the second.</p>
     *
     * <pre><code>
     * builder.lessThanOrEqual(player.getFoodLevel(), 18);
     * builder.lessThanOrEqual("deaths", 3);
     * </code></pre>
     *
     * @param value1 the first value to compare
     * @param value2 the second value to compare
     * @return this builder instance for method chaining
     */
    public ConditionBuilder lessThanOrEqual(Object value1, Object value2) {
        condition = new LessThanOrEqualCondition(value1, value2);
        return this;
    }

    /**
     * <p>Creates a mathematical condition that performs an operation on two numbers and compares the result.</p>
     *
     * <pre><code>
     * // Check if (level + experience) > 50
     * builder.math(player.getLevel(), player.getExp(), "addition", ">", 50);
     *
     * // Check if (health * 2) <= 40
     * builder.math(player.getHealth(), 2, "multiplication", "<=", 40);
     * </code></pre>
     *
     * @param value1 the first numeric value
     * @param value2 the second numeric value
     * @param operationName the name of the mathematical operation (addition, subtraction, multiplication, division, etc.)
     * @param comparisonOperator the comparison operator (>, <, >=, <=, ==, !=)
     * @param comparisonValue the value to compare the operation result against
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if the operation name is unknown
     */
    public ConditionBuilder math(Number value1, Number value2, String operationName, String comparisonOperator, Number comparisonValue) {
        MathOperation operation = MathOperations.getOperation(operationName);
        if (operation == null) {
            throw new IllegalArgumentException("Unknown operation: " + operationName);
        }
        condition = new MathCondition(value1, value2, operation, comparisonOperator, comparisonValue);
        return this;
    }

    /**
     * <p>Creates a logical AND condition that requires all provided conditions to be true.</p>
     *
     * <pre><code>
     * Condition levelCheck = new GreaterThanCondition(player.getLevel(), 5);
     * Condition healthCheck = new GreaterThanCondition(player.getHealth(), 10);
     * builder.and(levelCheck, healthCheck);
     * </code></pre>
     *
     * @param conditions the conditions to combine with logical AND
     * @return this builder instance for method chaining
     */
    public ConditionBuilder and(Condition... conditions) {
        condition = new AndCondition(conditions);
        return this;
    }

    /**
     * <p>Creates a logical OR condition that requires at least one of the provided conditions to be true.</p>
     *
     * <pre><code>
     * Condition isAdmin = new EqualCondition(player.getRole(), "admin");
     * Condition isModerator = new EqualCondition(player.getRole(), "moderator");
     * builder.or(isAdmin, isModerator);
     * </code></pre>
     *
     * @param conditions the conditions to combine with logical OR
     * @return this builder instance for method chaining
     */
    public ConditionBuilder or(Condition... conditions) {
        condition = new OrCondition(conditions);
        return this;
    }

    /**
     * <p>Creates a logical NOT condition that negates the provided condition.</p>
     *
     * <pre><code>
     * Condition isBanned = new EqualCondition(player.getStatus(), "banned");
     * builder.not(isBanned);
     * </code></pre>
     *
     * @param condition the condition to negate
     * @return this builder instance for method chaining
     */
    public ConditionBuilder not(Condition condition) {
        this.condition = new NotCondition(condition);
        return this;
    }

    /**
     * <p>Creates a ternary conditional expression that returns different material conditions based on a boolean condition.</p>
     *
     * <pre><code>
     * Condition hasPermission = new EqualCondition(player.hasPermission("admin"), true);
     * MaterialCondition adminCondition = new HeadMaterialCondition(Material.DIAMOND_BLOCK);
     * MaterialCondition userCondition = new HeadMaterialCondition(Material.STONE);
     * builder.ternary(hasPermission, adminCondition, userCondition);
     * </code></pre>
     *
     * @param condition the boolean condition to evaluate
     * @param trueCondition the material condition to use if the condition is true
     * @param falseCondition the material condition to use if the condition is false
     * @return this builder instance for method chaining
     */
    public ConditionBuilder ternary(Condition condition, MaterialCondition trueCondition, MaterialCondition falseCondition) {
        this.condition = new TernaryCondition(condition, trueCondition, falseCondition);
        return this;
    }

    /**
     * <p>Creates a ternary conditional expression that returns different materials based on a boolean condition.</p>
     *
     * <pre><code>
     * Condition isOnline = new EqualCondition(player.isOnline(), true);
     * builder.ternary(isOnline, Material.GREEN_WOOL, Material.RED_WOOL);
     * </code></pre>
     *
     * @param condition the boolean condition to evaluate
     * @param trueMaterial the material to use if the condition is true
     * @param falseMaterial the material to use if the condition is false
     * @return this builder instance for method chaining
     */
    public ConditionBuilder ternary(Condition condition, Material trueMaterial, Material falseMaterial) {
        this.condition = new TernaryCondition(condition, trueMaterial, falseMaterial);
        return this;
    }

    /**
     * <p>Sets the material to use when the current condition evaluates to true.</p>
     *
     * <pre><code>
     * builder.equal(player.isOnline(), true)
     *        .trueMaterial(Material.EMERALD_BLOCK);
     * </code></pre>
     *
     * @param material the material to use when the condition is true
     * @return this builder instance for method chaining
     */
    public ConditionBuilder trueMaterial(Material material) {
        if (condition != null) {
            condition.setTrueMaterial(material);
        }
        return this;
    }

    /**
     * <p>Sets the material to use when the current condition evaluates to false.</p>
     *
     * <pre><code>
     * builder.equal(player.hasPermission("fly"), true)
     *        .trueMaterial(Material.FEATHER)
     *        .falseMaterial(Material.STONE);
     * </code></pre>
     *
     * @param material the material to use when the condition is false
     * @return this builder instance for method chaining
     */
    public ConditionBuilder falseMaterial(Material material) {
        if (condition != null) {
            condition.setFalseMaterial(material);
        }
        return this;
    }

    /**
     * <p>Sets the default material to use when the condition cannot be evaluated or returns an unexpected result.</p>
     *
     * <pre><code>
     * builder.equal(player.getName(), "unknown")
     *        .trueMaterial(Material.DIAMOND)
     *        .falseMaterial(Material.STONE)
     *        .defaultMaterial(Material.BEDROCK);
     * </code></pre>
     *
     * @param material the default material to use as fallback
     * @return this builder instance for method chaining
     */
    public ConditionBuilder defaultMaterial(Material material) {
        if (condition != null) {
            condition.setDefaultMaterial(material);
        }
        return this;
    }

    /**
     * <p>Builds and returns the final MaterialCondition that was constructed using this builder.</p>
     *
     * <pre><code>
     * MaterialCondition condition = new ConditionBuilder()
     *     .equal(player.getLevel(), 10)
     *     .trueMaterial(Material.GOLD_BLOCK)
     *     .falseMaterial(Material.DIRT)
     *     .build();
     * </code></pre>
     *
     * @return the constructed MaterialCondition
     * @throws IllegalStateException if no condition has been created yet
     */
    public MaterialCondition build() {
        if (condition == null) {
            throw new IllegalStateException("No condition has been created");
        }
        return condition;
    }
}