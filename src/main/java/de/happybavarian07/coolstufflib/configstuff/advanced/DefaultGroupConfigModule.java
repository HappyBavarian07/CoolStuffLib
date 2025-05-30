package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfigGroup;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.GroupConfigModule;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.BaseConfigModule;

import java.util.Map;
import java.util.function.Consumer;

class DefaultGroupConfigModule implements GroupConfigModule {
    private final BaseConfigModule module;
    private AdvancedConfigGroup group;

    public DefaultGroupConfigModule(BaseConfigModule module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return module.getName();
    }

    @Override
    public void onGroupAttach(AdvancedConfigGroup group) {
        this.group = group;
        for (AdvancedConfig config : group.getConfigs()) {
            if (!config.hasModule(module.getName())) {
                config.registerModule(module);
                config.getModuleByName(module.getName()).enable();
            } else {
                BaseConfigModule existingModule = config.getModuleByName(module.getName());
                if (existingModule != null && !existingModule.isEnabled()) {
                    existingModule.enable();
                }
            }
        }
    }

    @Override
    public void onGroupDetach() {
        this.group = null;
    }

    @Override
    public boolean isEnabled() {
        return module.isEnabled();
    }

    @Override
    public void enable() {
        module.enable();
    }

    @Override
    public void disable() {
        module.disable();
    }

    @Override
    public void setEnabled(boolean enabled) {
        module.setEnabled(enabled);
    }

    @Override
    public AdvancedConfigGroup getGroup() {
        return group;
    }

    @Override
    public void reload() {
        if (group == null) return;
        for (AdvancedConfig config : group.getConfigs()) {
            if (config.hasModule(module.getName())) {
                config.getModuleByName(module.getName()).reload();
            }
        }
    }

    @Override
    public void save() {
        if (group == null) return;
        for (AdvancedConfig config : group.getConfigs()) {
            if (config.hasModule(module.getName())) {
                config.getModuleByName(module.getName()).save();
            }
        }
    }

    @Override
    public boolean supportsConfig(AdvancedConfig config) {
        return config != null && config.hasModule(module.getName()) &&
                module.supportsConfig(config);
    }

    @Override
    public void onGetValuesFromAll(String key, Map<String, Object> values) {
        if (group == null) return;
        for (AdvancedConfig config : group.getConfigs()) {
            if (config.hasModule(module.getName())) {
                config.getModuleByName(module.getName()).onGetValue(key, values);
            }
        }
    }

    @Override
    public <T> T onGetFirstValue(String key, Class<T> type, T defaultValue) {
        if (group == null) return defaultValue;
        for (AdvancedConfig config : group.getConfigs()) {
            Object value = config.get(key);
            if (type.isInstance(value)) {
                return type.cast(value);
            }
        }
        return defaultValue;
    }

    @Override
    public void onConfigAdded(AdvancedConfig config) {
        if (!config.hasModule(module.getName())) {
            config.registerModule(module);
            if (!config.getModuleByName(module.getName()).isEnabled()) {
                config.getModuleByName(module.getName()).enable();
            }
        }
    }

    @Override
    public void onConfigRemoved(AdvancedConfig config) {
        if (config.hasModule(module.getName())) {
            if (config.getModuleByName(module.getName()).isEnabled()) {
                config.getModuleByName(module.getName()).disable();
            }
            config.unregisterModule(module.getName());
        }
    }

    @Override
    public void onGroupApply(Consumer<AdvancedConfig> action) {
        if (group == null) return;
        for (AdvancedConfig config : group.getConfigs()) {
            action.accept(config);
        }
    }

    public BaseConfigModule getModule() {
        return module;
    }
}
