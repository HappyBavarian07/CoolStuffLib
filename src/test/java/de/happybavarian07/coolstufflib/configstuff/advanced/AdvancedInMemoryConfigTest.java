package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.BaseConfigModule;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.ConfigModule;
import org.junit.jupiter.api.*;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedInMemoryConfigTest {
    @Test
    void testCrudOperations() {
        AdvancedInMemoryConfig config = new AdvancedInMemoryConfig("test");
        assertEquals("test", config.getName());
        assertNull(config.get("key1"));
        config.setValue("key1", "val1");
        assertEquals("val1", config.get("key1"));
        assertTrue(config.containsKey("key1"));
        config.remove("key1");
        assertFalse(config.containsKey("key1"));
    }

    @Test
    void testModuleRegistration() {
        AdvancedInMemoryConfig config = new AdvancedInMemoryConfig("test");
        DummyModule module = new DummyModule();
        config.registerModule(module);
        assertTrue(config.getModules().containsValue(module));
        config.unregisterModule(module.getName());
        assertFalse(config.getModules().containsValue(module));
    }

    @Test
    void testInMemoryLogic() {
        AdvancedInMemoryConfig config = new AdvancedInMemoryConfig("test");
        config.setValue("a", 1);
        config.setValue("b", 2);
        assertEquals(2, config.getKeys().size());
        config.remove("a");
        assertEquals(1, config.getKeys().size());
        config.setValue("b", null);
        assertNull(config.get("b"));
    }

    static class DummyModule extends ConfigModule {
        @Override
        public String getName() {
            return "Dummy";
        }

        @Override
        public void enable() {
        }

        @Override
        public void disable() {
        }

        @Override
        public void onConfigChange(String key, Object oldValue, Object newValue) {
        }

        @Override
        public boolean supportsConfig(AdvancedConfig config) {
            return true;
        }

        @Override
        public void reload() {
        }

        @Override
        public void save() {
        }

        @Override
        public Map<String, Object> getModuleState() {
            return Collections.emptyMap();
        }

        @Override
        public Object onGetValue(String key, Object value) {
            return value;
        }
    }
}
