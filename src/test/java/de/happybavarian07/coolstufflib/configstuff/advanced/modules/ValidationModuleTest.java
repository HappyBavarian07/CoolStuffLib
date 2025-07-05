package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.AdvancedInMemoryConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.BaseConfigModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidationModuleTest {

    private ValidationModule module;
    private AdvancedInMemoryConfig config;

    @BeforeEach
    void setUp() {
        module = new ValidationModule();
        config = new AdvancedInMemoryConfig("testConfig");

        module.initialize(config);
        module.enable();
    }

    @Test
    void testModuleInitialization() {
        assertEquals("ValidationModule", module.getName());
        assertNotNull(module.getDescription());
        assertNotNull(module.getVersion());
        assertEquals(BaseConfigModule.ModuleState.ENABLED, module.getState());
    }

    @Test
    void testTypedStringValidation() {
        module.addValidator("username", String.class, new ValidationModule.Validator<String>() {
            @Override
            public boolean validate(String value) {
                return value.length() >= 3 && value.length() <= 20;
            }

            @Override
            public String getErrorMessage() {
                return "Username must be between 3 and 20 characters";
            }
        });

        config.set("username", "john");
        assertTrue(module.validateValue("username", "john"));
        assertFalse(module.validateValue("username", "ab"));
        assertFalse(module.validateValue("username", "thisnameistoolongtobevalid"));
    }

    @Test
    void testTypedNumericValidation() {
        module.addValidator("port", Integer.class, value ->
            value >= 1024 && value <= 65535);

        assertTrue(module.validateValue("port", 8080));
        assertFalse(module.validateValue("port", 80));
        assertFalse(module.validateValue("port", 70000));
    }

    @Test
    void testPatternValidation() {
        module.addPatternValidator("email", "^[\\w\\.-]+@[\\w\\.-]+\\.[a-zA-Z]{2,}$");

        assertTrue(module.validateValue("email", "user@example.com"));
        assertTrue(module.validateValue("email", "test.user@domain.org"));
        assertFalse(module.validateValue("email", "invalid-email"));
        assertFalse(module.validateValue("email", "user@"));
    }

    @Test
    void testPatternValidationWithCustomMessage() {
        module.addPatternValidator("phone", "\\d{10}", "Phone number must be exactly 10 digits");

        assertTrue(module.validateValue("phone", "1234567890"));
        assertFalse(module.validateValue("phone", "123456789"));
        assertEquals("Phone number must be exactly 10 digits", module.getError("phone"));
    }

    @Test
    void testRangeValidation() {
        module.addRule("percentage", new ValidationModule.RangeValidationRule(0, 100));

        assertTrue(module.validateValue("percentage", 50));
        assertTrue(module.validateValue("percentage", 0));
        assertTrue(module.validateValue("percentage", 100));
        assertFalse(module.validateValue("percentage", -1));
        assertFalse(module.validateValue("percentage", 101));
    }

    @Test
    void testTypeValidation() {
        module.addRule("count", new ValidationModule.TypeValidationRule(Integer.class));

        assertTrue(module.validateValue("count", 42));
        assertFalse(module.validateValue("count", "not a number"));
        assertFalse(module.validateValue("count", 3.14));
    }

    @Test
    void testValidationErrors() {
        module.addValidator("password", String.class, new ValidationModule.Validator<String>() {
            @Override
            public boolean validate(String value) {
                return value.length() >= 8;
            }

            @Override
            public String getErrorMessage() {
                return "Password must be at least 8 characters";
            }
        });

        assertFalse(module.validateValue("password", "short"));
        assertTrue(module.hasError("password"));
        assertEquals("Password must be at least 8 characters", module.getError("password"));

        assertTrue(module.validateValue("password", "validpassword"));
        assertFalse(module.hasError("password"));
    }

    @Test
    void testMultipleValidators() {
        module.addValidator("username", String.class, value -> value.length() >= 3);
        module.addPatternValidator("username", "^[a-zA-Z0-9_]+$");

        assertTrue(module.validateValue("username", "john_doe"));
        assertFalse(module.validateValue("username", "ab"));
        assertFalse(module.validateValue("username", "john-doe"));
    }

    @Test
    void testValidateAllValues() {
        module.addValidator("name", String.class, value -> !value.isEmpty());
        module.addValidator("age", Integer.class, value -> value > 0 && value < 150);

        config.set("name", "John");
        config.set("age", 25);
        config.set("invalid", "");

        module.addValidator("invalid", String.class, value -> !value.isEmpty());
        config.set("invalid", "foo");
        module.getAllRules();

        module.validateAllValues();
        assertFalse(module.hasErrors());
        config.set("invalid", "");
        module.validateAllValues();
        assertTrue(module.hasErrors());
    }

    @Test
    void testRemoveValidator() {
        module.addValidator("test", String.class, value -> false);
        assertFalse(module.validateValue("test", "anything"));

        module.removeRule("test");
        assertTrue(module.validateValue("test", "anything"));
    }

    @Test
    void testCustomValidatorWithLambda() {
        module.addValidator("score", Double.class, score -> score >= 0.0 && score <= 100.0);

        assertTrue(module.validateValue("score", 85.5));
        assertFalse(module.validateValue("score", -5.0));
        assertFalse(module.validateValue("score", 105.0));
    }

    @Test
    void testTypeValidationFailure() {
        module.addValidator("count", Integer.class, value -> value > 0);

        assertFalse(module.validateValue("count", "not_an_integer"));
        assertTrue(module.hasError("count"));
        assertTrue(module.getError("count").contains("Expected type Integer"));
    }

    @Test
    void testModuleLifecycle() {
        assertEquals(BaseConfigModule.ModuleState.ENABLED, module.getState());

        module.disable();
        assertEquals(BaseConfigModule.ModuleState.DISABLED, module.getState());

        module.enable();
        assertEquals(BaseConfigModule.ModuleState.ENABLED, module.getState());

        module.disable();
        module.cleanup();
        assertEquals(BaseConfigModule.ModuleState.UNINITIALIZED, module.getState());
    }

    @Test
    void testValidationRuleManagement() {
        assertFalse(module.hasRule("test"));

        module.addValidator("test", String.class, value -> true);
        assertTrue(module.hasRule("test"));

        module.removeRule("test");
        assertFalse(module.hasRule("test"));
    }
}
