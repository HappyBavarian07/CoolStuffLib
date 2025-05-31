package de.happybavarian07.coolstufflib;

import de.happybavarian07.coolstufflib.backupmanager.BackupManager;
import de.happybavarian07.coolstufflib.configstuff.advanced.AdvancedConfigManager;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers.JsonConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.PersistentBackupModule;
import org.junit.Before;
import org.junit.jupiter.api.*;

import java.io.File;
import java.nio.file.Path;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ConfigIntegrationTest {
    static final Path rootDir = Path.of("").resolve(ConfigIntegrationTest.class.getSimpleName());
    private AdvancedConfigManager configManager;

    @BeforeEach
    public void setup() {
        configManager = new AdvancedConfigManager();
    }

    @Test
    void testFullConfigLifecycle() {
        Path testDir = rootDir.resolve("testFullConfigLifecycle");
        testDir.toFile().mkdirs();
        File configFile = new File(testDir.toFile(), "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("k: v");
        } catch (IOException e) {
            fail();
        }
        File backupDir = new File(testDir.toFile(), "backups");
        backupDir.mkdir();
        AdvancedConfig config = configManager.createPersistentConfig("test", configFile, new JsonConfigFileHandler(), true);
        PersistentBackupModule module = new PersistentBackupModule(5);
        module.enable();
        module.setEnabled(true);
        module.onAttach(config);
        config.setValue("k", "v2");
        module.save();
        assertFalse(module.listBackups().isEmpty());
        File backupFile = module.listBackups().values().iterator().next();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("changed");
        } catch (IOException e) {
            fail();
        }
        boolean restored = module.restoreBackup(1);
        assertTrue(restored);
        boolean deleted = module.deleteBackup(1);
        assertTrue(deleted);
    }

    @Test
    void testBackupRetention() {
        Path testDir = rootDir.resolve("testBackupRetention");
        testDir.toFile().mkdirs();
        File configFile = new File(testDir.toFile(), "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("k: v");
        } catch (IOException e) {
            fail();
        }
        File backupDir = new File(testDir.toFile(), "backups");
        backupDir.mkdir();
        AdvancedConfig config = configManager.createPersistentConfig("test", configFile, new JsonConfigFileHandler(), true);
        PersistentBackupModule module = new PersistentBackupModule(5);
        module.enable();
        module.setEnabled(true);
        module.onAttach(config);
        module.save();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("k: v2");
        } catch (IOException e) {
            fail();
        }
        module.save();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("k: v3");
        } catch (IOException e) {
            fail();
        }
        module.save();
        assertTrue(module.listBackups().size() <= 5);
    }

    @Test
    void testErrorScenarios() {
        Path testDir = rootDir.resolve("testErrorScenarios");
        testDir.toFile().mkdirs();
        File configFile = new File(testDir.toFile(), "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("k: v");
        } catch (IOException e) {
            fail();
        }
        File backupDir = new File(testDir.toFile(), "backups");
        backupDir.mkdir();
        AdvancedConfig config = configManager.createPersistentConfig("test", configFile, new JsonConfigFileHandler(), true);
        PersistentBackupModule module = new PersistentBackupModule(5);
        module.enable();
        module.setEnabled(true);
        module.onAttach(config);
        // Try restoring non-existent backup
        assertThrows(IndexOutOfBoundsException.class, () -> {
            module.restoreBackup(99);
        });
        // Try deleting non-existent backup
        assertThrows(IndexOutOfBoundsException.class, () -> {
            module.deleteBackup(99);
        });
    }
}