package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigValueEvent;

import java.util.HashMap;
import java.util.Map;

public class DefaultsModule extends AbstractBaseConfigModule {
    private final Map<String, Object> defaults = new HashMap<>();

    public DefaultsModule() {
        super("DefaultsModule",
              "Provides default values for configuration entries",
              "1.0.0");
    }

    @Override
    protected void onInitialize() {
        // Apply defaults for any missing values
        applyDefaults();
    }

    @Override
    protected void onEnable() {
        // Register for value change events to monitor removals
        registerEventListener(
            config.getEventBus(),
            ConfigValueEvent.class,
            this::onValueChangeEvent
        );
    }

    @Override
    protected void onDisable() {
        // Unregister from value change events
        unregisterEventListener(
            config.getEventBus(),
            ConfigValueEvent.class,
            this::onValueChangeEvent
        );
    }

    @Override
    protected void onCleanup() {
        // Nothing to clean up
    }

    private void onValueChangeEvent(ConfigValueEvent event) {
        if (event.getType() == ConfigValueEvent.Type.REMOVE) {
            String path = event.getFullPath();

            if (defaults.containsKey(path)) {
                config.set(path, defaults.get(path));
            }
        }
    }

    private void applyDefaults() {
        if (config == null) {
            return;
        }

        // Apply all defaults if they don't already exist
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            if (!config.containsKey(entry.getKey())) {
                config.set(entry.getKey(), entry.getValue());
            }
        }
    }

    public void setDefault(String key, Object value) {
        defaults.put(key, value);

        // If config exists and initialized, apply this default if needed
        if (config != null && !config.containsKey(key)) {
            config.set(key, value);
        }
    }

    public void setDefaults(Map<String, Object> defaultValues) {
        defaults.putAll(defaultValues);

        // If config exists and initialized, apply these defaults where needed
        if (config != null) {
            for (Map.Entry<String, Object> entry : defaultValues.entrySet()) {
                if (!config.containsKey(entry.getKey())) {
                    config.set(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public Object getDefault(String key) {
        return defaults.get(key);
    }

    public boolean hasDefault(String key) {
        return defaults.containsKey(key);
    }

    public void removeDefault(String key) {
        defaults.remove(key);
    }

    public void clearDefaults() {
        defaults.clear();
    }

    public Map<String, Object> getDefaults() {
        return new HashMap<>(defaults);
    }

    public int getDefaultCount() {
        return defaults.size();
    }

    public Map<String, Object> getAdditionalModuleState() {
        Map<String, Object> state = new HashMap<>();
        state.put("defaultsCount", defaults.size());
        state.put("defaults", new HashMap<>(defaults));
        return state;
    }
}
