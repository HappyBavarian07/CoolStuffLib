package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.logging.ConfigLogger;
import org.junit.jupiter.api.BeforeAll;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigValueEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.AbstractBaseConfigModule;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.ListSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.MapSection;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedInMemoryConfigTest {
    @BeforeAll
    static void setupLogger() {
        if (!isLoggerInitialized()) {
            ConfigLogger.initialize(new java.io.File("target"));
        }
    }
    private static boolean isLoggerInitialized() {
        try {
            ConfigLogger.getLogger();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }
    @Test
    void testBasicCrudOperations() {
        ConfigLogger.info("Running testBasicCrudOperations", "AdvancedInMemoryConfigTest", true);
        AdvancedConfig config = new AdvancedInMemoryConfig("test");
        assertEquals("test", config.getName());
        assertNull(config.get("key1"));
        config.set("key1", "val1");
        assertEquals("val1", config.get("key1"));
        assertTrue(config.containsKey("key1"));
        config.remove("key1");
        assertFalse(config.containsKey("key1"));
    }

    @Test
    void testModuleRegistration() {
        ConfigLogger.info("Running testModuleRegistration", "AdvancedInMemoryConfigTest", true);
        AdvancedConfig config = new AdvancedInMemoryConfig("test");
        DummyModule module = new DummyModule();
        config.registerModule(module);
        assertTrue(config.getModules().containsValue(module));
        config.unregisterModule(module.getName());
        assertFalse(config.getModules().containsValue(module));
    }

    @Test
    void testInMemoryLogic() {
        ConfigLogger.info("Running testInMemoryLogic", "AdvancedInMemoryConfigTest", true);
        AdvancedConfig config = new AdvancedInMemoryConfig("test");
        config.set("a", 1);
        config.set("b", 2);
        assertEquals(2, config.getKeys(true).size());
        config.remove("a");
        assertEquals(1, config.getKeys(true).size());
        config.set("b", null);
        assertNull(config.get("b"));
    }

    @Test
    void testSectionOperations() {
        ConfigLogger.info("Running testSectionOperations", "AdvancedInMemoryConfigTest", true);
        AdvancedConfig config = new AdvancedInMemoryConfig("test");

        ConfigSection section = config.createSection("auth");
        section.set("enabled", true);
        section.set("timeout", 3600);

        // Verify section created correctly
        assertTrue(config.hasSection("auth"));
        assertEquals(true, config.getBoolean("auth.enabled"));
        assertEquals(3600, config.getInt("auth.timeout"));

        // Test nested section
        ConfigSection nested = section.createSection("providers");
        nested.set("local", true);
        nested.set("oauth", false);

        assertTrue(config.hasSection("auth.providers"));
        assertEquals(true, config.getBoolean("auth.providers.local"));

        // Test section retrieval
        ConfigSection retrieved = config.getSection("auth");
        assertNotNull(retrieved);
        assertEquals(true, retrieved.getBoolean("enabled"));

        // Test section removal
        config.removeSection("auth.providers");
        assertTrue(config.hasSection("auth"));
        assertFalse(config.hasSection("auth.providers"));

        config.removeSection("auth");
        assertFalse(config.hasSection("auth"));
    }

    @Test
    void testSpecializedSectionTypes() {
        ConfigLogger.info("Running testSpecializedSectionTypes", "AdvancedInMemoryConfigTest", true);
        AdvancedConfig config = new AdvancedInMemoryConfig("test");

        // Test ListSection
        ListSection listSection = config.createCustomSection("tags", ListSection.class);
        listSection.add("important");
        listSection.add("urgent");
        listSection.add("review");

        assertEquals(3, listSection.size());
        assertEquals("important", listSection.get(0));
        assertEquals("urgent", listSection.get(1));
        assertEquals("review", listSection.get(2));

        listSection.remove(1);
        assertEquals(2, listSection.size());
        assertEquals("review", listSection.get(1));

        // Test MapSection
        MapSection mapSection = config.createCustomSection("metadata", MapSection.class);
        mapSection.put("creator", "system");
        mapSection.put("version", 2);
        mapSection.put("timestamp", System.currentTimeMillis());

        assertEquals("system", mapSection.getValue("creator", String.class));
        assertEquals(2, mapSection.getValue("version", Integer.class));
        assertTrue(mapSection.getValue("timestamp", Long.class) > 0);

        mapSection.removeValue("creator");
        assertFalse(mapSection.containsKey("creator"));
        assertEquals(2, mapSection.size());
    }

    @Test
    void testEventSystem() {
        ConfigLogger.info("Running testEventSystem", "AdvancedInMemoryConfigTest", true);
        AdvancedConfig config = new AdvancedInMemoryConfig("test");
        AtomicBoolean valueChangeEventFired = new AtomicBoolean(false);
        final ConfigValueEvent[] lastEvent = new ConfigValueEvent[1];

        // Register event listener
        config.getEventBus().subscribe(ConfigValueEvent.class, event -> {
            lastEvent[0] = event;
            if (event.getFullPath().equals("testKey") &&
                    event.getNewValue().equals("testValue")) {
                valueChangeEventFired.set(true);
            }
        });

        // Trigger event
        config.set("testKey", "testValue");

        if (!valueChangeEventFired.get()) {
            String eventDetails = lastEvent[0] == null ? "No event received" :
                ("getFullPath=" + lastEvent[0].getFullPath() + ", getNewValue=" + lastEvent[0].getNewValue());
            throw new AssertionError("Event not fired. Debug: " + eventDetails + ", valueChangeEventFired=" + valueChangeEventFired.get());
        }

        // Verify event fired
        assertTrue(valueChangeEventFired.get());
    }

    @Test
    void testNestedStructure() {
        ConfigLogger.info("Running testNestedStructure", "AdvancedInMemoryConfigTest", true);
        AdvancedConfig config = new AdvancedInMemoryConfig("test");

        // Build complex structure
        ConfigSection app = config.createSection("app");
        app.set("name", "TestApp");
        app.set("version", "1.0.0");

        ConfigSection features = app.createSection("features");
        features.set("analytics", true);
        features.set("darkMode", true);

        ConfigSection users = app.createSection("users");
        ConfigSection admin = users.createSection("admin");
        admin.set("username", "admin");
        admin.set("fullAccess", true);

        ListSection adminPermissions = config.createCustomSection("app.users.admin.permissions", ListSection.class);
        adminPermissions.add("READ");
        adminPermissions.add("WRITE");
        adminPermissions.add("DELETE");

        // Verify structure
        assertEquals("TestApp", config.getString("app.name"));
        assertEquals("1.0.0", config.getString("app.version"));
        assertEquals(true, config.getBoolean("app.features.analytics"));
        assertEquals(true, config.getBoolean("app.features.darkMode"));
        assertEquals("admin", config.getString("app.users.admin.username"));
        assertEquals(true, config.getBoolean("app.users.admin.fullAccess"));

        // Verify paths
        Set<String> keys = config.getRootSection().getKeys(true);
        assertTrue(keys.contains("app.name"));
        assertTrue(keys.contains("app.version"));
        assertTrue(keys.contains("app.features.analytics"));
        assertTrue(keys.contains("app.users.admin.username"));

        // Test clear
        config.clear();
        keys = config.getRootSection().getKeys(true);
        assertTrue(keys.isEmpty());
    }

    @Test
    void testOptionalValues() {
        ConfigLogger.info("Running testOptionalValues", "AdvancedInMemoryConfigTest", true);
        AdvancedConfig config = new AdvancedInMemoryConfig("test");

        config.set("existing", "value");

        // Test optional values
        Optional<String> existingValue = config.getOptionalValue("existing", String.class);
        Optional<String> missingValue = config.getOptionalValue("nonexistent", String.class);

        assertTrue(existingValue.isPresent());
        assertEquals("value", existingValue.get());
        assertFalse(missingValue.isPresent());
    }

    @Test
    void testTypedValuesAndDefaults() {
        ConfigLogger.info("Running testTypedValuesAndDefaults", "AdvancedInMemoryConfigTest", true);
        AdvancedConfig config = new AdvancedInMemoryConfig("test");

        // Set values of different types
        config.set("string", "hello");
        config.set("integer", 42);
        config.set("double", 3.14);
        config.set("boolean", true);
        config.set("list", Arrays.asList("a", "b", "c"));

        // Test type-specific getters
        assertEquals("hello", config.getString("string"));
        assertEquals(42, config.getInt("integer"));
        assertEquals(3.14, config.getDouble("double"), 0.001);
        assertTrue(config.getBoolean("boolean"));

        // Test default values
        assertEquals("default", config.getString("nonexistent", "default"));
        assertEquals(100, config.getInt("nonexistent", 100));
        assertEquals(2.5, config.getDouble("nonexistent", 2.5), 0.001);
        assertFalse(config.getBoolean("nonexistent", false));
        // Test list retrieval
        ConfigLogger.info("Config Keys: " + config.getKeys(true), "AdvancedInMemoryConfigTest", true);
        ConfigLogger.info("Config Sections: " + config.getRootSection().getSubSections(), "AdvancedInMemoryConfigTest", true);
        List<?> retrievedList = config.getList("list");
        ConfigLogger.info("Retrieved list: " + retrievedList, "AdvancedInMemoryConfigTest", true);
        assertNotNull(retrievedList);
        assertEquals(3, retrievedList.size());
        assertEquals("a", retrievedList.get(0));

        // Test empty list default
        List<String> emptyDefault = new ArrayList<>();
        List<?> emptyList = config.getList("nonexistent", emptyDefault);
        assertTrue(emptyList.isEmpty());
    }

    @Test
    void testCopyAndMerge() {
        ConfigLogger.info("Running testCopyAndMerge", "AdvancedInMemoryConfigTest", true);
        AdvancedConfig config1 = new AdvancedInMemoryConfig("config1");
        config1.set("key1", "value1");
        config1.set("key2", "value2");
        config1.createSection("section1").set("nested", "nestedValue");

        AdvancedConfig config2 = new AdvancedInMemoryConfig("config2");
        config2.set("key2", "newValue2");  // Should override
        config2.set("key3", "value3");     // Should be added
        config2.createSection("section2").set("nested2", "nestedValue2");

        // Copy from config2 to config1
        config1.copyFrom(config2);

        System.out.println("Config1 after copy: " + config1.getRootSection().toMap());
        System.out.println("Config2 after copy: " + config2.getRootSection().toMap());
        System.out.println("Config1 keys: " + config1.getKeys(true));
        System.out.println("Config2 keys: " + config2.getKeys(true));

        // Check values after copy
        assertEquals("value1", config1.getString("key1"));     // Original kept
        assertEquals("newValue2", config1.getString("key2"));  // Overridden
        assertEquals("value3", config1.getString("key3"));     // Added
        assertEquals("nestedValue", config1.getString("section1.nested"));  // Original section kept
        assertEquals("nestedValue2", config1.getString("section2.nested2"));  // New section added
    }

    @Test
    void testCommentSupport() {
        ConfigLogger.info("Running testCommentSupport", "AdvancedInMemoryConfigTest", true);
        AdvancedConfig config = new AdvancedInMemoryConfig("test");

        // Set comments on keys and sections
        config.setComment("key1", "This is the first key");
        config.set("key1", "value1");

        config.setComment("nested", "This is a nested section");
        ConfigSection section = config.createSection("nested");
        section.set("nestedKey", "nestedValue");

        // Verify comments were set
        assertEquals("This is the first key", config.getComment("key1"));
        assertEquals("This is a nested section", config.getComment("nested"));

        // Update a comment
        config.setComment("key1", "Updated comment");
        assertEquals("Updated comment", config.getComment("key1"));

        // Remove a comment
        config.removeComment("key1");
        assertNull(config.getComment("key1"));
    }

    @Test
    void testConfigMetadata() {
        ConfigLogger.info("Running testConfigMetadata", "AdvancedInMemoryConfigTest", true);
        AdvancedConfig config = new AdvancedInMemoryConfig("test");

        // Set metadata
        config.addMetadata("created", System.currentTimeMillis());
        config.addMetadata("version", "1.0.0");
        config.addMetadata("encrypted", false);

        // Verify metadata
        assertTrue(config.hasMetadata("created"));
        assertEquals("1.0.0", config.getMetadata("version"));
        assertFalse((Boolean) config.getMetadata("encrypted"));

        // Make sure metadata doesn't affect values
        assertFalse(config.containsKey("created"));
        assertFalse(config.containsKey("version"));

        // Get all metadata
        Map<String, Object> allMetadata = config.getMetadata();
        assertEquals(3, allMetadata.size());

        // Remove metadata
        config.removeMetadata("version");
        assertFalse(config.hasMetadata("version"));
    }

    static class DummyModule extends AbstractBaseConfigModule {
        public DummyModule() {
            super("DummyModule",
                    "A dummy module for testing purposes",
                    "1.0.0");
        }

        @Override
        public String getName() {
            return "dummy";
        }

        @Override
        protected void onInitialize() {

        }

        @Override
        protected void onEnable() {

        }

        @Override
        protected void onDisable() {

        }

        @Override
        protected void onCleanup() {

        }

        @Override
        public AdvancedConfig getConfig() {
            return config;
        }

        @Override
        protected Map<String, Object> getAdditionalModuleState() {
            return Map.of();
        }
    }
}
