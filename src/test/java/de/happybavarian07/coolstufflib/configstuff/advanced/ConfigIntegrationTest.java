package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigFileType;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.BaseConfigModule;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.PersistentBackupModule;
import de.happybavarian07.coolstufflib.logging.ConfigLogger;
import org.junit.jupiter.api.*;

import java.io.File;
import java.nio.file.Path;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ConfigIntegrationTest {
    static final Path rootDir = Path.of("TestOutputs").resolve(ConfigIntegrationTest.class.getSimpleName());
    private AdvancedConfigManager configManager;

    @BeforeEach
    public void setup() {
        configManager = new AdvancedConfigManager();
        ConfigLogger.initialize(rootDir.toFile());
    }

    @Test
    void testFullConfigLifecycle() {
        Path testDir = rootDir.resolve("testFullConfigLifecycle");
        testDir.toFile().mkdirs();
        File configFile = new File(testDir.toFile(), "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("k: v");
        } catch (IOException e) {
            fail("Failed to create test config file");
        }

        AdvancedConfig config = configManager.createPersistentConfig("test", configFile, ConfigFileType.YAML, true);

        PersistentBackupModule module = new PersistentBackupModule(5);
        config.registerModule(module);

        assertEquals(BaseConfigModule.ModuleState.ENABLED, module.getState());

        config.set("k", "v2");
        config.save();

        assertEquals("v2", config.getString("k"));

        config.set("newKey", "newValue");
        config.save();

        assertEquals("newValue", config.getString("newKey"));
    }

    @Test
    void testBackupRetention() {
        Path testDir = rootDir.resolve("testBackupRetention");
        testDir.toFile().mkdirs();
        File configFile = new File(testDir.toFile(), "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("k: v");
        } catch (IOException e) {
            fail("Failed to create test config file");
        }

        AdvancedConfig config = configManager.createPersistentConfig("test", configFile, ConfigFileType.YAML, true);

        PersistentBackupModule module = new PersistentBackupModule(3);
        config.registerModule(module);

        for (int i = 1; i <= 5; i++) {
            config.set("k", "v" + i);
            config.save();
        }

        assertEquals("v5", config.getString("k"));
    }

    @Test
    void testErrorScenarios() {
        Path testDir = rootDir.resolve("testErrorScenarios");
        testDir.toFile().mkdirs();
        File configFile = new File(testDir.toFile(), "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("k: v");
        } catch (IOException e) {
            fail("Failed to create test config file");
        }

        AdvancedConfig config = configManager.createPersistentConfig("test", configFile, ConfigFileType.YAML, true);

        PersistentBackupModule module = new PersistentBackupModule(5);
        config.registerModule(module);

        config.set("testKey", "testValue");
        config.save();

        assertEquals("testValue", config.getString("testKey"));
    }

    @Test
    void testMultipleModules() {
        Path testDir = rootDir.resolve("testMultipleModules");
        testDir.toFile().mkdirs();
        File configFile = new File(testDir.toFile(), "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("enabled: true");
        } catch (IOException e) {
            fail("Failed to create test config file");
        }

        AdvancedConfig config = configManager.createPersistentConfig("test", configFile, ConfigFileType.YAML, true);

        PersistentBackupModule backupModule = new PersistentBackupModule(5);
        config.registerModule(backupModule);

        assertTrue(config.hasModule("PersistentBackupModule"));
        assertEquals(1, config.getModules().size());

        config.unregisterModule("PersistentBackupModule");
        assertFalse(config.hasModule("PersistentBackupModule"));
        assertEquals(0, config.getModules().size());
    }

    @Test
    void testConfigReload() {
        Path testDir = rootDir.resolve("testConfigReload");
        testDir.toFile().mkdirs();
        File configFile = new File(testDir.toFile(), "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("initial: value");
        } catch (IOException e) {
            fail("Failed to create test config file");
        }

        AdvancedConfig config = configManager.createPersistentConfig("test", configFile, ConfigFileType.YAML, true);
        assertEquals("value", config.getString("initial"));

        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("initial: changed\nnew: added");
        } catch (IOException e) {
            fail("Failed to update test config file");
        }

        config.reload();
        assertEquals("changed", config.getString("initial"));
        assertEquals("added", config.getString("new"));
    }

    @Test
    void testCommentPersistence() {
        Path testDir = rootDir.resolve("testCommentPersistence");
        testDir.toFile().mkdirs();
        File configFile = new File(testDir.toFile(), "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("server:\n  port: 8080");
        } catch (IOException e) {
            fail("Failed to create test config file");
        }

        AdvancedConfig config = configManager.createPersistentConfig("test", configFile, ConfigFileType.YAML, true);

        config.setComment("server.port", "Web server port");
        config.set("server.host", "localhost");
        config.setComment("server.host", "Web server host");

        assertEquals("Web server port", config.getComment("server.port"));
        assertEquals("Web server host", config.getComment("server.host"));

        assertNull(config.getComment("nonexistent"));

        config.removeComment("server.port");
        assertNull(config.getComment("server.port"));
        assertEquals("Web server host", config.getComment("server.host"));
    }
}