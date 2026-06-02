package de.happybavarian07.coolstufflib.jpa.utils;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class for validating SQL structural elements to prevent SQL Injection.
 * This class ensures that only approved operators and identifiers are used in query construction.
 */
public final class SqlSafe {

    private static final Set<String> ALLOWED_OPERATORS = Set.of(
            "=", "!=", "<", ">", "<=", ">=", "LIKE", "IN", "NOT IN", "IS", "IS NOT"
    );

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private SqlSafe() {
        // Utility class
    }

    /**
     * Validates that the provided SQL operator is within the approved allow-list.
     *
     * @param op The SQL operator to validate (e.g., "=", "LIKE").
     * @throws IllegalArgumentException if the operator is null or not permitted.
     */
    public static void validateOperator(String op) {
        if (op == null || !ALLOWED_OPERATORS.contains(op.toUpperCase().trim())) {
            throw new IllegalArgumentException("Invalid or forbidden SQL operator: " + op);
        }
    }

    /**
     * Validates that the provided identifier (column or table name) conforms to a safe alphanumeric pattern.
     * This prevents identifier injection when falling back to raw field names.
     *
     * @param identifier The identifier to validate.
     * @throws IllegalArgumentException if the identifier is null or contains illegal characters.
     */
    public static void validateIdentifier(String identifier) {
        if (identifier == null || !IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Invalid or forbidden SQL identifier: " + identifier);
        }
    }
}
