package de.happybavarian07.coolstufflib.configstuff.advanced.migration;

import de.happybavarian07.coolstufflib.configstuff.advanced.AdvancedInMemoryConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.AdvancedPersistentConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.BaseAdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigFileType;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers.PropertiesConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.ListSection;
import de.happybavarian07.coolstufflib.logging.ConfigLogger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


class ConfigMigrationUtilityTest {

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
    static final Path tempDir = Path.of("TestOutputs/ConfigMigrationUtilityTest");

    @Test
    void testMigrateFromLegacyAdvanced() {
        ConfigLogger.info("Starting testMigrateFromLegacyAdvanced", "ConfigMigrationUtilityTest", true);
        ConfigLogger.initialize(tempDir.toFile());
        MockLegacyAdvancedConfig legacyConfig = new MockLegacyAdvancedConfig();
        legacyConfig.set("server.host", "localhost");
        legacyConfig.set("server.port", 8080);
        legacyConfig.set("database.url", "jdbc:mysql://localhost:3306/mydb");
        legacyConfig.set("database.username", "user");
        legacyConfig.set("database.password", "pass");
        legacyConfig.set("app.name", "TestApp");
        legacyConfig.set("app.version", "1.0.0");
        AdvancedConfig newConfig = new AdvancedInMemoryConfig("migrated");
        ConfigMigrationUtility.migrateFromLegacyAdvanced(legacyConfig, newConfig, new MigrationContext());
        ConfigLogger.info("All keys in new config: " + newConfig.getKeys(true), "ConfigMigrationUtilityTest", true);
        ConfigLogger.info("All sections in new config: " + newConfig.getRootSection().toMap(), "ConfigMigrationUtilityTest", true);
        assertTrue(newConfig.hasSection("server"));
        assertTrue(newConfig.hasSection("database"));
        assertTrue(newConfig.hasSection("app"));
        assertEquals("localhost", newConfig.getString("server.host"));
        assertEquals(8080, newConfig.getInt("server.port"));
        assertEquals("jdbc:mysql://localhost:3306/mydb", newConfig.getString("database.url"));
        assertEquals("user", newConfig.getString("database.username"));
        assertEquals("pass", newConfig.getString("database.password"));
        assertEquals("TestApp", newConfig.getString("app.name"));
        assertEquals("1.0.0", newConfig.getString("app.version"));
        ConfigLogger.info("testMigrateFromLegacyAdvanced completed", "ConfigMigrationUtilityTest", true);
    }

    @Test
    void testDetectAndConvertCollections() {
        ConfigLogger.info("Starting testDetectAndConvertCollections", "ConfigMigrationUtilityTest", true);
        AdvancedConfig config = new AdvancedInMemoryConfig("test");
        config.set("users.0", "Alice");
        config.set("users.1", "Bob");
        config.set("users.2", "Charlie");
        config.set("scores.0", 95);
        config.set("scores.1", 87);
        config.set("scores.2", 92);
        ConfigLogger.info("Before conversion: " + config.getKeys(true), "ConfigMigrationUtilityTest", true);
        ConfigMigrationUtility.detectAndConvertCollections(config);
        ConfigLogger.info("After conversion: " + config.getKeys(true), "ConfigMigrationUtilityTest", true);
        assertTrue(config.hasSection("users"));
        assertTrue(config.hasSection("scores"));
        assertInstanceOf(ListSection.class, config.getSection("users"));
        assertInstanceOf(ListSection.class, config.getSection("scores"));
        List<?> users = config.getList("users");
        assertNotNull(users);
        assertEquals(3, users.size());
        assertEquals("Alice", users.get(0));
        assertEquals("Bob", users.get(1));
        assertEquals("Charlie", users.get(2));
        List<?> scores = config.getList("scores");
        assertNotNull(scores);
        assertEquals(3, scores.size());
        assertEquals(95, scores.get(0));
        assertEquals(87, scores.get(1));
        assertEquals(92, scores.get(2));
        ConfigLogger.info("testDetectAndConvertCollections completed", "ConfigMigrationUtilityTest", true);
    }

