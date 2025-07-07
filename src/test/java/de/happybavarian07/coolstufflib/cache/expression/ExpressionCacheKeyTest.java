package de.happybavarian07.coolstufflib.cache.expression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ExpressionCacheKeyTest {

    @Test
    void testBasicKeyCreation() {
        Map<String, Object> variables = Map.of("x", 10, "y", 20);
        ExpressionCacheKey key = new ExpressionCacheKey("x + y", variables);

        assertNotNull(key);
        assertEquals("x + y", key.getExpression());
    }

    @Test
    void testKeyEquality() {
        Map<String, Object> vars1 = Map.of("x", 10, "y", 20);
        Map<String, Object> vars2 = Map.of("x", 10, "y", 20);

        ExpressionCacheKey key1 = new ExpressionCacheKey("x + y", vars1);
        ExpressionCacheKey key2 = new ExpressionCacheKey("x + y", vars2);

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    void testKeyInequalityDifferentExpressions() {
        Map<String, Object> variables = Map.of("x", 10);

        ExpressionCacheKey key1 = new ExpressionCacheKey("x + 1", variables);
        ExpressionCacheKey key2 = new ExpressionCacheKey("x + 2", variables);

        assertNotEquals(key1, key2);
    }

    @Test
    void testKeyInequalityDifferentVariables() {
        ExpressionCacheKey key1 = new ExpressionCacheKey("x + y", Map.of("x", 10, "y", 20));
        ExpressionCacheKey key2 = new ExpressionCacheKey("x + y", Map.of("x", 15, "y", 20));

        assertNotEquals(key1, key2);
    }

    @Test
    void testVariableOrderIndependence() {
        Map<String, Object> vars1 = new HashMap<>();
        vars1.put("x", 10);
        vars1.put("y", 20);

        Map<String, Object> vars2 = new HashMap<>();
        vars2.put("y", 20);
        vars2.put("x", 10);

        ExpressionCacheKey key1 = new ExpressionCacheKey("x + y", vars1);
        ExpressionCacheKey key2 = new ExpressionCacheKey("x + y", vars2);

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    void testNullExpression() {
        Map<String, Object> variables = Map.of("x", 10);
        ExpressionCacheKey key = new ExpressionCacheKey(null, variables);

        assertNull(key.getExpression());
    }

    @Test
    void testEmptyVariables() {
        ExpressionCacheKey key1 = new ExpressionCacheKey("42", Map.of());
        ExpressionCacheKey key2 = new ExpressionCacheKey("42", Map.of());

        assertEquals(key1, key2);
    }

    @Test
    void testNullVariables() {
        ExpressionCacheKey key1 = new ExpressionCacheKey("42", null);
        ExpressionCacheKey key2 = new ExpressionCacheKey("42", null);

        assertEquals(key1, key2);
    }

    @Test
    void testComplexVariableTypes() {
        Map<String, Object> variables = Map.of(
            "string", "test",
            "integer", 42,
            "double", 3.14,
            "boolean", true
        );

        ExpressionCacheKey key1 = new ExpressionCacheKey("test expression", variables);
        ExpressionCacheKey key2 = new ExpressionCacheKey("test expression", new HashMap<>(variables));

        assertEquals(key1, key2);
    }

    @Test
    void testHashCodeConsistency() {
        Map<String, Object> variables = Map.of("x", 10, "y", 20);
        ExpressionCacheKey key = new ExpressionCacheKey("x + y", variables);

        int hashCode1 = key.hashCode();
        int hashCode2 = key.hashCode();

        assertEquals(hashCode1, hashCode2);
    }

    @Test
    void testToString() {
        Map<String, Object> variables = Map.of("x", 10);
        ExpressionCacheKey key = new ExpressionCacheKey("x * 2", variables);

        String toString = key.toString();
        System.out.println(toString);
        assertNotNull(toString);
        assertTrue(toString.contains("x * 2"));
    }
}
