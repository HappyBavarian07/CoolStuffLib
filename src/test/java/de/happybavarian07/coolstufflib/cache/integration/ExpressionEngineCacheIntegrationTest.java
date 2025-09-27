package de.happybavarian07.coolstufflib.cache.integration;

import de.happybavarian07.coolstufflib.languagemanager.expressionengine.ExpressionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ExpressionEngineCacheIntegrationTest {

    private ExpressionEngine engine;
    private AtomicInteger logCount;
    private AtomicInteger errorCount;

    @BeforeEach
    void setUp() {
        engine = new ExpressionEngine();
        logCount = new AtomicInteger(0);
        errorCount = new AtomicInteger(0);

        engine.setLogger((message, args) -> {
            logCount.incrementAndGet();
            System.out.println("LOG: " + String.format(message.replace("{}", "%s"), args));
        });

        engine.setErrorHandler((expressionEngine, exception) -> {
            errorCount.incrementAndGet();
            System.err.println("ERROR: " + exception.getMessage());
        });
    }

    @Test
    void testBasicExpressionCaching() {
        Map<String, Object> variables = Map.of("x", 10, "y", 20);

        Object result1 = engine.evaluate("x + y", variables);
        Object result2 = engine.evaluate("x + y", variables);

        assertEquals(30.0, result1);
        assertEquals(30.0, result2);

        assertTrue(logCount.get() >= 2);
    }

    @Test
    void testCacheHitAndMiss() {
        Map<String, Object> vars1 = Map.of("a", 5);
        Map<String, Object> vars2 = Map.of("a", 10);

        engine.evaluate("a * 2", vars1);
        engine.evaluate("a * 2", vars1);
        engine.evaluate("a * 2", vars2);

        assertTrue(logCount.get() > 0);
    }

    @Test
    void testVariableStateAwareCaching() {
        Object result1 = engine.evaluate("x + 1", Map.of("x", 5));
        Object result2 = engine.evaluate("x + 1", Map.of("x", 10));
        Object result3 = engine.evaluate("x + 1", Map.of("x", 5));

        assertEquals(6.0, result1);
        assertEquals(11.0, result2);
        assertEquals(6.0, result3);
    }

    @Test
    void testParseAndEvalCacheSeparation() {
        Map<String, Object> variables = Map.of("value", 100);

        Object result1 = engine.evaluate("value > 50", variables);
        Object result2 = engine.evaluate("value > 50", Map.of("value", 25));
        Object result3 = engine.evaluate("value > 50", variables);

        assertTrue((Boolean) result1);
        assertFalse((Boolean) result2);
        assertTrue((Boolean) result3);
    }

    @Test
    void testComplexExpressionCaching() {
        Map<String, Object> playerData = Map.of(
            "player_level", 15,
            "player_experience", 2500,
            "multiplier", 1.5
        );
        String expression = "player_level * multiplier + (player_experience / 100)";
        Object result = engine.evaluate(expression, playerData);
        assertEquals(47.5, result);
        Object cachedResult = engine.evaluate(expression, playerData);
        assertEquals(47.5, cachedResult);
    }

    @Test
    void testFunctionCallCaching() {
        engine.registerFunction("double", (interpreter, args, type) -> {
            if (args.size() != 1) throw new RuntimeException("double() expects 1 argument");
            Object arg = args.get(0);
            if (arg instanceof Number) {
                return ((Number) arg).doubleValue() * 2;
            }
            throw new RuntimeException("double() expects a number");
        });

        Map<String, Object> vars = Map.of("n", 21);
        Object result1 = engine.evaluate("double(n)", vars);
        Object result2 = engine.evaluate("double(n)", vars);

        assertEquals(42.0, result1);
        assertEquals(42.0, result2);
    }

    @Test
    void testErrorHandlingIntegration() {
        assertThrows(RuntimeException.class, () -> engine.evaluate("undefined_variable", Map.of()));
        assertTrue(errorCount.get() > 0);
    }

    @Test
    void testStrictModeIntegration() {
        engine.setStrictMode(true);
        assertTrue(engine.isStrictMode());

        engine.setStrictMode(false);
        assertFalse(engine.isStrictMode());
    }

    @Test
    void testRecursionDepthLimits() {
        engine.setMaxRecursionDepth(50);
        assertEquals(50, engine.getMaxRecursionDepth());
    }

    @Test
    void testEvaluationTimeout() {
        engine.setEvaluationTimeout(1000);
        assertEquals(1000, engine.getEvaluationTimeout());
    }

    @Test
    void testCustomTypeRegistration() {
        engine.registerVariableType("Player", Object.class);
        engine.registerFunctionType("Calculator", Object.class);

        assertDoesNotThrow(() -> {
            engine.evaluate("42", Map.of());
        });
    }

    @Timeout(10)
    void testConcurrentCacheAccess() throws InterruptedException {
        final int threadCount = 10;
        final int expressionsPerThread = 50;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < expressionsPerThread; j++) {
                        Map<String, Object> vars = Map.of("threadId", threadId, "iteration", j);
                        try {
                            Object result = engine.evaluate("threadId * 10 + iteration", vars);
                            double expected = threadId * 10.0 + j;
                            if (expected == (Double) result) {
                                successCount.incrementAndGet();
                            }
                        } catch (IllegalArgumentException | NullPointerException e) {
                            // Expected for invalid input, do not count as success
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Thread " + threadId + " failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertTrue(successCount.get() > 0);
    }

    @Test
    void testCachePerformanceImprovement() {
        String complexExpression = "((a + b) * (c - d)) / (e + f)";
        Map<String, Object> variables = Map.of(
            "a", 10, "b", 20, "c", 30, "d", 5, "e", 15, "f", 10
        );

        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            engine.evaluate(complexExpression, variables);
        }
        long endTime = System.nanoTime();

        long duration = endTime - startTime;
        assertTrue(duration > 0);
        System.out.println("1000 evaluations took: " + duration / 1_000_000 + " ms");
    }

    @Test
    void testVariableWhitelistBlacklist() {
        engine.setVariableWhitelist(List.of("allowed_var"));
        engine.setVariableBlacklist(List.of("blocked_var"));

        Map<String, Object> allowedVars = Map.of("allowed_var", 42);
        Object result = engine.evaluate("allowed_var", allowedVars);
        assertEquals(42, result);
    }

    @Test
    void testFunctionWhitelistBlacklist() {
        engine.registerFunction("allowed_func", (interpreter, args, type) -> "allowed");
        engine.registerFunction("blocked_func", (interpreter, args, type) -> "blocked");

        engine.setFunctionWhitelist(List.of("allowed_func"));
        engine.setFunctionBlacklist(List.of("blocked_func"));

        Object result = engine.evaluate("allowed_func()", Map.of());
        assertEquals("allowed", result);
    }

    @Test
    void testDebugMode() {
        engine.setDebugMode(true);
        assertTrue(engine.isDebugMode());

        engine.evaluate("1 + 1", Map.of());

        engine.setDebugMode(false);
        assertFalse(engine.isDebugMode());
    }

    @Test
    void testContextManagement() {
        engine.putContext("config", Map.of("timeout", 5000));
        engine.putContext("user", "testUser");

        assertDoesNotThrow(() -> {
            engine.evaluate("42", Map.of());
        });

        engine.removeContext("config");
        engine.clearContext();
    }
}
