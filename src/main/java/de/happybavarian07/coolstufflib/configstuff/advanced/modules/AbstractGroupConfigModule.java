package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfigGroup;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.GroupConfigModule;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractGroupConfigModule implements GroupConfigModule {
    private final String name;
    private final String description;
    private final String version;
    private AdvancedConfigGroup group;
    private boolean enabled;

    public AbstractGroupConfigModule(String name, String description, String version) {
        this.name = name;
        this.description = description;
        this.version = version;
        this.enabled = false;
    }

    @Override
    public final String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    public AdvancedConfigGroup getGroup() {
        return group;
    }

    @Override
    public final void initialize(AdvancedConfigGroup group) {
        if (this.group != null) {
            return;
        }
        this.group = group;
        onInitialize();
    }

    @Override
    public final void enable() {
        if (enabled) {
            return;
        }
        enabled = true;
        onEnable();
    }

    @Override
    public final void disable() {
        if (!enabled) {
            return;
        }
        enabled = false;
        onDisable();
    }

    @Override
    public final void cleanup() {
        if (enabled) {
            disable();
        }
        onCleanup();
        group = null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    protected abstract void onInitialize();

    protected abstract void onEnable();

    protected abstract void onDisable();

    protected abstract void onCleanup();

    @Override
    public final Map<String, Object> getModuleState() {
        Map<String, Object> state = new HashMap<>();

        state.put("name", name);
        state.put("description", description);
        state.put("version", version);
        state.put("enabled", enabled);

        Map<String, Object> additionalState = getAdditionalModuleState();
        if (additionalState != null) {
            state.putAll(additionalState);
        }

        return state;
    }

    protected abstract Map<String, Object> getAdditionalModuleState();
}
