package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfigGroup;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.BaseConfigModule;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.GroupConfigModule;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

class DefaultAdvancedConfigGroup implements AdvancedConfigGroup {
    private final String name;
    private final List<AdvancedConfig> configs = new CopyOnWriteArrayList<>();
    private final Map<String, GroupConfigModule> groupModules = new HashMap<>();
    private final Map<String, BaseConfigModule> defaultModules = new HashMap<>();

    public DefaultAdvancedConfigGroup(String name) {
        this(name, new BaseConfigModule[0]);
    }

    public DefaultAdvancedConfigGroup(String name, BaseConfigModule... defaultModule) {
        this.name = name;
        for (BaseConfigModule module : defaultModule) {
            if (module != null) {
                defaultModules.put(module.getName(), module);
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addConfig(AdvancedConfig config) {
        configs.add(config);
        // Register default modules if specified
        for (String moduleName : defaultModules.keySet()) {
            if (!config.hasModule(moduleName)) {
                BaseConfigModule module = defaultModules.get(moduleName);
                if (module != null) {
                    config.registerModule(module);
                    config.enableModule(moduleName);
                }
            }
        }
        groupModules.values().forEach(m -> m.onConfigAdded(config));
    }

    @Override
    public void removeConfig(AdvancedConfig config) {
        configs.remove(config);
        groupModules.values().forEach(m -> m.onConfigRemoved(config));
    }

    @Override
    public List<AdvancedConfig> getConfigs() {
        return Collections.unmodifiableList(configs);
    }

    @Override
    public Map<String, Object> getValuesFromAll(String key) {
        Map<String, Object> result = new HashMap<>();
        for (AdvancedConfig config : configs) {
            Object value = config.get(key);
            if (value != null) {
                result.put(config.getName(), value);
            }
        }
        groupModules.values().forEach(m -> m.onGetValuesFromAll(key, result));
        return result;
    }

    @Override
    public <T> T getFirstValue(String key, Class<T> type) {
        for (AdvancedConfig config : configs) {
            Object value = config.get(key);
            if (type.isInstance(value)) {
                return type.cast(value);
            }
        }
        for (GroupConfigModule m : groupModules.values()) {
            T val = m.onGetFirstValue(key, type, null);
            if (val != null) return val;
        }
        return null;
    }

    @Override
    public boolean containsKeyInAny(String key) {
        for (AdvancedConfig config : configs) {
            if (config.containsKey(key)) return true;
        }
        return false;
    }

    @Override
    public Set<String> getAllKeys() {
        Set<String> keys = new HashSet<>();
        for (AdvancedConfig config : configs) {
            keys.addAll(config.getKeys());
        }
        return keys;
    }

    @Override
    public void saveAll() {
        for (AdvancedConfig config : configs) {
            config.save();
        }
        groupModules.values().forEach(GroupConfigModule::save);
    }

    @Override
    public void reloadAll() {
        for (AdvancedConfig config : configs) {
            config.reload();
        }
        groupModules.values().forEach(GroupConfigModule::reload);
    }

    @Override
    public void applyToAll(Consumer<AdvancedConfig> action) {
        for (AdvancedConfig config : configs) {
            action.accept(config);
        }
        groupModules.values().forEach(m -> m.onGroupApply(action));
    }

    @Override
    public void registerGroupModule(GroupConfigModule module) {
        if (groupModules.containsKey(module.getName())) return;
        groupModules.put(module.getName(), module);
        // Propagate to all configs: the group module wraps a config module, so call its logic
        for (AdvancedConfig config : configs) {
            module.onConfigAdded(config);
        }
        module.onGroupAttach(this);
        if( !module.isEnabled()) {
            module.enable();
            module.setEnabled(true);
        }
    }

    @Override
    public void unregisterGroupModule(String name) {
        if (!groupModules.containsKey(name)) return;
        GroupConfigModule module = groupModules.remove(name);
        for (AdvancedConfig config : configs) {
            module.onConfigRemoved(config);
        }
        if(module.isEnabled()) {
            module.disable();
            module.setEnabled(false);
        }
        module.onGroupDetach();
    }

    @Override
    public Map<String, GroupConfigModule> getGroupModules() {
        return Collections.unmodifiableMap(groupModules);
    }
}
