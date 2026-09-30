package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.AdvancedInMemoryConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.AdvancedPersistentConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigFileType;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.logging.ConfigLogger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VersioningModuleTest {
    static Path tempDir = Path.of("TestOutputs/VersioningModuleTest");

    @BeforeAll
    static void setupLogger() {
        if (!isLoggerInitialized()) {
            ConfigLogger.initialize(new File("target"));
        }
        try {
            Files.walk(tempDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("Failed to delete file: " + path);
                        }
                    });
        } catch (IOException e) {
            System.err.println("Failed to clean target directory: " + e.getMessage());
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

    private static VersioningModule attach(AdvancedConfig config, int maxVersions) {
        VersioningModule module = new VersioningModule(maxVersions);
        config.registerModule(module);
        return module;
    }

    @Test
    void saveOfTheConfigCapturesAVersion() {
        AdvancedConfig config = new AdvancedInMemoryConfig("versioning");
        VersioningModule module = attach(config, VersioningModule.DEFAULT_MAX_VERSIONS);

        config.set("key", "value");
        config.save();

        assertEquals(1, module.getVersionHistory().size());
        assertEquals(1, module.getCurrentVersion());
        assertEquals("value", module.getVersion(0).get("key"));
        assertNull(module.getVersion(1));
    }

    @Test
    void capturedVersionsAreWrittenIntoTheConfigItself() {
        AdvancedConfig config = new AdvancedInMemoryConfig("versioning-in-config");
        VersioningModule module = attach(config, VersioningModule.DEFAULT_MAX_VERSIONS);

        config.set("key", "value");
        config.save();

        ConfigSection stored = config.getSection(VersioningModule.VERSIONS_KEY);
        assertNotNull(stored);
        assertTrue(stored.hasSection("0"));
        assertEquals(0, stored.getInt("0.__version"));
        assertEquals("value", stored.getString("0.snapshot.key"));
        assertFalse(stored.contains("0.snapshot." + VersioningModule.VERSIONS_KEY));
        assertEquals(1, module.getVersionHistory().size());
    }

    @Test
    void versionHistoryAccumulatesAcrossRestarts() {
        File file = tempDir.resolve("versioning.yml").toFile();

        AdvancedPersistentConfig first = new AdvancedPersistentConfig("versioning", file, ConfigFileType.YAML);
        attach(first, VersioningModule.DEFAULT_MAX_VERSIONS);
        first.set("key", "v1");
        first.save();
        first.save();
        first.close();

        AdvancedPersistentConfig second = new AdvancedPersistentConfig("versioning", file, ConfigFileType.YAML);
        VersioningModule reopened = attach(second, VersioningModule.DEFAULT_MAX_VERSIONS);
        assertEquals(1, reopened.getVersionHistory().size());
        assertEquals(1, reopened.getCurrentVersion());
        assertEquals("v1", reopened.getVersion(0).get("key"));

        second.set("key", "v2");
        second.save();
        second.save();
        second.close();

        AdvancedPersistentConfig third = new AdvancedPersistentConfig("versioning", file, ConfigFileType.YAML);
        VersioningModule accumulated = attach(third, VersioningModule.DEFAULT_MAX_VERSIONS);
        assertEquals(2, accumulated.getVersionHistory().size());
        assertEquals(2, accumulated.getCurrentVersion());
        assertEquals("v1", accumulated.getVersion(0).get("key"));
        assertEquals("v2", accumulated.getVersion(1).get("key"));
        third.close();
    }

    @Test
    void reopeningContinuesTheVersionNumbering() {
        File file = tempDir.resolve("numbering.yml").toFile();

        AdvancedPersistentConfig first = new AdvancedPersistentConfig("numbering", file, ConfigFileType.YAML);
        VersioningModule written = attach(first, VersioningModule.DEFAULT_MAX_VERSIONS);
        first.set("key", "v1");
        first.save();
        first.save();
        assertEquals(2, written.getCurrentVersion());
        first.close();

        AdvancedPersistentConfig second = new AdvancedPersistentConfig("numbering", file, ConfigFileType.YAML);
        VersioningModule reopened = attach(second, VersioningModule.DEFAULT_MAX_VERSIONS);
        assertEquals(1, reopened.getCurrentVersion());

        second.set("key", "v3");
        second.save();
        second.save();

        assertEquals(3, reopened.getCurrentVersion());
        assertEquals(3, reopened.getVersionHistory().size());
        assertEquals("v1", reopened.getVersion(0).get("key"));
        assertEquals("v3", reopened.getVersion(2).get("key"));
        second.close();
    }

    @Test
    void storedHistoryIsCapped() {
        AdvancedConfig config = new AdvancedInMemoryConfig("capped");
        VersioningModule module = attach(config, 3);

        for (int i = 0; i < 10; i++) {
            config.set("key", i);
            config.save();
        }

        assertEquals(3, module.getVersionHistory().size());
        assertEquals(10, module.getCurrentVersion());
        assertNull(module.getVersion(6));
        assertEquals(7, module.getVersion(7).get("key"));
        assertEquals(3, config.getSection(VersioningModule.VERSIONS_KEY).getSubSections().size());
    }

    @Test
    void capCanBeChangedThroughTheModuleConfiguration() {
        AdvancedConfig config = new AdvancedInMemoryConfig("configured");
        VersioningModule module = new VersioningModule();
        module.configure(Map.of("maxVersions", 2));
        config.registerModule(module);

        for (int i = 0; i < 5; i++) {
            config.set("key", i);
            config.save();
        }

        assertEquals(2, module.getMaxVersions());
        assertEquals(2, module.getVersionHistory().size());
    }

    @Test
    void disabledModuleStopsCapturingVersions() {
        AdvancedConfig config = new AdvancedInMemoryConfig("disabled");
        VersioningModule module = attach(config, VersioningModule.DEFAULT_MAX_VERSIONS);

        config.set("key", "v1");
        config.save();
        assertEquals(1, module.getVersionHistory().size());

        config.disableModule("VersioningModule");
        int capturedOnDisable = module.getVersionHistory().size();

        config.set("key", "v2");
        config.save();

        assertEquals(capturedOnDisable, module.getVersionHistory().size());
    }

    @Test
    void moduleStateReportsTheHistory() {
        AdvancedConfig config = new AdvancedInMemoryConfig("state");
        VersioningModule module = attach(config, VersioningModule.DEFAULT_MAX_VERSIONS);
        config.set("key", "v1");
        config.save();

        Map<String, Object> state = module.getModuleState();
        assertEquals(1, state.get("versionCount"));
        assertEquals(1, state.get("currentVersion"));
        assertEquals(VersioningModule.DEFAULT_MAX_VERSIONS, state.get("maxVersions"));
    }

    @Test
    void unreadableStoredVersionsAreSkippedInsteadOfFailing() {
        AdvancedConfig config = new AdvancedInMemoryConfig("malformed");
        config.getRootSection().set(VersioningModule.VERSIONS_KEY, Map.of("0", "not-a-snapshot"));
        VersioningModule module = attach(config, VersioningModule.DEFAULT_MAX_VERSIONS);

        assertTrue(module.getVersionHistory().isEmpty());
        assertEquals(0, module.getCurrentVersion());
    }
}
