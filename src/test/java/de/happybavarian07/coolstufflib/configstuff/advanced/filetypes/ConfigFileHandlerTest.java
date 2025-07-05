package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes;

import de.happybavarian07.coolstufflib.configstuff.advanced.AdvancedConfigManager;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.ConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigFileHandlerTest {
    static final Path rootDir = Path.of("").resolve(ConfigFileHandlerTest.class.getSimpleName());
    private AdvancedConfigManager configManager;

    @BeforeEach
    public void setup() {
        configManager = new AdvancedConfigManager();
    }

    @ParameterizedTest
    @EnumSource(value = ConfigFileType.class, names = {"MEMORY"}, mode = EnumSource.Mode.EXCLUDE)
    void testBasicSaveAndLoad(ConfigFileType fileType) throws Exception {
        Path testDir = rootDir.resolve(fileType.name());
        testDir.toFile().mkdirs();
        File tempFile = new File(testDir.toFile(), "test." + getFileExtension(fileType));
        ConfigFileHandler handler = fileType.createHandler();
        AdvancedConfig config = configManager.createInMemoryConfig("test", true);
        config.set("key", "value");
        config.set("num", 123);
        handler.save(tempFile, config.getRootSection().toSerializableMap());
        Map<String, Object> loaded = handler.load(tempFile);
        assertEquals("value", loaded.get("key"));
        Object num = loaded.get("num");
        if (num instanceof Number) {
            assertEquals(123, ((Number) num).intValue());
        } else if (num instanceof String) {
            assertEquals("123", num);
        } else {
            throw new AssertionError("Unexpected type for num: " + num.getClass());
        }
    }

    @ParameterizedTest
    @EnumSource(value = ConfigFileType.class, names = {"MEMORY"}, mode = EnumSource.Mode.EXCLUDE)
    void testComplexDataStructures(ConfigFileType fileType) throws Exception {
        Path testDir = rootDir.resolve(fileType.name());
        testDir.toFile().mkdirs();
        File tempFile = new File(testDir.toFile(), "complex." + getFileExtension(fileType));
        ConfigFileHandler handler = fileType.createHandler();

        AdvancedConfig config = configManager.createInMemoryConfig("complex", true);
        config.set("simple.string", "test_value");
        config.set("simple.boolean", true);
        config.set("simple.integer", 42);
        config.set("simple.double", 3.14);
        config.set("nested.section.key", "nested_value");
        config.set("nested.section.number", 999);
        config.set("list.items", List.of("item1", "item2", "item3"));

        handler.save(tempFile, config.getRootSection().toSerializableMap());
        Map<String, Object> loaded = handler.load(tempFile);

        System.out.println("[" + fileType + "] Loaded config: " + loaded);
        System.out.println("[" + fileType + "] simple.string: " + getNestedValue(loaded, "simple.string"));
        System.out.println("[" + fileType + "] simple.boolean: " + getNestedValue(loaded, "simple.boolean"));
        System.out.println("[" + fileType + "] nested.section.key: " + getNestedValue(loaded, "nested.section.key"));
        System.out.println("[" + fileType + "] list.items: " + getNestedValue(loaded, "list.items"));

        assertEquals("test_value", getNestedValue(loaded, "simple.string"));
        assertEquals(true, getNestedValue(loaded, "simple.boolean"));
        assertNotNull(getNestedValue(loaded, "nested.section.key"));
        assertNotNull(getNestedValue(loaded, "list.items"));
    }

    @ParameterizedTest
    @EnumSource(value = ConfigFileType.class, names = {"MEMORY"}, mode = EnumSource.Mode.EXCLUDE)
    void testEmptyAndNullValues(ConfigFileType fileType) throws Exception {
        Path testDir = rootDir.resolve(fileType.name());
        testDir.toFile().mkdirs();
        File tempFile = new File(testDir.toFile(), "empty." + getFileExtension(fileType));
        ConfigFileHandler handler = fileType.createHandler();

        AdvancedConfig config = configManager.createInMemoryConfig("empty", true);
        config.set("empty.string", "");
        config.set("null.value", null);
        config.set("zero.number", 0);

        handler.save(tempFile, config.getRootSection().toSerializableMap());
        Map<String, Object> loaded = handler.load(tempFile);

        assertTrue(loaded.containsKey("empty") || containsNestedKey(loaded, "empty.string"));
        assertTrue(loaded.containsKey("zero") || containsNestedKey(loaded, "zero.number"));
    }

    @ParameterizedTest
    @EnumSource(value = ConfigFileType.class, names = {"MEMORY"}, mode = EnumSource.Mode.EXCLUDE)
    void testFileExtensionHandling(ConfigFileType fileType) {
        ConfigFileHandler handler = fileType.createHandler();
        String extension = getFileExtension(fileType);

        assertTrue(handler.canHandle(new File("test." + extension)));
        assertTrue(handler.canHandle(new File("test." + extension.toUpperCase())));

        if (!fileType.equals(ConfigFileType.MEMORY)) {
            assertFalse(handler.canHandle(new File("test.wrongext")));
        }
    }

    @ParameterizedTest
    @EnumSource(value = ConfigFileType.class, names = {"MEMORY"}, mode = EnumSource.Mode.EXCLUDE)
    void testErrorHandling(ConfigFileType fileType) {
        ConfigFileHandler handler = fileType.createHandler();
        File nonExistentFile = new File("nonexistent." + getFileExtension(fileType));

        assertThrows(IOException.class, () -> handler.load(nonExistentFile));
    }

    @Test
    void testAllHandlersAreAvailable() {
        for (ConfigFileType type : ConfigFileType.values()) {
            ConfigFileHandler handler = type.createHandler();
            assertNotNull(handler, "Handler should be available for " + type);
            assertNotNull(type.getFileExtension(), "File extension should be defined for " + type);
        }
    }

    @ParameterizedTest
    @EnumSource(value = ConfigFileType.class, names = {"MEMORY"}, mode = EnumSource.Mode.EXCLUDE)
    void testCommentSupport(ConfigFileType fileType) {
        ConfigFileHandler handler = fileType.createHandler();
        boolean supportsComments = handler.supportsComments();

        if (fileType == ConfigFileType.YAML || fileType == ConfigFileType.INI || fileType == ConfigFileType.TOML || fileType == ConfigFileType.PROPERTIES) {
            assertTrue(supportsComments, "Handler should support comments for " + fileType);
        } else {
            assertFalse(supportsComments, "Handler should not support comments for " + fileType);
        }
    }

    private Object getNestedValue(Map<String, Object> map, String path) {
        String[] parts = path.split("\\.");
        Object current = map;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    private boolean containsNestedKey(Map<String, Object> map, String path) {
        return getNestedValue(map, path) != null || map.containsKey(path.split("\\.")[0]);
    }

    private String getFileExtension(ConfigFileType fileType) {
        return switch (fileType) {
            case YAML -> "yml";
            case JSON -> "json";
            case PROPERTIES -> "properties";
            case INI -> "ini";
            case TOML -> "toml";
            case JSON5 -> "json5";
            case MEMORY -> "mem";
            default -> throw new IllegalArgumentException("Unknown file type: " + fileType);
        };
    }
}
