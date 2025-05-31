package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.AdvancedConfigManager;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers.JsonConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class PersistentBackupModuleTest {
    static final Path rootDir = Path.of("").resolve(PersistentBackupModuleTest.class.getSimpleName());
    private AdvancedConfigManager configManager;

    @BeforeEach
    public void setup() {
        configManager = new AdvancedConfigManager();
    }

    @Test void testBackupCreation() {
        System.out.println(rootDir.toAbsolutePath());
        Path testDir = rootDir.resolve("testBackupCreation");
        testDir.toFile().mkdirs();
        File configFile = new File(testDir.toFile(), "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) { writer.write("k: v"); } catch (IOException e) { fail(); }
        File backupDir = new File(testDir.toFile(), "backups_testBackupCreation");
        backupDir.mkdir();
        AdvancedConfig config = configManager.createPersistentConfig("test", configFile, new JsonConfigFileHandler(), true);
        PersistentBackupModule module = new PersistentBackupModule(5);
        config.registerModule(module);
        module.enable();
        module.setEnabled(true);
        module.save();
        assertFalse(module.listBackups().isEmpty());
    }

    @Test void testRestore() {
        Path testDir = rootDir.resolve("testRestore");
        testDir.toFile().mkdirs();
        File configFile = new File(testDir.toFile(), "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) { writer.write("k: v"); } catch (IOException e) { fail(); }
        File backupDir = new File(testDir.toFile(), "backups_testRestore");
        backupDir.mkdir();
        AdvancedConfig config = configManager.createPersistentConfig("test", configFile, new JsonConfigFileHandler(), true);
        PersistentBackupModule module = new PersistentBackupModule(5);
        module.enable();
        module.setEnabled(true);
        module.onAttach(config);
        module.save();
        File backupFile = module.listBackups().values().iterator().next();
        try (FileWriter writer = new FileWriter(configFile)) { writer.write("changed"); } catch (IOException e) { fail(); }
        boolean restored = module.restoreBackup(0);
        assertTrue(restored);
    }

    @Test void testDelete() {
        Path testDir = rootDir.resolve("testDelete");
        testDir.toFile().mkdirs();
        File configFile = new File(testDir.toFile(), "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) { writer.write("k: v"); } catch (IOException e) { fail(); }
        File backupDir = new File(testDir.toFile(), "backups_testDelete");
        backupDir.mkdir();
        AdvancedConfig config = configManager.createPersistentConfig("test", configFile, new JsonConfigFileHandler(), true);
        PersistentBackupModule module = new PersistentBackupModule(5);
        module.enable();
        module.setEnabled(true);
        module.onAttach(config);
        module.save();
        assertFalse(module.listBackups().isEmpty());
        boolean deleted = module.deleteBackup(0);
        assertTrue(deleted);
    }

    @Test void testModuleLifecycle() {
        Path testDir = rootDir.resolve("testModuleLifecycle");
        testDir.toFile().mkdirs();
        File configFile = new File(testDir.toFile(), "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) { writer.write("k: v"); } catch (IOException e) { fail(); }
        File backupDir = new File(testDir.toFile(), "backups_testModuleLifecycle");
        backupDir.mkdir();
        File unzipDir = new File(testDir.toFile(), "unzip");
        unzipDir.mkdir();
        AdvancedConfig config = configManager.createPersistentConfig("test", configFile, new JsonConfigFileHandler(), true);
        PersistentBackupModule module = new PersistentBackupModule(5);
        module.enable();
        module.setEnabled(true);
        module.onAttach(config);
        assertTrue(module.isEnabled());
        module.disable();
        module.setEnabled(false);
        assertFalse(module.isEnabled());
        module.onDetach();
        assertNull(module.getConfig());
    }

    @Test void testIntegrationWithBackupManager() {
        Path testDir = rootDir.resolve("testIntegrationWithBackupManager");
        testDir.toFile().mkdirs();
        File configFile = new File(testDir.toFile(), "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) { writer.write("k: v"); } catch (IOException e) { fail(); }
        File backupDir = new File(testDir.toFile(), "backups_testIntegrationWithBackupManager");
        backupDir.mkdir();
        AdvancedConfig config = configManager.createPersistentConfig("test", configFile, new JsonConfigFileHandler(), true);
        PersistentBackupModule module = new PersistentBackupModule(5);
        module.enable();
        module.setEnabled(true);
        module.onAttach(config);
        module.save();
        assertFalse(module.listBackups().isEmpty());
    }
}
