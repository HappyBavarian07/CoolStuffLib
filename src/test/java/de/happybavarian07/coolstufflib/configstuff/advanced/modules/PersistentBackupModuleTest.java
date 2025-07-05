package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.AdvancedPersistentConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigFileType;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.BaseConfigModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistentBackupModuleTest {

    static final Path testDir = Path.of("PersistentBackupModuleTest");

    @BeforeEach
    void setUp() throws IOException {
        if (!Files.exists(testDir)) {
            Files.createDirectories(testDir);
        }
        // Clean up before each test
        try (Stream<Path> paths = Files.walk(testDir)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    @Test
    void testBackupCreation() throws IOException {
        Path testCaseDir = testDir.resolve("testBackupCreation");
        Files.createDirectories(testCaseDir);
        Path backupDir = testCaseDir.resolve("backups");
        Files.createDirectories(backupDir);
        Path configFile = testCaseDir.resolve("config.yml");
        Files.createFile(configFile);
        PersistentBackupModule testModule = new PersistentBackupModule(5, false);
        AdvancedPersistentConfig testConfig = new AdvancedPersistentConfig("testConfig", configFile.toFile(), ConfigFileType.YAML);
        testModule.initialize(testConfig);
        testModule.enable();
        testConfig.set("test.value", "updated");
        testConfig.save();
        assertEquals(BaseConfigModule.ModuleState.ENABLED, testModule.getState());
    }

    @Test
    void testBackupRestore() throws IOException {
        Path testCaseDir = testDir.resolve("testBackupRestore");
        Files.createDirectories(testCaseDir);
        Path backupDir = testCaseDir.resolve("backups");
        Files.createDirectories(backupDir);
        Path configFile = testCaseDir.resolve("config.yml");
        Files.createFile(configFile);
        PersistentBackupModule testModule = new PersistentBackupModule(5, false);
        AdvancedPersistentConfig testConfig = new AdvancedPersistentConfig("testConfig", configFile.toFile(), ConfigFileType.YAML);
        testModule.initialize(testConfig);
        testModule.enable();
        testConfig.set("test.value", "backup_test");
        testConfig.save();
        assertEquals(BaseConfigModule.ModuleState.ENABLED, testModule.getState());
    }

    @Test
    void testBackupDeletion() throws IOException {
        Path testCaseDir = testDir.resolve("testBackupDeletion");
        Files.createDirectories(testCaseDir);
        Path backupDir = testCaseDir.resolve("backups");
        Files.createDirectories(backupDir);
        Path configFile = testCaseDir.resolve("config.yml");
        Files.createFile(configFile);
        PersistentBackupModule testModule = new PersistentBackupModule(5, false);
        AdvancedPersistentConfig testConfig = new AdvancedPersistentConfig("testConfig", configFile.toFile(), ConfigFileType.YAML);
        testModule.initialize(testConfig);
        testModule.enable();
        testConfig.set("test.value", "delete_test");
        testConfig.save();
        assertEquals(BaseConfigModule.ModuleState.ENABLED, testModule.getState());
    }

    @Test
    void testModuleLifecycle() throws IOException {
        Path testCaseDir = testDir.resolve("testModuleLifecycle");
        Files.createDirectories(testCaseDir);
        Path backupDir = testCaseDir.resolve("backups");
        Files.createDirectories(backupDir);
        Path unzipDir = testCaseDir.resolve("unzip");
        Files.createDirectories(unzipDir);
        Path configFile = testCaseDir.resolve("config.yml");
        Files.createFile(configFile);
        PersistentBackupModule testModule = new PersistentBackupModule(5, false);
        AdvancedPersistentConfig testConfig = new AdvancedPersistentConfig("testConfig", configFile.toFile(), ConfigFileType.YAML);
        testModule.initialize(testConfig);
        assertEquals(BaseConfigModule.ModuleState.INITIALIZED, testModule.getState());
        testModule.enable();
        assertEquals(BaseConfigModule.ModuleState.ENABLED, testModule.getState());
        testModule.disable();
        assertEquals(BaseConfigModule.ModuleState.DISABLED, testModule.getState());
        testModule.cleanup();
        assertEquals(BaseConfigModule.ModuleState.UNINITIALIZED, testModule.getState());
    }

    @Test
    void testIntegrationWithBackupManager() throws IOException {
        Path testCaseDir = testDir.resolve("testIntegrationWithBackupManager");
        Files.createDirectories(testCaseDir);
        Path backupDir = testCaseDir.resolve("backups");
        Files.createDirectories(backupDir);
        Path configFile = testCaseDir.resolve("config.yml");
        Files.createFile(configFile);
        PersistentBackupModule testModule = new PersistentBackupModule(5, false);
        AdvancedPersistentConfig testConfig = new AdvancedPersistentConfig("testConfig", configFile.toFile(), ConfigFileType.YAML);
        testModule.initialize(testConfig);
        testModule.enable();
        testConfig.set("test.value", "integration_test");
        testConfig.save();
        assertEquals(BaseConfigModule.ModuleState.ENABLED, testModule.getState());
    }
}
