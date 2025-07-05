package de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc.Group;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc.Key;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.templates.AutoGenTemplate;
import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.UUID;

class AutoGenModuleTest {
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

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path CONFIG_PATH = Paths.get("AutoGenTestPlayerConfig.json");

    @BeforeEach
    void setup() throws IOException {
        Files.deleteIfExists(CONFIG_PATH);
    }

    @Test
    void testAutoGenModulePerPlayerConfig() throws Exception {
        DummyPlayer player = new DummyPlayer(
                "TestPlayer",
                new DummyLocation("world", 100.5, 64, -30.25),
                "§aTestPlayer",
                UUID.randomUUID(),
                20
        );

        AutoGenModule module = new AutoGenModule();
        AutoGenTemplate template = AutoGenUtils.createTemplateFromObject("players." + player.uuid, player);
        module.registerTemplate(template, "players." + player.uuid);

        // Serialize config to disk (simple JSON-like, not production)
        Map<String, Object> configMap = extractGroup(module.getGroupByPath("players." + player.uuid));
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            writer.write(toJson(configMap));
        }

        // Read back and verify
        String content = Files.readString(CONFIG_PATH);
        Assertions.assertTrue(content.contains("TestPlayer"));
        Assertions.assertTrue(content.contains("§aTestPlayer"));
        Assertions.assertTrue(content.contains("world"));
        Assertions.assertTrue(content.contains("health"));
        Assertions.assertTrue(content.contains(player.uuid.toString()));
    }

    // Helper: recursively extract group to map
    private Map<String, Object> extractGroup(Group group) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Key key : group.getKeys()) {
            map.put(key.getName(), key.getValue());
        }
        for (Group sub : group.getSubGroups()) {
            map.put(sub.getName(), extractGroup(sub));
        }
        return map;
    }

    // Simple JSON (no escaping, for test purposes only)
    private String toJson(Map<String, Object> map) {
        return gson.toJson(map);
    }
}
