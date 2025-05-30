package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;

import java.util.HashMap;
import java.util.Map;

public class DefaultsModule extends ConfigModule {
    private final Map<String, Object> defaults = new HashMap<>();

    public void setDefault(String key, Object value) {
        defaults.put(key, value);
    }

    public Object getDefault(String key) {
        return defaults.get(key);
    }

    @Override
    public String getName() { return "DefaultsModule"; }

    @Override
    public void enable() {
        // Do nothing
    }

    @Override
    public void disable() {
        // Do nothing
    }

    @Override
    public void onAttach(AdvancedConfig config) {
        super.onAttach(config);
        // Apply defaults to missing keys
        for (var entry : defaults.entrySet()) {
            if (config.get(entry.getKey()) == null) {
                config.setValue(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void reload() {}
    @Override
    public void save() {}
    @Override
    public void onConfigChange(String key, Object oldValue, Object newValue) {
        if (newValue == null && defaults.containsKey(key) && getConfig() != null && isEnabled()) {
            getConfig().setValue(key, defaults.get(key));
        }
    }
    @Override
    public boolean supportsConfig(AdvancedConfig config) { return true; }
    @Override
    public Map<String, Object> getModuleState() { return Map.of(); }
    @Override
    public Object onGetValue(String key, Object value) {
        return value;
    }
}
