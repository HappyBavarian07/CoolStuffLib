package de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen;

import de.happybavarian07.coolstufflib.configstuff.advanced.AdvancedConfigManager;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers.JsonConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers.YamlConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.converter.MapConfigTypeConverter;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

class AutoGenModuleIntegrationTest {
    private AdvancedConfigManager configManager;

    static class DummyLocation {
        public double x, y, z;
        public String world;

        public DummyLocation(String world, double x, double y, double z) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    static class DummyPlayer {
        public String name;
        public DummyLocation position;
        public String displayName;
        public UUID uuid;
        public int health;

        public DummyPlayer(String name, DummyLocation position, String displayName, UUID uuid, int health) {
            this.name = name;
            this.position = position;
            this.displayName = displayName;
            this.uuid = uuid;
            this.health = health;
        }
    }

    private static final Path JSON_PATH = Paths.get("TestOutputs/AutoGenIntegrationTest.json");
    private static final Path YAML_PATH = Paths.get("TestOutputs/AutoGenIntegrationTest2.yaml");

    @BeforeEach
    void setup() throws IOException {
        configManager = new AdvancedConfigManager();
        Files.deleteIfExists(JSON_PATH);
    }

    @Test
    void testAutoGenWithInMemoryConfig() {
        DummyPlayer player = new DummyPlayer("MemPlayer", new DummyLocation("world", 1, 2, 3), "memDisp", UUID.randomUUID(), 10);
        AdvancedConfig config = configManager.createInMemoryConfig("mem", true);
        AutoGenModule module = new AutoGenModule();
        module.registerTemplate(AutoGenUtils.createTemplateFromObject("players." + player.uuid, player), "playerTemplate");
        config.registerModule(module);
        Assertions.assertNotNull(module.getGroupByPath("players." + player.uuid));
    }

    @Test
    void testAutoGenWithJsonConfig() throws Exception {
        DummyPlayer player = new DummyPlayer("JsonPlayer", new DummyLocation("overworld", 10, 64, 10), "jsonDisp", UUID.randomUUID(), 18);
        JsonConfigFileHandler handler = new JsonConfigFileHandler();
        File file = JSON_PATH.toFile();
        System.out.println("File Path: " + file.getAbsolutePath());
        AdvancedConfig config = configManager.createPersistentConfig("json", file, handler, true);
        AutoGenModule module = new AutoGenModule();
        module.registerTemplate(AutoGenUtils.createTemplateFromObject("players." + player.uuid, player), "playerTemplate");
        config.registerModule(module);
        module.applyTemplateToConfig("playerTemplate");
        config.save();
        Assertions.assertTrue(file.exists());
        Map<String, Object> map = handler.load(file);
        Assertions.assertNotNull(map);
        System.out.println(map);
        Assertions.assertTrue(map.containsKey("players"));
    }

    @Test
    void testAutoGenWithYamlConfig() throws Exception {
        DummyPlayer player = new DummyPlayer("YamlPlayer", new DummyLocation("nether", 0, 80, 0), "yamlDisp", UUID.randomUUID(), 15);
        File file = YAML_PATH.toFile();
        YamlConfigFileHandler handler = new YamlConfigFileHandler();
        handler.getConverterRegistry().register(DummyLocation.class, new MapConfigTypeConverter<>(DummyLocation.class) {
            @Override
            public Map<String, Object> toSerialized(DummyLocation value) {
                return Map.of("world", value.world, "x", value.x, "y", value.y, "z", value.z);
            }

            @Override
            public DummyLocation fromSerialized(Map<String, Object> serializedValue) {
                return new DummyLocation((String) serializedValue.get("world"), (Double) serializedValue.get("x"), (Double) serializedValue.get("y"), (Double) serializedValue.get("z"));
            }

            @Override
            public boolean canConvertFrom(Object value) {
                return value instanceof Map<?, ?> map && map.containsKey("world") && map.containsKey("x") && map.containsKey("y") && map.containsKey("z");
            }

            @Override
            public Class<?> getInputType() {
                return DummyLocation.class;
            }
        });
        System.out.println("File Path: " + file.getAbsolutePath());
        AdvancedConfig config = configManager.createPersistentConfig("yaml", file, handler, true);
        AutoGenModule module = new AutoGenModule();
        module.registerTemplate(AutoGenUtils.createTemplateFromObject("players." + player.uuid, player), "playerTemplate");
        config.registerModule(module);
        config.save();
        // Reload and verify
        Assertions.assertTrue(file.exists());
        Map<String, Object> map = handler.load(file);
        Assertions.assertTrue(map.containsKey("players"));
    }
}
