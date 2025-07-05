package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfigGroup;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.AbstractGroupConfigModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedConfigGroupTest {
    static final Path tempDir = Path.of("AdvancedConfigGroupTest");
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
        config1.set("foo", 10);
        config2.set("foo", 20);
        var group = manager.createGroup("g1", List.of(config1, config2));
        Map<String, Object> values = group.getValuesFromAll("foo");
        assertEquals(2, values.size());
        assertTrue(values.containsKey("c1"));
        assertTrue(values.containsKey("c2"));
        assertEquals(10, values.get("c1"));
        assertEquals(20, values.get("c2"));

        Integer first = group.getFirstValue("foo", Integer.class);
        assertNotNull(first);
        assertTrue(first == 10 || first == 20);
    }

    @Test
    void testGetValuesWithType() {
        AdvancedConfig config1 = manager.createInMemoryConfig("c1", true);
        AdvancedConfig config2 = manager.createInMemoryConfig("c2", true);
        config1.set("foo", 10);
        config2.set("foo", 20);
        var group = manager.createGroup("g1", List.of(config1, config2));

        Map<String, Integer> typedValues = group.getValuesFromAll("foo", Integer.class);
        assertEquals(2, typedValues.size());
        assertEquals(10, typedValues.get("c1"));
        assertEquals(20, typedValues.get("c2"));
    }

    @Test
    void testContainsKeyInAnyAndGetAllKeys() {
        AdvancedConfig config1 = manager.createInMemoryConfig("c1", true);
        AdvancedConfig config2 = manager.createInMemoryConfig("c2", true);
        config1.set("a", 1);
        config2.set("b", 2);
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
    void testSetValueInAll() {
        AdvancedConfig config1 = manager.createInMemoryConfig("c1", true);
        AdvancedConfig config2 = manager.createInMemoryConfig("c2", true);
        var group = manager.createGroup("g1", List.of(config1, config2));

        group.setValueInAll("test", "value");

        assertEquals("value", config1.get("test"));
        assertEquals("value", config2.get("test"));
    }

    @Test
    void testGetConfigValue() {
        AdvancedConfig config1 = manager.createInMemoryConfig("c1", true);
        AdvancedConfig config2 = manager.createInMemoryConfig("c2", true);
        config1.set("foo", 10);
        config2.set("bar", "test");
        var group = manager.createGroup("g1", List.of(config1, config2));

        Integer value1 = group.getConfigValue("c1", "foo", Integer.class);
        assertEquals(10, value1);

        String value2 = group.getConfigValue("c2", "bar", String.class);
        assertEquals("test", value2);

        Integer valueWithDefault = group.getConfigValue("c1", "nonexistent", 42, Integer.class);
        assertEquals(42, valueWithDefault);
    }

    @Test
    void testGroupModuleLifecycle() {
        AdvancedConfig config1 = manager.createInMemoryConfig("c1", true);
        AdvancedConfig config2 = manager.createInMemoryConfig("c2", true);
        var group = manager.createGroup("g1", List.of(config1, config2));

        TestGroupModule module = new TestGroupModule();
        group.registerGroupModule("test", module);

        assertTrue(group.hasGroupModule("test"));
        assertSame(module, group.getGroupModule("test"));
        assertEquals(1, group.getGroupModules().size());

        group.enableGroupModule("test");
        assertTrue(module.isEnabled());

        group.disableGroupModule("test");
        assertFalse(module.isEnabled());

        group.unregisterGroupModule("test");
        assertFalse(group.hasGroupModule("test"));
        assertTrue(module.cleanedUp);
    }

    @Test
    void testFirstValueWithDefault() {
        AdvancedConfig config1 = manager.createInMemoryConfig("c1", true);
        AdvancedConfig config2 = manager.createInMemoryConfig("c2", true);
        var group = manager.createGroup("g1", List.of(config1, config2));

        String defaultValue = group.getFirstValue("nonexistent", "default", String.class);
        assertEquals("default", defaultValue);
    }

    @Test
    void testManagerGroupOperations() {
        AdvancedConfigGroup group1 = manager.createEmptyGroup("g1");
        AdvancedConfigGroup group2 = manager.createEmptyGroup("g2");

        assertTrue(manager.hasGroup("g1"));
        assertTrue(manager.hasGroup("g2"));
        assertEquals(2, manager.getGroups().size());

        assertSame(group1, manager.getGroup("g1"));

        manager.unregisterGroup("g1");
        assertFalse(manager.hasGroup("g1"));
        assertEquals(1, manager.getGroups().size());
    }

    @Test
    void testGlobalGroupModules() {
        TestGroupModule globalModule = new TestGroupModule();
        manager.registerGlobalGroupModule(globalModule);

        assertTrue(manager.hasGlobalGroupModule("test"));
        assertSame(globalModule, manager.getGlobalGroupModule("test"));
        assertEquals(1, manager.getGlobalGroupModules().size());

        // Test if new groups automatically get the global module
        AdvancedConfigGroup group = manager.createEmptyGroup("g1");
        assertTrue(group.hasGroupModule("test"));
        assertSame(globalModule, group.getGroupModule("test"));

        // Test if existing groups get the global module when it's registered
        AdvancedConfigGroup group2 = new DefaultAdvancedConfigGroup("g2");
        manager.registerGroup(group2);
        assertTrue(group2.hasGroupModule("test"));

        // Test module unregistration
        manager.unregisterGlobalGroupModule("test");
        assertFalse(manager.hasGlobalGroupModule("test"));

        // New groups shouldn't get the unregistered module
        AdvancedConfigGroup group3 = manager.createEmptyGroup("g3");
        assertFalse(group3.hasGroupModule("test"));
    }

    static class TestGroupModule extends AbstractGroupConfigModule {
        private boolean cleanedUp = false;

        public TestGroupModule() {
            super("test", "Test Group Module for AdvancedConfigGroup", "1.0.0");
        }

        @Override
        protected void onInitialize() {
            cleanedUp = false;
        }

        @Override
        protected void onEnable() {

        }

        @Override
        protected void onDisable() {

        }

        @Override
        protected void onCleanup() {
            cleanedUp = true;
        }

        @Override
        public void onConfigAdded(AdvancedConfig config) {
            // For testing purposes
        }

        @Override
        public void onConfigRemoved(AdvancedConfig config) {
            // For testing purposes
        }

        @Override
        public boolean appliesTo(AdvancedConfig config) {
            return true; // For testing purposes, applies to all configs
        }

        @Override
        public Set<String> getRequiredModules() {
            return Set.of(); // No dependencies for this test module
        }

        @Override
        public Map<String, Object> getAdditionalModuleState() {
            return Map.of("isEnabled", isEnabled(),
                    "groupName", getGroup() != null ? getGroup().getName() : "None",
                    "version", getVersion());
        }
    }
}
