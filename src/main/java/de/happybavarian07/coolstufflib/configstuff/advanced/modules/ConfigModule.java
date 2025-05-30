package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.BaseConfigModule;
import de.happybavarian07.coolstufflib.configstuff.advanced.ConfigLogger;

import java.util.Map;

/*
 * @Author HappyBavarian07
 * @Date Mai 29, 2025 | 18:42
 */
public abstract class ConfigModule implements BaseConfigModule {
    private boolean enabled = false;
    private AdvancedConfig config;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void onAttach(AdvancedConfig config) {
        this.config = config;
        ConfigLogger.info(String.format("Module '%s' attached to config '%s'", 
            getClass().getSimpleName(), config.getName()), getName(), false);
    }

    @Override
    public void onDetach() {
        if (this.config != null) {
            ConfigLogger.info(String.format("Module '%s' detached from config '%s'", 
                getClass().getSimpleName(), config.getName()), getName(), false);
        }
        this.config = null;
    }

    @Override
    public AdvancedConfig getConfig() {
        return config;
    }

    @Override
    public abstract void enable();
    @Override
    public abstract void disable();
    @Override
    public abstract String getName();
    @Override
    public abstract void reload();
    @Override
    public abstract void save();
    @Override
    public abstract Object onGetValue(String key, Object value);
    @Override
    public abstract void onConfigChange(String key, Object oldValue, Object newValue);
    @Override
    public abstract boolean supportsConfig(AdvancedConfig config);
    @Override
    public abstract Map<String, Object> getModuleState();
}
