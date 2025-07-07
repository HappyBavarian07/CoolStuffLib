package de.happybavarian07.coolstufflib.cache.expression;

import de.happybavarian07.coolstufflib.cache.InMemoryCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExpressionCacheTest {

    private ExpressionCache<String> expressionCache;

    @BeforeEach
    void setUp() {
        expressionCache = new ExpressionCache<>();
    }

    @Test
    void testBasicCacheOperations() {
        ExpressionCacheKey key = new ExpressionCacheKey("x + y",
            java.util.Map.of("x", 10, "y", 20));

        expressionCache.put(key, "result");
        assertEquals("result", expressionCache.get(key));
        assertTrue(expressionCache.containsKey(key));
    }

    @Test
    void testCacheWithDifferentVariableStates() {
        ExpressionCacheKey key1 = new ExpressionCacheKey("x + y",
            java.util.Map.of("x", 10, "y", 20));
        ExpressionCacheKey key2 = new ExpressionCacheKey("x + y",
            java.util.Map.of("x", 15, "y", 20));

        expressionCache.put(key1, "result1");
        expressionCache.put(key2, "result2");

        assertEquals("result1", expressionCache.get(key1));
        assertEquals("result2", expressionCache.get(key2));
    }

    @Test
    void testCacheWithSameExpressionSameVariables() {
        java.util.Map<String, Object> variables = java.util.Map.of("x", 10, "y", 20);

        ExpressionCacheKey key1 = new ExpressionCacheKey("x + y", variables);
        ExpressionCacheKey key2 = new ExpressionCacheKey("x + y", variables);

        expressionCache.put(key1, "cached_result");

        assertEquals("cached_result", expressionCache.get(key2));
        assertTrue(expressionCache.containsKey(key2));
    }

    @Test
    void testCacheRemoval() {
        ExpressionCacheKey key = new ExpressionCacheKey("x * 2",
            java.util.Map.of("x", 5));

        expressionCache.put(key, "result");
        expressionCache.remove(key);

        assertNull(expressionCache.get(key));
        assertFalse(expressionCache.containsKey(key));
    }

    @Test
    void testCacheClear() {
        ExpressionCacheKey key1 = new ExpressionCacheKey("expr1", java.util.Map.of());
        ExpressionCacheKey key2 = new ExpressionCacheKey("expr2", java.util.Map.of());

        expressionCache.put(key1, "result1");
        expressionCache.put(key2, "result2");

        expressionCache.clear();

        assertNull(expressionCache.get(key1));
        assertNull(expressionCache.get(key2));
    }

    @Test
    void testExpressionCacheWithUnderlyingCache() {
        InMemoryCache<ExpressionCacheKey, String> underlyingCache = new InMemoryCache<>();
        ExpressionCache<String> customCache = new ExpressionCache<>(underlyingCache);

        ExpressionCacheKey key = new ExpressionCacheKey("test", java.util.Map.of());
        customCache.put(key, "test_result");

        assertEquals("test_result", customCache.get(key));
        assertEquals("test_result", underlyingCache.get(key));
    }

    @Test
    void testNullHandling() {
        ExpressionCacheKey key = new ExpressionCacheKey("test", java.util.Map.of());
        assertThrows(IllegalArgumentException.class, () -> expressionCache.put(null, "value"));
        assertThrows(IllegalArgumentException.class, () -> expressionCache.put(key, null));
        assertThrows(IllegalArgumentException.class, () -> expressionCache.get(null));
        assertThrows(IllegalArgumentException.class, () -> expressionCache.containsKey(null));
        assertThrows(IllegalArgumentException.class, () -> expressionCache.remove(null));
    }

    @Test
    void testVariableOrderIndependence() {
        java.util.Map<String, Object> vars1 = new java.util.HashMap<>();
        vars1.put("a", 1);
        vars1.put("b", 2);

        java.util.Map<String, Object> vars2 = new java.util.HashMap<>();
        vars2.put("b", 2);
        vars2.put("a", 1);

        ExpressionCacheKey key1 = new ExpressionCacheKey("a + b", vars1);
        ExpressionCacheKey key2 = new ExpressionCacheKey("a + b", vars2);

        expressionCache.put(key1, "sum_result");
        assertEquals("sum_result", expressionCache.get(key2));
    }

    @Test
    void testComplexExpressionCaching() {
        ExpressionCacheKey key = new ExpressionCacheKey(
            "if player.level > 10: 'high' else: 'low'",
            java.util.Map.of("player.level", 15, "player.name", "test"));

        expressionCache.put(key, "high");
        assertEquals("high", expressionCache.get(key));
    }
}
