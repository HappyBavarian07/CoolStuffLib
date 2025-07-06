package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigLifecycleEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigFileType;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.ListSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.MapSection;
import de.happybavarian07.coolstufflib.logging.ConfigLogger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedPersistentConfigTest {
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
    Path tempDir = Path.of("TestOutputs/AdvancedPersistentConfigTest");

    @Test
    void testBasicCrudOperations() {
        ConfigLogger.info("Running testBasicCrudOperations", "AdvancedPersistentConfigTest", true);
        File file = tempDir.resolve("testCrud.properties").toFile();
        AdvancedConfig config = new AdvancedPersistentConfig("test", file, ConfigFileType.PROPERTIES);
        assertEquals("test", config.getName());

        // Test basic value operations
        assertNull(config.get("key1"));
        config.set("key1", "val1");
        assertEquals("val1", config.get("key1"));
        assertTrue(config.containsKey("key1"));
        config.remove("key1");
        assertFalse(config.containsKey("key1"));
    }

    @Test
    void testFileIO() {
        ConfigLogger.info("Running testFileIO", "AdvancedPersistentConfigTest", true);
        File file = tempDir.resolve("testFileIO.properties").toFile();
        AdvancedConfig config = new AdvancedPersistentConfig("test", file, ConfigFileType.PROPERTIES);
        config.set("foo", "bar");
        config.save();

        AdvancedConfig loaded = new AdvancedPersistentConfig("test", file, ConfigFileType.PROPERTIES);
        assertEquals("bar", loaded.get("foo"));
    }

    @Test
    void testPersistence() {
        ConfigLogger.info("Running testPersistence", "AdvancedPersistentConfigTest", true);
        File file = tempDir.resolve("testPersistence.properties").toFile();
        AdvancedConfig config = new AdvancedPersistentConfig("test", file, ConfigFileType.PROPERTIES);
        config.set("persist", 42);
        config.save();

        AdvancedConfig loaded = new AdvancedPersistentConfig("test", file, ConfigFileType.PROPERTIES);
        assertEquals(42, loaded.getInt("persist"));
    }

    @Test
    void testSectionOperations() {
        ConfigLogger.info("Running testSectionOperations", "AdvancedPersistentConfigTest", true);
        File file = tempDir.resolve("testSections.properties").toFile();
        AdvancedConfig config = new AdvancedPersistentConfig("test", file, ConfigFileType.PROPERTIES);

        // Create a section
        ConfigSection section = config.createSection("database");
        section.set("url", "jdbc:mysql://localhost:3306/mydb");
        section.set("username", "user");
        section.set("password", "password");

        // Test direct section access
        assertTrue(config.hasSection("database"));
        assertEquals("jdbc:mysql://localhost:3306/mydb", config.getString("database.url"));
        assertEquals("user", config.getString("database.username"));
        assertEquals("password", config.getString("database.password"));

        // Test section retrieval
        ConfigSection retrieved = config.getSection("database");
        assertNotNull(retrieved);
        assertEquals("jdbc:mysql://localhost:3306/mydb", retrieved.getString("url"));

        // Test nested sections
        ConfigSection nestedSection = config.createSection("server.http");
        nestedSection.set("port", 8080);
        nestedSection.set("host", "localhost");

        assertEquals(8080, config.getInt("server.http.port"));

        // Test section removal
        config.removeSection("database");
        assertFalse(config.hasSection("database"));
    }

    @Test
    void testSpecializedSections() {
        ConfigLogger.info("Running testSpecializedSections", "AdvancedPersistentConfigTest", true);
        File file = tempDir.resolve("testSpecializedSections.properties").toFile();
        AdvancedConfig config = new AdvancedPersistentConfig("test", file, ConfigFileType.PROPERTIES);
        ListSection listSection = config.createCustomSection("permissions", ListSection.class);
        listSection.add("READ");
        listSection.add("WRITE");
        listSection.add("EXECUTE");
        ConfigLogger.info("Before save: permissions section type = " + listSection.getClass().getSimpleName(), "AdvancedPersistentConfigTest", true);
        config.save();
        AdvancedConfig reloaded = new AdvancedPersistentConfig("test", file, ConfigFileType.PROPERTIES);
        ConfigSection reloadedSection = reloaded.getSection("permissions");
        ConfigLogger.info("After load: permissions section type = " + (reloadedSection != null ? reloadedSection.getClass().getSimpleName() : "null"), "AdvancedPersistentConfigTest", true);
        assertInstanceOf(ListSection.class, reloadedSection);
        ListSection reloadedListSection = (ListSection) reloadedSection;
        assertEquals(3, reloadedListSection.size());
        assertEquals("READ", reloadedListSection.get(0));
        assertEquals("WRITE", reloadedListSection.get(1));
        assertEquals("EXECUTE", reloadedListSection.get(2));
        MapSection mapSection = config.createCustomSection("metadata", MapSection.class);
        mapSection.put("created", "today");
        mapSection.put("version", 1);
        mapSection.put("valid", true);
        ConfigLogger.info("Before save: metadata section type = " + mapSection.getClass().getSimpleName(), "AdvancedPersistentConfigTest", true);
        config.save();
        AdvancedConfig reloaded2 = new AdvancedPersistentConfig("test", file, ConfigFileType.PROPERTIES);
        ConfigSection reloadedSection2 = reloaded2.getSection("metadata");
        ConfigLogger.info("After load: metadata section type = " + (reloadedSection2 != null ? reloadedSection2.getClass().getSimpleName() : "null"), "AdvancedPersistentConfigTest", true);
        assertTrue(reloadedSection2 instanceof MapSection);
        MapSection reloadedMapSection = (MapSection) reloadedSection2;
        assertEquals("today", reloadedMapSection.getValue("created"));
        assertEquals(1, reloadedMapSection.getValue("version"));
        assertEquals(true, reloadedMapSection.getValue("valid"));
    }

    @Test
    void testTypedAccessors() {
        ConfigLogger.info("Running testTypedAccessors", "AdvancedPersistentConfigTest", true);
        File file = tempDir.resolve("testTyped.properties").toFile();
        AdvancedConfig config = new AdvancedPersistentConfig("test", file, ConfigFileType.PROPERTIES);

        // Set values of different types
        config.set("string", "text");
        config.set("integer", 42);
        config.set("boolean", true);
        config.set("double", 3.14);

        // Test type-specific accessors
        assertEquals("text", config.getString("string"));
        assertEquals(42, config.getInt("integer"));
        assertTrue(config.getBoolean("boolean"));
        assertEquals(3.14, config.getDouble("double"), 0.001);

        // Test default values
        assertEquals("default", config.getString("nonexistent", "default"));
        assertEquals(100, config.getInt("nonexistent", 100));
        assertFalse(config.getBoolean("nonexistent", false));

        // Test generic accessor
        assertEquals(Integer.valueOf(42), config.getValue("integer", Integer.class));
        assertEquals(Boolean.TRUE, config.getValue("boolean", Boolean.class));
    }

    @Test
    void testComplexStructurePersistence() {
        ConfigLogger.info("Running testComplexStructurePersistence", "AdvancedPersistentConfigTest", true);
        File file = tempDir.resolve("testComplex.properties").toFile();
        AdvancedConfig config = new AdvancedPersistentConfig("test", file, ConfigFileType.PROPERTIES);

        ConfigLogger.info("Creating users section", "AdvancedPersistentConfigTest", true);
        ConfigSection users = config.createSection("users");

        ConfigLogger.info("Creating user1 (alice)", "AdvancedPersistentConfigTest", true);
        ConfigSection user1 = users.createSection("alice");
        user1.set("name", "Alice");
        user1.set("age", 30);
        user1.set("active", true);

        ConfigLogger.info("Creating user2 (bob)", "AdvancedPersistentConfigTest", true);
        ConfigSection user2 = users.createSection("bob");
        user2.set("name", "Bob");
        user2.set("age", 25);
        user2.set("active", false);

        ConfigLogger.info("Creating roles list section for alice", "AdvancedPersistentConfigTest", true);
        ListSection aliceRoles = config.createCustomSection("users.alice.roles", ListSection.class);
        aliceRoles.add("admin");
        aliceRoles.add("user");

        ConfigLogger.info("Before save: users section type = " + users.getClass().getSimpleName(), "AdvancedPersistentConfigTest", true);

        config.save();
        ConfigLogger.info("Config saved to file: " + file.getAbsolutePath(), "AdvancedPersistentConfigTest", true);

        AdvancedConfig loaded = new AdvancedPersistentConfig("test", file, ConfigFileType.PROPERTIES);
        ConfigLogger.info("Config reloaded from file: " + file.getAbsolutePath(), "AdvancedPersistentConfigTest", true);

        ConfigLogger.info("Checking existence of users, users.alice, users.bob sections", "AdvancedPersistentConfigTest", true);
        assertTrue(loaded.hasSection("users"));
        assertTrue(loaded.hasSection("users.alice"));
        assertTrue(loaded.hasSection("users.bob"));

        ConfigLogger.info("Verifying alice's properties", "AdvancedPersistentConfigTest", true);
        assertEquals("Alice", loaded.getString("users.alice.name"));
        assertEquals(30, loaded.getInt("users.alice.age"));
        assertTrue(loaded.getBoolean("users.alice.active"));

        ConfigLogger.info("Verifying bob's properties", "AdvancedPersistentConfigTest", true);
        assertEquals("Bob", loaded.getString("users.bob.name"));
        assertEquals(25, loaded.getInt("users.bob.age"));
        assertFalse(loaded.getBoolean("users.bob.active"));

        ConfigLogger.info("Loaded users: " + loaded.getSection("users").getKeys(true), "AdvancedPersistentConfigTest", true);
        ConfigLogger.info("Loaded alice roles: " + loaded.getSection("users.alice.roles").getKeys(true), "AdvancedPersistentConfigTest", true);

        List<?> roles = loaded.getList("users.alice.roles");
        ConfigLogger.info("Loaded roles for alice: " + roles, "AdvancedPersistentConfigTest", true);
        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertEquals("admin", roles.get(0));
        assertEquals("user", roles.get(1));
    }

    @Test
    void testFileTypeAutoDetection() {
        ConfigLogger.info("Running testFileTypeAutoDetection", "AdvancedPersistentConfigTest", true);
        // Create configs with different file extensions
        File propertiesFile = tempDir.resolve("config.properties").toFile();
        File yamlFile = tempDir.resolve("config.yml").toFile();
        File jsonFile = tempDir.resolve("config.json").toFile();

        // Create configs with auto-detection
        AdvancedConfig propertiesConfig = new AdvancedPersistentConfig("properties", propertiesFile, ConfigFileType.PROPERTIES);
        AdvancedConfig yamlConfig = new AdvancedPersistentConfig("yaml", yamlFile, ConfigFileType.YAML);
        AdvancedConfig jsonConfig = new AdvancedPersistentConfig("json", jsonFile, ConfigFileType.JSON);

        // Set values
        propertiesConfig.set("type", "properties");
        yamlConfig.set("type", "yaml");
        jsonConfig.set("type", "json");

        // Save all
        propertiesConfig.save();
        yamlConfig.save();
        jsonConfig.save();

        // Verify files were created
        assertTrue(propertiesFile.exists());
        assertTrue(yamlFile.exists());
        assertTrue(jsonFile.exists());

        // Load and verify values
        AdvancedConfig loadedProperties = new AdvancedPersistentConfig("properties", propertiesFile, ConfigFileType.PROPERTIES);
        AdvancedConfig loadedYaml = new AdvancedPersistentConfig("yaml", yamlFile, ConfigFileType.YAML);
        AdvancedConfig loadedJson = new AdvancedPersistentConfig("json", jsonFile, ConfigFileType.JSON);

        assertEquals("properties", loadedProperties.getString("type"));
        assertEquals("yaml", loadedYaml.getString("type"));
        assertEquals("json", loadedJson.getString("type"));
    }

    @Test
    void testReload() {
        ConfigLogger.info("Running testReload", "AdvancedPersistentConfigTest", true);
        File file = tempDir.resolve("reloadTest.properties").toFile();

        // Create and save initial config
        AdvancedConfig config1 = new AdvancedPersistentConfig("test", file, ConfigFileType.PROPERTIES);
        config1.set("key1", "value1");
        config1.set("key2", "value2");
        config1.save();

        // Create a second instance pointing to the same file
        AdvancedConfig config2 = new AdvancedPersistentConfig("test", file, ConfigFileType.PROPERTIES);

        // Modify first config and save
        config1.set("key1", "modified");
        config1.set("key3", "new");
        config1.save();

        // Reload second config and verify it sees the changes
        config2.reload();
        assertEquals("modified", config2.getString("key1"));
        assertEquals("value2", config2.getString("key2"));
        assertEquals("new", config2.getString("key3"));
    }

    @Test
    void testFileBackedEvents() {
        ConfigLogger.info("Running testFileBackedEvents", "AdvancedPersistentConfigTest", true);
        File file = tempDir.resolve("events.properties").toFile();
        AdvancedConfig config = new AdvancedPersistentConfig("test", file, ConfigFileType.PROPERTIES);

        AtomicBoolean saveEventFired = new AtomicBoolean(false);
        AtomicBoolean loadEventFired = new AtomicBoolean(false);

        // Register event listeners
        config.getEventBus().subscribe(ConfigLifecycleEvent.class, event -> {
            if (event.getType() == ConfigLifecycleEvent.Type.SAVE) {
                saveEventFired.set(true);
            } else if (event.getType() == ConfigLifecycleEvent.Type.RELOAD) {
                loadEventFired.set(true);
            }
        });

        // Trigger events
        config.set("test", "value");
        config.save();
        assertTrue(saveEventFired.get());

        config.reload();
        assertTrue(loadEventFired.get());
    }

    @Test
    void testNestedStructurePersistence() {
        ConfigLogger.info("Running testNestedStructurePersistence", "AdvancedPersistentConfigTest", true);
        File file = tempDir.resolve("nested.json").toFile();
        AdvancedConfig config = new AdvancedPersistentConfig("test", file, ConfigFileType.JSON);

        // Create complex nested structure
        ConfigSection root = config.createSection("app");
        root.set("name", "TestApp");
        root.set("version", "1.0.0");

        ConfigSection database = root.createSection("database");
        database.set("host", "localhost");
        database.set("port", 3306);
        database.set("credentials", Map.of("user", "admin", "password", "secret"));

        ConfigSection features = root.createSection("features");
        ListSection enabledFeatures = config.createCustomSection("app.features.enabled", ListSection.class);
        enabledFeatures.addAll(Arrays.asList("search", "notifications", "sharing"));

        // Save config
        config.save();

        // Load in a new instance
        AdvancedConfig loaded = new AdvancedPersistentConfig("test", file, ConfigFileType.JSON);

        // Verify structure was preserved
        assertEquals("TestApp", loaded.getString("app.name"));
        assertEquals("1.0.0", loaded.getString("app.version"));
        assertEquals("localhost", loaded.getString("app.database.host"));
        assertEquals(3306, loaded.getInt("app.database.port"));

        // Verify complex types were preserved
        assertTrue(loaded.hasSection("app.database.credentials"));
        System.out.println("All Keys: " + loaded.getKeys(true));
        System.out.println("Credentials Keys: " + loaded.getSection("app.database.credentials").getKeys(true));
        ConfigSection credentials = loaded.getSection("app.database.credentials");
        assertEquals("admin", credentials.get("user"));
        assertEquals("secret", credentials.get("password"));

        // Verify list was preserved
        ConfigSection loadedFeatures = loaded.getSection("app.features");
        assertTrue(loadedFeatures.hasSection("enabled"));
        ListSection loadedEnabledFeatures = (ListSection) loadedFeatures.getSection("enabled");
        assertEquals(3, loadedEnabledFeatures.size());
        assertEquals("search", loadedEnabledFeatures.get(0));
        assertEquals("notifications", loadedEnabledFeatures.get(1));
        assertEquals("sharing", loadedEnabledFeatures.get(2));
    }

    @Test
    void testCorruptedFileHandling() throws IOException {
        ConfigLogger.info("Running testCorruptedFileHandling", "AdvancedPersistentConfigTest", true);
        File file = tempDir.resolve("corrupted.json").toFile();

        // Write invalid content to file
        Files.writeString(file.toPath(), "{this is not valid json}");

        // Try to load the corrupted file
        AdvancedConfig config = new AdvancedPersistentConfig("test", file, ConfigFileType.JSON);

        // Should load but be empty due to handling the parsing error
        assertTrue(config.getKeys(false).isEmpty());

        // Setting a value and saving should work
        config.set("fixed", true);
        config.save();

        // Reload to verify it's fixed
        config.reload();
        assertTrue(config.getBoolean("fixed"));
    }
}
