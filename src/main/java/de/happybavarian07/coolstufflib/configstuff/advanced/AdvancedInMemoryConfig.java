package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.BaseConfigModule;

import java.io.File;
import java.util.*;

class AdvancedInMemoryConfig implements AdvancedConfig {
    private final Map<String, BaseConfigModule> modules = new HashMap<>();
    private final String name;
    private final Map<String, Object> values = new HashMap<>();

    public AdvancedInMemoryConfig(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public File getFile() {
        return null;
    }

    @Override
    public Object get(String key) {
        return values.get(key);
    }

    @Override
    public Object get(String key, Object defaultValue) {
        Object value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        for (BaseConfigModule module : modules.values()) {
            value = module.onGetValue(key, value);
        }
        return value;
    }

    @Override
    public <T> T get(String key, T defaultValue, Class<T> type) {
        Object value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        for (BaseConfigModule module : modules.values()) {
            value = module.onGetValue(key, value);
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        } else {
            throw new ClassCastException("Value for key '" + key + "' is not of type " + type.getName());
        }
    }

    @Override
    public void setValue(String key, Object value) {
        Object oldValue = values.put(key, value);
        for (BaseConfigModule module : modules.values()) {
            module.onConfigChange(key, oldValue, value);
        }
    }

    @Override
    public void setValueBulk(Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            setValue(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void remove(String key) {
        Object oldValue = values.remove(key);
        for (BaseConfigModule module : modules.values()) {
            module.onConfigChange(key, oldValue, null);
        }
    }

    @Override
    public boolean containsKey(String key) {
        return values.containsKey(key);
    }

    @Override
    public void registerModule(BaseConfigModule module) {
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        }
        if (modules.containsKey(module.getName())) {
            throw new IllegalArgumentException("Module already registered with the name: " + module.getName());
        }
        modules.put(module.getName(), module);
        module.onAttach(this);
        if (!module.isEnabled()) {
            module.enable();
            module.setEnabled(true);
        }
    }

    @Override
    public void unregisterModule(BaseConfigModule module) {
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        }
        if (!modules.containsKey(module.getName())) {
            throw new IllegalArgumentException("Module not registered with the name: " + module.getName());
        }
        if (module.isEnabled()) {
            module.disable();
            module.setEnabled(false);
        }
        module.onDetach();
        modules.remove(module.getName());
    }

    @Override
    public void unregisterModule(String name) {
        if (!modules.containsKey(name)) {
            throw new IllegalArgumentException("Module not registered with the name: " + name);
        }
        BaseConfigModule module = modules.remove(name);
        if (module.isEnabled()) {
            module.disable();
            module.setEnabled(false);
        }
        module.onDetach();
    }

    @Override
    public boolean hasModule(BaseConfigModule module) {
        return modules.containsKey(module.getName());
    }

    @Override
    public boolean hasModule(String moduleName) {
        return modules.containsKey(moduleName);
    }

    @Override
    public BaseConfigModule getModuleByName(String name) {
        return modules.get(name);
    }

    @Override
    public void enableModule(String moduleName) {
        BaseConfigModule module = modules.get(moduleName);
        if (module != null) {
            if (module.isEnabled()) {
                throw new IllegalStateException("Module is already enabled: " + moduleName);
            }
            module.enable();
            module.setEnabled(true);
        }
    }

    @Override
    public void disableModule(String moduleName) {
        BaseConfigModule module = modules.get(moduleName);
        if (module != null) {
            if (!module.isEnabled()) {
                throw new IllegalStateException("Module is already disabled: " + moduleName);
            }
            module.disable();
            module.setEnabled(false);
        }
    }

    @Override
    public Map<String, BaseConfigModule> getModules() {
        return Map.copyOf(modules);
    }

    @Override
    public void save() {
        for (BaseConfigModule module : modules.values()) module.save();
    }

    @Override
    public void reload() {
        for (BaseConfigModule module : modules.values()) module.reload();
    }

    @Override
    public List<String> getKeys() {
        return new ArrayList<>(values.keySet());
    }

    @Override
    public Map<String, Object> getValueMap() {
        return Collections.unmodifiableMap(values);
    }
}
