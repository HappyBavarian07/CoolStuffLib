package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.AdvancedInMemoryConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.BaseConfigModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigChangeTrackerModuleTest {

    private ConfigChangeTrackerModule module;
    private AdvancedInMemoryConfig config;

    @BeforeEach
    void setUp() {
        module = new ConfigChangeTrackerModule();
        config = new AdvancedInMemoryConfig("testConfig");

        module.initialize(config);
        module.enable();
    }

    @Test
    void testInitialState() {
        assertEquals("ConfigChangeTracker", module.getName());
        assertEquals("1.0.0", module.getVersion());
        assertEquals(BaseConfigModule.ModuleState.ENABLED, module.getState());
    }

    @Test
    void testTrackingValueChanges() {
        config.set("test.key1", "value1");
        config.set("test.key2", "value2");
        config.set("test.key1", "updatedValue1");

        List<String> changes = module.getChanges();
        assertFalse(changes.isEmpty());
    }

    @Test
    void testTrackingWithFilter() {
        config.set("track.this", "value1");
        config.set("ignore.this", "value2");

        List<String> filteredChanges = module.getFilteredChanges(change -> change.contains("track."));
        assertFalse(filteredChanges.isEmpty());
    }

    @Test
    void testMaxChangeHistoryLimit() {
        module.configure(Map.of("maxChangesToTrack", 3));

        for (int i = 0; i < 5; i++) {
            config.set("test.key" + i, "value" + i);
        }

        List<String> changes = module.getChanges();
        assertTrue(changes.size() <= 3);
    }

    @Test
    void testClearChanges() {
        config.set("test.key", "value");
        assertFalse(module.getChanges().isEmpty());

        module.clearChanges();
        assertTrue(module.getChanges().isEmpty());
    }

    @Test
    void testValueChangeTracking() {
        module.configure(Map.of("trackValueChanges", true));

        config.set("test.value", "tracked");
        assertFalse(module.getChanges().isEmpty());
    }

    @Test
    void testDisableValueChangeTracking() {
        module.configure(Map.of("trackValueChanges", false));
        System.out.println("trackValueChanges after configure: " + module.isTrackValueChanges());
        config.set("test.value", "not_tracked");
        System.out.println("Changes after set: " + module.getChanges());
        assertTrue(module.getChanges().isEmpty(), "Changes should be empty when value tracking is disabled, but was: " + module.getChanges());
        module.disable();
        module.cleanup();
    }

    @Test
    void testSectionChangeTracking() {
        module.configure(Map.of("trackSectionChanges", true));

        config.set("section.key", "value");
        assertFalse(module.getChanges().isEmpty());

        module.disable();
        module.cleanup();
    }

    @Test
    void testModuleLifecycle() {
        assertEquals(BaseConfigModule.ModuleState.ENABLED, module.getState());

        module.disable();
        assertEquals(BaseConfigModule.ModuleState.DISABLED, module.getState());

        module.enable();
        assertEquals(BaseConfigModule.ModuleState.ENABLED, module.getState());

        module.disable();
        module.cleanup();
        assertEquals(BaseConfigModule.ModuleState.UNINITIALIZED, module.getState());
    }

    @Test
    void testConfigurationOptions() {
        module.configure(Map.of(
            "maxChangesToTrack", 50,
            "trackValueChanges", false,
            "trackSectionChanges", false
        ));

        assertTrue(module.isConfigured());
    }

    @Test
    void testChangeFormatting() {
        config.set("test.format", "value");

        List<String> changes = module.getChanges();
        assertFalse(changes.isEmpty());

        String lastChange = changes.get(changes.size() - 1);
        assertTrue(lastChange.contains("SET"));
        assertTrue(lastChange.contains("test.format"));
    }
}
