package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.BaseConfigModule;

import java.io.File;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class AdvancedInMemoryConfig implements AdvancedConfig {
    private final ReadWriteLock moduleLock = new ReentrantReadWriteLock();
    private final ReadWriteLock valuesLock = new ReentrantReadWriteLock();
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
    public void lockModuleWrite() {
        moduleLock.writeLock().lock();
    }

    @Override
    public void unlockModuleWrite() {
        moduleLock.writeLock().unlock();
    }

    @Override
    public void lockModuleRead() {
        moduleLock.readLock().lock();
    }

    @Override
    public void unlockModuleRead() {
        moduleLock.readLock().unlock();
    }

    @Override
    public void lockValuesRead() {
        valuesLock.readLock().lock();
    }

    @Override
    public void unlockValuesRead() {
        valuesLock.readLock().unlock();
    }

    @Override
    public void lockValuesWrite() {
        valuesLock.writeLock().lock();
    }

    @Override
    public void unlockValuesWrite() {
        valuesLock.writeLock().unlock();
    }

    @Override
    public File getFile() {
        return null;
    }

    @Override
    public Object get(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        valuesLock.readLock().lock();
        moduleLock.readLock().lock();
        try {
            Object value = values.get(key);
            if (value == null) {
                return null;
            }
            for (BaseConfigModule module : modules.values()) {
                value = module.onGetValue(key, value);
            }
            return value;
        } finally {
            moduleLock.readLock().unlock();
            valuesLock.readLock().unlock();
        }
    }

    @Override
    public Object get(String key, Object defaultValue) {
        if (key == null || key.isEmpty() || defaultValue == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        valuesLock.readLock().lock();
        moduleLock.readLock().lock();
        try {
            Object value = values.get(key);
            if (value == null) {
                return defaultValue;
            }
            for (BaseConfigModule module : modules.values()) {
                value = module.onGetValue(key, value);
            }
            return value;
        } finally {
            moduleLock.readLock().unlock();
            valuesLock.readLock().unlock();
        }
    }

    @Override
    public <T> T get(String key, T defaultValue, Class<T> type) {
        if (key == null || key.isEmpty() || defaultValue == null || type == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        valuesLock.readLock().lock();
        moduleLock.readLock().lock();
        try {
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
        } finally {
            moduleLock.readLock().unlock();
            valuesLock.readLock().unlock();
        }
    }

    @Override
    public String getString(String key) {
        return (String) get(key);
    }

    @Override
    public int getInt(String key) {
        return (int) get(key);
    }

    @Override
    public boolean getBoolean(String key) {
        return (boolean) get(key);
    }

    @Override
    public double getDouble(String key) {
        return (double) get(key);
    }

    @Override
    public long getLong(String key) {
        return (long) get(key);
    }

    @Override
    public List<?> getList(String key) {
        return (List<?>) get(key);
    }

    @Override
    public List<String> getStringList(String key) {
        return (List<String>) get(key);
    }

    @Override
    public Map<?, ?> getMap(String key) {
        return (Map<?, ?>) get(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return (String) get(key, defaultValue);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return (int) get(key, defaultValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return (boolean) get(key, defaultValue);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        return (double) get(key, defaultValue);
    }

    @Override
    public float getFloat(String key) {
        return (float) get(key);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        return (float) get(key, defaultValue);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return (long) get(key, defaultValue);
    }

    @Override
    public List<?> getList(String key, List<?> defaultValue) {
        return (List<?>) get(key, defaultValue);
    }

    @Override
    public List<String> getStringList(String key, List<String> defaultValue) {
        return (List<String>) get(key, defaultValue);
    }

    @Override
    public Map<?, ?> getMap(String key, Map<?, ?> defaultValue) {
        return (Map<?, ?>) get(key, defaultValue);
    }

    @Override
    public void setValue(String key, Object value) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        valuesLock.writeLock().lock();
        moduleLock.readLock().lock();
        try {
            Object oldValue = values.put(key, value);
            for (BaseConfigModule module : modules.values()) {
                module.onConfigChange(key, oldValue, value);
            }
        } finally {
            valuesLock.writeLock().unlock();
            moduleLock.readLock().unlock();
        }
    }

    @Override
    public void setValueBulk(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Values cannot be null or empty");
        }
        valuesLock.writeLock().lock();
        moduleLock.readLock().lock();
        try {
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                Object oldValue = this.values.put(entry.getKey(), entry.getValue());
                for (BaseConfigModule module : modules.values()) {
                    module.onConfigChange(entry.getKey(), oldValue, entry.getValue());
                }
            }
        } finally {
            valuesLock.writeLock().unlock();
            moduleLock.readLock().unlock();
        }
    }

    @Override
    public void remove(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        valuesLock.writeLock().lock();
        moduleLock.readLock().lock();
        try {
            Object oldValue = values.remove(key);
            for (BaseConfigModule module : modules.values()) {
                module.onConfigChange(key, oldValue, null);
            }
        } finally {
            valuesLock.writeLock().unlock();
            moduleLock.readLock().unlock();
        }
    }

    @Override
    public boolean containsKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        valuesLock.readLock().lock();
        try {
            return values.containsKey(key);
        } finally {
            valuesLock.readLock().unlock();
        }
    }

    @Override
    public void registerModule(BaseConfigModule module) {
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        }
        if (module.getName() == null || module.getName().isEmpty()) {
            throw new IllegalArgumentException("Module name cannot be null or empty");
        }

        moduleLock.writeLock().lock();
        try {
            if (modules.containsKey(module.getName())) {
                throw new IllegalArgumentException("Module already registered with the name: " + module.getName());
            }
            modules.put(module.getName(), module);
            BaseConfigModule existingModule = modules.get(module.getName());
            existingModule.onAttach(this);
            if (!existingModule.isEnabled()) {
                existingModule.enable();
                existingModule.setEnabled(true);
            }
        } finally {
            moduleLock.writeLock().unlock();
        }
    }

    @Override
    public void unregisterModule(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Module name cannot be null or empty");
        }
        moduleLock.writeLock().lock();
        try {
            if (!modules.containsKey(name)) {
                throw new IllegalArgumentException("Module not registered with the name: " + name);
            }
            BaseConfigModule module = modules.remove(name);
            if (module.isEnabled()) {
                module.disable();
                module.setEnabled(false);
            }
            module.onDetach();
        } finally {
            moduleLock.writeLock().unlock();
        }
    }

    @Override
    public boolean hasModule(BaseConfigModule module) {
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        }
        moduleLock.readLock().lock();
        try {
            return modules.containsKey(module.getName());
        } finally {
            moduleLock.readLock().unlock();
        }
    }

    @Override
    public boolean hasModule(String moduleName) {
        if (moduleName == null || moduleName.isEmpty()) {
            throw new IllegalArgumentException("Module name cannot be null or empty");
        }
        moduleLock.readLock().lock();
        try {
            return modules.containsKey(moduleName);
        } finally {
            moduleLock.readLock().unlock();
        }
    }

    @Override
    public BaseConfigModule getModuleByName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Module name cannot be null or empty");
        }
        moduleLock.readLock().lock();
        try {
            return modules.get(name);
        } finally {
            moduleLock.readLock().unlock();
        }
    }

    @Override
    public void enableModule(String moduleName) {
        moduleLock.readLock().lock();
        try {
            BaseConfigModule module = modules.get(moduleName);
            if (module == null) {
                throw new IllegalArgumentException("Module not registered with the name: " + moduleName);
            }
            if (module.isEnabled()) {
                throw new IllegalStateException("Module is already enabled: " + moduleName);
            }
            module.enable();
            module.setEnabled(true);
        } finally {
            moduleLock.readLock().unlock();
        }
    }

    @Override
    public void disableModule(String moduleName) {
        moduleLock.readLock().lock();
        try {
            BaseConfigModule module = modules.get(moduleName);
            if (module == null) {
                throw new IllegalArgumentException("Module not registered with the name: " + moduleName);
            }
            if (!module.isEnabled()) {
                throw new IllegalStateException("Module is already disabled: " + moduleName);
            }
            module.disable();
            module.setEnabled(false);
        } finally {
            moduleLock.readLock().unlock();
        }
    }

    @Override
    public Map<String, BaseConfigModule> getModules() {
        moduleLock.readLock().lock();
        try {
            return Collections.unmodifiableMap(modules);
        } finally {
            moduleLock.readLock().unlock();
        }
    }

    @Override
    public void save() {
        moduleLock.readLock().lock();
        try {
            for (BaseConfigModule module : modules.values()) module.save();
        } finally {
            moduleLock.readLock().unlock();
        }
    }

    @Override
    public void reload() {
        moduleLock.readLock().lock();
        try {
            for (BaseConfigModule module : modules.values()) module.reload();
        } finally {
            moduleLock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        valuesLock.writeLock().lock();
        try {
            values.clear();
        } finally {
            valuesLock.writeLock().unlock();
        }
    }

    @Override
    public List<String> getKeys() {
        valuesLock.readLock().lock();
        try {
            return new ArrayList<>(values.keySet());
        } finally {
            valuesLock.readLock().unlock();
        }
    }

    @Override
    public Map<String, Object> getValueMap() {
        valuesLock.readLock().lock();
        try {
            return Collections.unmodifiableMap(values);
        } finally {
            valuesLock.readLock().unlock();
        }
    }
}
