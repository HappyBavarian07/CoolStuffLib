package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes;

import de.happybavarian07.coolstufflib.configstuff.advanced.AdvancedConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.ConfigFileHandler;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigFileHandlerTest {
    static final Path rootDir = Path.of("").resolve(ConfigFileHandlerTest.class.getSimpleName());
    private AdvancedConfigManager configManager;

    @BeforeEach
    public void setup() {
        configManager = new AdvancedConfigManager();
    }

    @ParameterizedTest
    @EnumSource(ConfigFileType.class)
    void testHandlerForEachType(ConfigFileType fileType) throws Exception {
        Path testDir = rootDir.resolve(fileType.name());
        testDir.toFile().mkdirs();
        File tempFile = new File(testDir.toFile(), "test." + fileType.name().toLowerCase());
        ConfigFileHandler handler = getHandler(fileType);
        AdvancedConfig config = configManager.createInMemoryConfig("test", true);
        config.setValue("key", "value");
        config.setValue("num", 123);
        handler.save(tempFile, config.getValueMap());
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

    private ConfigFileHandler getHandler(ConfigFileType type) {
        return switch (type) {
            case YAML ->
                    new de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers.YamlConfigFileHandler();
            case JSON ->
                    new de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers.JsonConfigFileHandler();
            case PROPERTIES ->
                    new de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers.PropertiesConfigFileHandler();
            case INI ->
                    new de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers.IniConfigFileHandler();
            case TOML ->
                    new de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers.TomlConfigFileHandler();
            case JSON5 ->
                    new de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers.Json5ConfigFileHandler();
        };
    }
}