    @Test
    void testMigrationWithNestedStructures() {
        ConfigLogger.info("Starting testMigrationWithNestedStructures", "ConfigMigrationUtilityTest", true);
        File file = tempDir.resolve("testMigration.properties").toFile();
        MockLegacyAdvancedConfig legacyConfig = new MockLegacyAdvancedConfig();
        legacyConfig.set("app.settings.theme", "dark");
        legacyConfig.set("app.settings.notifications", true);
        legacyConfig.set("app.users.admin.username", "admin");
        legacyConfig.set("app.users.admin.permissions", "ALL");
        legacyConfig.set("app.users.guest.username", "guest");
        legacyConfig.set("app.users.guest.permissions", "READ");
        ConfigLogger.info("File name: " + file.getName(), "ConfigMigrationUtilityTest", true);
        ConfigLogger.info("File absolute path: " + file.getAbsolutePath(), "ConfigMigrationUtilityTest", true);
        PropertiesConfigFileHandler handler = new PropertiesConfigFileHandler();
        boolean canHandle = handler.canHandle(file);
        ConfigLogger.info("Handler canHandle: " + canHandle, "ConfigMigrationUtilityTest", true);
        Assertions.assertTrue(file.getName().matches(".*\\.(properties|props)$"), "File name does not match expected pattern");
        Assertions.assertTrue(canHandle, "Handler cannot handle the file: " + file.getName());
        AdvancedConfig newConfig = new AdvancedPersistentConfig("migrated", file, ConfigFileType.PROPERTIES);
        ConfigMigrationUtility.migrateFromLegacyAdvanced(legacyConfig, newConfig, new MigrationContext());
        ConfigLogger.info("All keys in new config: " + newConfig.getKeys(true), "ConfigMigrationUtilityTest", true);
        ConfigLogger.info("All sections in new config: " + newConfig.getRootSection().toMap(), "ConfigMigrationUtilityTest", true);
        Assertions.assertTrue(newConfig.hasSection("app"));
        Assertions.assertTrue(newConfig.hasSection("app.settings"));
        Assertions.assertTrue(newConfig.hasSection("app.users"));
        Assertions.assertTrue(newConfig.hasSection("app.users.admin"));
        Assertions.assertTrue(newConfig.hasSection("app.users.guest"));
        Assertions.assertEquals("dark", newConfig.getString("app.settings.theme"));
        Assertions.assertTrue(newConfig.getBoolean("app.settings.notifications"));
        Assertions.assertEquals("admin", newConfig.getString("app.users.admin.username"));
        Assertions.assertEquals("ALL", newConfig.getString("app.users.admin.permissions"));
        Assertions.assertEquals("guest", newConfig.getString("app.users.guest.username"));
        Assertions.assertEquals("READ", newConfig.getString("app.users.guest.permissions"));
        newConfig.save();
        AdvancedConfig reloaded = new AdvancedPersistentConfig("reloaded", file, ConfigFileType.PROPERTIES);
        ConfigLogger.info("All keys in reloaded config: " + reloaded.getKeys(true), "ConfigMigrationUtilityTest", true);
        assertTrue(reloaded.hasSection("app.settings"));
        assertEquals("dark", reloaded.getString("app.settings.theme"));
        ConfigLogger.info("testMigrationWithNestedStructures completed", "ConfigMigrationUtilityTest", true);
    }

