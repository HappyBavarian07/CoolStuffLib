package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.GroupConfigModule;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.BaseConfigModule;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.ConfigModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedConfigGroupTest {
    private AdvancedConfigManager manager;

    @BeforeEach
    void setup() {
        manager = new AdvancedConfigManager();
    }

    @Test
    void testGroupCreationAndConfigAddRemove() {
        AdvancedConfig config1 = manager.createInMemoryConfig("c1", true);
        AdvancedConfig config2 = manager.createInMemoryConfig("c2", true);
        var group = manager.createEmptyGroup("g1");
        group.addConfig(config1);
        group.addConfig(config2);
        assertEquals(2, group.getConfigs().size());
        group.removeConfig(config1);
        assertEquals(1, group.getConfigs().size());
    }

    @Test
    void testGetValuesFromAllAndFirstValue() {
        AdvancedConfig config1 = manager.createInMemoryConfig("c1", true);
        AdvancedConfig config2 = manager.createInMemoryConfig("c2", true);
        config1.setValue("foo", 10);
        config2.setValue("foo", 20);
        var group = manager.createGroup("g1", List.of(config1, config2));
        Map<String, Object> values = group.getValuesFromAll("foo");
        assertEquals(2, values.size());
        assertTrue(values.containsValue(10));
        assertTrue(values.containsValue(20));
        Integer first = group.getFirstValue("foo", Integer.class);
        assertNotNull(first);
        assertTrue(first == 10 || first == 20);
    }

    @Test
    void testContainsKeyInAnyAndGetAllKeys() {
        AdvancedConfig config1 = manager.createInMemoryConfig("c1", true);
        AdvancedConfig config2 = manager.createInMemoryConfig("c2", true);
        config1.setValue("a", 1);
        config2.setValue("b", 2);
        var group = manager.createGroup("g1", List.of(config1, config2));
        assertTrue(group.containsKeyInAny("a"));
        assertTrue(group.containsKeyInAny("b"));
        assertFalse(group.containsKeyInAny("c"));
        var keys = group.getAllKeys();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains("b"));
    }

    @Test
    void testDefaultGroupModuleRegistration() {
        AdvancedConfig config1 = manager.createInMemoryConfig("c1", true);
        AdvancedConfig config2 = manager.createInMemoryConfig("c2", true);
        DummyModule dummy = new DummyModule();
        config1.registerModule(dummy);
        var group = manager.createGroup("g1", List.of(config1, config2));
        manager.registerConfigModuleAsGroupModule("g1", dummy);
        GroupConfigModule groupModule = group.getGroupModules().values().stream().findFirst().orElse(null);
        assertNotNull(groupModule);
        assertTrue(config1.hasModule(dummy.getName()));
        assertTrue(config2.hasModule(dummy.getName()));
        assertTrue(config1.getModuleByName(dummy.getName()).isEnabled());
        assertTrue(config2.getModuleByName(dummy.getName()).isEnabled());
    }

    static class DummyModule extends ConfigModule {
        @Override
        public void enable() {
            // Do nothing
        }

        @Override
        public void disable() {
            // Do nothing
        }

        @Override public String getName() { return "Dummy"; }
        @Override public void reload() {}
        @Override public void save() {}

        @Override
        public Object onGetValue(String key, Object value) {
            return value;
        }

        @Override
        public void onConfigChange(String key, Object oldValue, Object newValue) {
            // Do nothing
        }

        @Override
        public boolean supportsConfig(AdvancedConfig config) {
            return true;
        }

        @Override
        public Map<String, Object> getModuleState() {
            return Map.of("enabled", isEnabled());
        }
    }
}
