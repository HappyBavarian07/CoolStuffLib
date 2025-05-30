package de.happybavarian07.coolstufflib.languagemanager.expressionengine;
import de.happybavarian07.coolstufflib.languagemanager.expressionengine.exceptions.ExpressionSyntaxException;
import de.happybavarian07.coolstufflib.languagemanager.expressionengine.exceptions.ExpressionVariableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExpressionEngineTest {
    private ExpressionEngine engine;

    @BeforeEach
    public void setup() {
        engine = new ExpressionEngine();
        engine.setDebugMode(true);
    }
    @Test
    void testBasicMath() {
        assertEquals(7.0, engine.parse("1 + 2 * 3", Double.class));
    }

    @Test
    void testVariables() {
        engine.setVariable("x", 4);
        assertEquals(16.0, engine.parse("x * x", Double.class));
    }

    @Test
    void testFunction() {
        engine.registerFunction("double", (i, args, t) -> ((Number) args.get(0)).doubleValue() * 2,
            "double", new Class<?>[]{Double.class}, Double.class);
        engine.registerFunction("square", (i, args, t) -> ((Number) args.get(0)).doubleValue() * ((Number) args.get(0)).doubleValue(),
            "square", new Class<?>[]{Double.class}, Double.class);
        double result = 2;
        for (int i = 0; i < 10; i++) {
            double expected = result * 2;
            engine.setVariable("x", result);
            result = engine.parse("double(x)", Double.class);
            assertEquals(expected, result);
        }

        // do the same test with double and square
        result = 2;
        for (int i = 0; i < 10; i++) {
            double expected = 2 * (result * result);
            engine.setVariable("x", result);
            result = engine.parse("double(square(x))", Double.class);
            assertEquals(expected, result);
        }
    }

    @Test
    void testConditionalChain() {
        engine.setVariable("x", 9);
        String expr = "if x > 10: Out<String>(\"big\") elif x > 5: Out<String>(\"medium\") else: Out<String>(\"small\")";
        assertEquals("medium", engine.parse(expr, String.class));
        engine.setVariable("x", 4);
        assertEquals("small", engine.parse(expr, String.class));
        engine.setVariable("x", 11);
        assertEquals("big", engine.parse(expr, String.class));
    }

    @Test
    void testMaterialParsing() {
        assertNotNull(engine.parse("DIAMOND_BLOCK", org.bukkit.Material.class));
    }

    @Test
    void testTypeInference() {
        engine.registerFunction("boolTest", (i, a, t) -> a.get(0), "boolean", new Class<?>[]{Boolean.class}, Boolean.class);
        assertEquals(true, engine.parse("Out<boolean>(boolTest('true'))", Boolean.class));
    }

    @Test
    void testAssignmentAndUsageLimit() {
        // Basic assignment and usage
        assertEquals(5.0, engine.parse("let x = 5; x", Double.class));
        // Usage limit
        assertEquals(10.0, engine.parse("let y = 10 as 2; y + y", Double.class));
        // TODO somehow fix this stupid error that somehow is not throwing the exception even tho its called
        Exception ex = assertThrows(ExpressionVariableException.class, () -> engine.parse("let z = 3 as 1; z + z", Double.class));
        assertTrue(ex.getMessage().contains("exceeded its allowed uses"));
        // Nested assignment
        assertEquals(12.0, engine.parse("let a = (let b = 6 as 1; b * 2) as 2; a + a", Double.class));
        // Assignment with unlimited uses
        assertEquals(9.0, engine.parse("let k = 9 as -1; k + 0", Double.class));
        // Invalid assignment syntax
        Exception ex2 = assertThrows(ExpressionSyntaxException.class, () -> engine.parse("let = 5", Double.class));
        assertTrue(ex2.getMessage().contains("Expect variable name after 'let'"));
        Exception ex3 = assertThrows(ExpressionSyntaxException.class, () -> engine.parse("let m 5", Double.class));
        assertTrue(ex3.getMessage().contains("Expect '=' after variable name."));
        // Using undefined variable
        Exception ex4 = assertThrows(ExpressionVariableException.class, () -> engine.parse("foo", Double.class));
        assertTrue(ex4.getMessage().contains("Undefined variable"));
    }
}