    @Test
    void testMigrationWithMixedDataTypes() {
        ConfigLogger.info("Starting testMigrationWithMixedDataTypes", "ConfigMigrationUtilityTest", true);
        MockLegacyAdvancedConfig legacyConfig = new MockLegacyAdvancedConfig();
        legacyConfig.set("strings.welcome", "Hello");
        legacyConfig.set("numbers.counter", 42);
        legacyConfig.set("flags.enabled", true);
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", "John");
        userData.put("age", 30);
        legacyConfig.set("complexData", userData);
        AdvancedConfig newConfig = new AdvancedInMemoryConfig("test");
        ConfigLogger.info("Legacy config keys: " + legacyConfig.getKeys(true), "ConfigMigrationUtilityTest", true);
        ConfigLogger.info("Legacy config sections: " + legacyConfig.getRootSection().toMap(), "ConfigMigrationUtilityTest", true);
        ConfigMigrationUtility.migrateFromLegacyAdvanced(legacyConfig, newConfig, new MigrationContext());
        ConfigLogger.info("All keys in new config: " + newConfig.getKeys(true), "ConfigMigrationUtilityTest", true);
        ConfigLogger.info("All sections in new config: " + newConfig.getRootSection().toMap(), "ConfigMigrationUtilityTest", true);
        assertEquals("Hello", newConfig.getString("strings.welcome"));
        assertEquals(42, newConfig.getInt("numbers.counter"));
        assertTrue(newConfig.getBoolean("flags.enabled"));
        ConfigSection migratedData = newConfig.getSection("complexData");
        assertNotNull(migratedData);
        assertEquals("John", migratedData.get("name"));
        assertEquals(30, migratedData.get("age"));
        ConfigLogger.info("testMigrationWithMixedDataTypes completed", "ConfigMigrationUtilityTest", true);
    }

    @Test
    void testMigrateFromLegacyAdvancedWithContext() {
        ConfigLogger.info("Starting testMigrateFromLegacyAdvancedWithContext", "ConfigMigrationUtilityTest", true);
        MockLegacyAdvancedConfig legacyConfig = new MockLegacyAdvancedConfig();
        legacyConfig.set("server.host", "localhost");
        legacyConfig.set("server.port", 8080);
        legacyConfig.set("database.url", "jdbc:mysql://localhost:3306/mydb");
        legacyConfig.set("database.username", "user");
        legacyConfig.set("database.password", "pass");
        legacyConfig.set("app.name", "TestApp");
        legacyConfig.set("app.version", "1.0.0");
        AdvancedConfig newConfig = new AdvancedInMemoryConfig("migrated");
        MigrationContext context = new MigrationContext();
        context.setMetadata("testKey", "testValue");
        context.setLegacyCompatibilityEnabled(false);
        ConfigMigrationUtility.migrateFromLegacyAdvanced(legacyConfig, newConfig, context);
        assertTrue(newConfig.hasSection("server"));
        assertTrue(newConfig.hasSection("database"));
        assertTrue(newConfig.hasSection("app"));
        assertEquals("localhost", newConfig.getString("server.host"));
        assertEquals(8080, newConfig.getInt("server.port"));
        assertEquals("jdbc:mysql://localhost:3306/mydb", newConfig.getString("database.url"));
        assertEquals("user", newConfig.getString("database.username"));
        assertEquals("pass", newConfig.getString("database.password"));
        assertEquals("TestApp", newConfig.getString("app.name"));
        assertEquals("1.0.0", newConfig.getString("app.version"));
        Object migrationContextValue = newConfig.getMigrationContext().getMetadata("testKey");
        assertFalse(newConfig.getMigrationContext().isLegacyCompatibilityEnabled());
        assertEquals("testValue", migrationContextValue);
        ConfigLogger.info("testMigrateFromLegacyAdvancedWithContext completed", "ConfigMigrationUtilityTest", true);
    }

    private static class MockLegacyAdvancedConfig extends AdvancedPersistentConfig {
        protected MockLegacyAdvancedConfig() {
            super("mockLegacy", new File("mockLegacy.mem"), ConfigFileType.MEMORY);
        }

        @Override
        public void save() {
            // Mock save does nothing
        }

        @Override
        public void reload() {
            // Mock reload does nothing
        }
    }
}
