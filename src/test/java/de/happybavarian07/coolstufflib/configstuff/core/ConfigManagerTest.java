package de.happybavarian07.coolstufflib.configstuff.core;

import de.happybavarian07.coolstufflib.configstuff.advanced.AdvancedConfigManager;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigFileType;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers.JsonConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers.PropertiesConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers.YamlConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {

    static final Path tempDir = Path.of("TestOutputs/ConfigManagerTest");

    private AdvancedConfigManager manager;

    @BeforeEach
    void setUp() {
        manager = new AdvancedConfigManager();
    }

    @Test
    void testConfigCreation() {
        // Create different types of configs
        File jsonFile = tempDir.resolve("config.json").toFile();
        File yamlFile = tempDir.resolve("config.yml").toFile();
        File propsFile = tempDir.resolve("config.properties").toFile();

        // Create configs with explicit handlers
        AdvancedConfig jsonConfig = manager.createPersistentConfig("json", jsonFile, new JsonConfigFileHandler(), false);
        AdvancedConfig yamlConfig = manager.createPersistentConfig("yaml", yamlFile, new YamlConfigFileHandler(), false);
        AdvancedConfig propsConfig = manager.createPersistentConfig("props", propsFile, new PropertiesConfigFileHandler(), false);

        // Create in-memory config
        AdvancedConfig memoryConfig = manager.createInMemoryConfig("memory");

        // Verify all configs were created and registered
        assertEquals(4, manager.getConfigs().size());
        assertTrue(manager.hasConfig("json"));
        assertTrue(manager.hasConfig("yaml"));
        assertTrue(manager.hasConfig("props"));
        assertTrue(manager.hasConfig("memory"));

        // Get configs by name
        assertSame(jsonConfig, manager.getConfig("json"));
        assertSame(yamlConfig, manager.getConfig("yaml"));
        assertSame(propsConfig, manager.getConfig("props"));
        assertSame(memoryConfig, manager.getConfig("memory"));
    }

    @Test
    void testConfigAutoDetection() {
        // Test auto-detection of file type
        File jsonFile = tempDir.resolve("autodetect.json").toFile();
        File yamlFile = tempDir.resolve("autodetect.yml").toFile();
        File propsFile = tempDir.resolve("autodetect.properties").toFile();

        AdvancedConfig jsonConfig = manager.createPersistentConfig("json", jsonFile, ConfigFileType.JSON);
        AdvancedConfig yamlConfig = manager.createPersistentConfig("yaml", yamlFile, ConfigFileType.YAML);
        AdvancedConfig propsConfig = manager.createPersistentConfig("props", propsFile, ConfigFileType.PROPERTIES);

        // Set sample values
        jsonConfig.set("type", "json");
        yamlConfig.set("type", "yaml");
        propsConfig.set("type", "properties");

        // Save all configs
        manager.saveAll();

        // Verify files were created with appropriate handlers
        assertTrue(jsonFile.exists());
        assertTrue(yamlFile.exists());
        assertTrue(propsFile.exists());

        // Create a new manager and load the configs
        AdvancedConfigManager newManager = new AdvancedConfigManager();
        AdvancedConfig loadedJson = newManager.createPersistentConfig("json", jsonFile, ConfigFileType.JSON);
        AdvancedConfig loadedYaml = newManager.createPersistentConfig("yaml", yamlFile, ConfigFileType.YAML);
        AdvancedConfig loadedProps = newManager.createPersistentConfig("props", propsFile, ConfigFileType.PROPERTIES);

        // Verify values were loaded correctly
        assertEquals("json", loadedJson.getString("type"));
        assertEquals("yaml", loadedYaml.getString("type"));
        assertEquals("properties", loadedProps.getString("type"));
    }

    @Test
    void testConfigRegistrationAndUnregistration() {
        // Create a config
        AdvancedConfig config = manager.createInMemoryConfig("test");
        assertTrue(manager.hasConfig("test"));

        // Unregister config
        manager.unregisterConfig("test");
        assertFalse(manager.hasConfig("test"));
        assertNull(manager.getConfig("test"));

        // Re-register the same config
        manager.registerConfig(config);
        assertTrue(manager.hasConfig("test"));
    }

    @Test
    void testBulkOperations() {
        // Create multiple configs
        manager.createInMemoryConfig("config1");
        manager.createInMemoryConfig("config2");
        manager.createInMemoryConfig("config3");

        // Set values
        manager.getConfig("config1").set("value", 1);
        manager.getConfig("config2").set("value", 2);
        manager.getConfig("config3").set("value", 3);

        // Test reloadAll
        manager.reloadAll();

        // Test clear
        manager.clear();
        //assertTrue(manager.getConfig("config1").getKeys(false).isEmpty());
        //assertTrue(manager.getConfig("config2").getKeys(false).isEmpty());
        //assertTrue(manager.getConfig("config3").getKeys(false).isEmpty());
    }


    @Test
    void testConfigGroups() {
        // Create configs
        manager.createInMemoryConfig("game.settings");
        manager.createInMemoryConfig("game.user");
        manager.createInMemoryConfig("system.settings");

        // Test getting configs by prefix
        Set<String> gameConfigs = manager.getConfigsByPrefix("game.");
        assertEquals(2, gameConfigs.size());
        assertTrue(gameConfigs.contains("game.settings"));
        assertTrue(gameConfigs.contains("game.user"));

        // Test getting system configs
        Set<String> systemConfigs = manager.getConfigsByPrefix("system.");
        assertEquals(1, systemConfigs.size());
        assertTrue(systemConfigs.contains("system.settings"));
    }

    @Test
    void testSaveAndLoadAll() throws IOException {
        // Create persistent configs
        File file1 = tempDir.resolve("config1.json").toFile();
        File file2 = tempDir.resolve("config2.json").toFile();

        AdvancedConfig config1 = manager.createPersistentConfig("config1", file1, new JsonConfigFileHandler(), false);
        AdvancedConfig config2 = manager.createPersistentConfig("config2", file2, new JsonConfigFileHandler(), false);

        // Set values
        config1.set("key1", "value1");
        config2.set("key2", "value2");

        // Save all
        manager.saveAll();

        // Verify files exist
        assertTrue(file1.exists());
        assertTrue(file2.exists());

        // Create a new manager
        AdvancedConfigManager newManager = new AdvancedConfigManager();

        // Load configs
        AdvancedConfig loaded1 = newManager.createPersistentConfig("config1", file1, new JsonConfigFileHandler(), false);
        AdvancedConfig loaded2 = newManager.createPersistentConfig("config2", file2, new JsonConfigFileHandler(), false);

        // Verify values
        assertEquals("value1", loaded1.getString("key1"));
        assertEquals("value2", loaded2.getString("key2"));
    }
}
