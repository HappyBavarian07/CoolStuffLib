package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigFileType;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers.*;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.ConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfigGroup;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.BaseConfigModule;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.GroupConfigModule;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class AdvancedConfigManager {
    private final Map<String, AdvancedConfig> configs;
    private final Map<String, BaseConfigModule> globalModules;
    private final Map<String, AdvancedConfigGroup> groups;
    private final Map<String, GroupConfigModule> globalGroupModules;
    private final ReadWriteLock configsLock;
    private final ReadWriteLock modulesLock;
    private final ReadWriteLock groupsLock;
    private final ReadWriteLock groupModulesLock;

    public AdvancedConfigManager() {
        this.configs = new ConcurrentHashMap<>();
        this.globalModules = new ConcurrentHashMap<>();
        this.groups = new ConcurrentHashMap<>();
        this.globalGroupModules = new ConcurrentHashMap<>();
        this.configsLock = new ReentrantReadWriteLock();
        this.modulesLock = new ReentrantReadWriteLock();
        this.groupsLock = new ReentrantReadWriteLock();
        this.groupModulesLock = new ReentrantReadWriteLock();
    }

    public AdvancedConfig createPersistentConfig(String name, File file, ConfigFileType type) {
        return createPersistentConfig(name, file, type.createHandler(), true);
    }

    public AdvancedConfig createPersistentConfig(String name, File file, ConfigFileType type, boolean registerGlobalModules) {
        ConfigFileHandler configFileHandler = getFileHandlerFromType(type);
        if (configFileHandler == null) {
            throw new IllegalArgumentException("Unsupported ConfigFileType: " + type);
        }
        return createPersistentConfig(name, file, configFileHandler, registerGlobalModules);
    }

    public AdvancedConfig createPersistentConfig(String name, File file, ConfigFileHandler configFileHandler, boolean registerGlobalModules) {
        configsLock.writeLock().lock();
        try {
            if (configs.containsKey(name)) {
                return configs.get(name);
            }

            AdvancedConfig config = new AdvancedPersistentConfig(name, file, configFileHandler);
            configs.put(name, config);

            if (registerGlobalModules) {
                registerGlobalModulesToConfig(config);
            }

            return config;
        } finally {
            configsLock.writeLock().unlock();
        }
    }

    public ConfigFileHandler getFileHandlerFromType(ConfigFileType type) {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case YAML -> new YamlConfigFileHandler();
            case JSON -> new JsonConfigFileHandler();
            case PROPERTIES -> new PropertiesConfigFileHandler();
            case INI -> new IniConfigFileHandler();
            case TOML ->  new TomlConfigFileHandler();
            case JSON5 -> new Json5ConfigFileHandler();
            case MEMORY -> new InMemoryConfigFileHandler();
            default -> throw new IllegalArgumentException("Unsupported ConfigFileType: " + type);
        };
    }

    public AdvancedConfig createInMemoryConfig(String name) {
        return createInMemoryConfig(name, true);
    }

    public AdvancedConfig createInMemoryConfig(String name, boolean registerGlobalModules) {
        configsLock.writeLock().lock();
        try {
            if (configs.containsKey(name)) {
                return configs.get(name);
            }

            AdvancedConfig config = new AdvancedInMemoryConfig(name);
            configs.put(name, config);

            if (registerGlobalModules) {
                registerGlobalModulesToConfig(config);
            }

            return config;
        } finally {
            configsLock.writeLock().unlock();
        }
    }

    public void registerConfig(AdvancedConfig config) {
        if (config == null) {
            return;
        }

        configsLock.writeLock().lock();
        try {
            configs.put(config.getName(), config);
        } finally {
            configsLock.writeLock().unlock();
        }
    }

    public void unregisterConfig(String name) {
        configsLock.writeLock().lock();
        try {
            configs.remove(name);
        } finally {
            configsLock.writeLock().unlock();
        }
    }

    public boolean hasConfig(String name) {
        configsLock.readLock().lock();
        try {
            return configs.containsKey(name);
        } finally {
            configsLock.readLock().unlock();
        }
    }

    public AdvancedConfig getConfig(String name) {
        configsLock.readLock().lock();
        try {
            return configs.get(name);
        } finally {
            configsLock.readLock().unlock();
        }
    }

    public Map<String, AdvancedConfig> getConfigs() {
        configsLock.readLock().lock();
        try {
            return Collections.unmodifiableMap(configs);
        } finally {
            configsLock.readLock().unlock();
        }
    }

    public AdvancedConfigGroup createEmptyGroup(String name) {
        groupsLock.writeLock().lock();
        try {
            if (groups.containsKey(name)) {
                return groups.get(name);
            }

            AdvancedConfigGroup group = new DefaultAdvancedConfigGroup(name);
            groups.put(name, group);

            // Register global group modules
            registerGlobalGroupModulesToGroup(group);

            return group;
        } finally {
            groupsLock.writeLock().unlock();
        }
    }

    public AdvancedConfigGroup createGroup(String name, List<AdvancedConfig> configs) {
        groupsLock.writeLock().lock();
        try {
            if (groups.containsKey(name)) {
                return groups.get(name);
            }

            AdvancedConfigGroup group = new DefaultAdvancedConfigGroup(name);
            groups.put(name, group);

            // Register global group modules
            registerGlobalGroupModulesToGroup(group);

            for (AdvancedConfig config : configs) {
                group.addConfig(config);
            }

            return group;
        } finally {
            groupsLock.writeLock().unlock();
        }
    }

    public void registerGroup(AdvancedConfigGroup group) {
        if (group == null) {
            return;
        }

        groupsLock.writeLock().lock();
        try {
            groups.put(group.getName(), group);

            // Register global group modules
            registerGlobalGroupModulesToGroup(group);
        } finally {
            groupsLock.writeLock().unlock();
        }
    }

    public void unregisterGroup(String name) {
        groupsLock.writeLock().lock();
        try {
            groups.remove(name);
        } finally {
            groupsLock.writeLock().unlock();
        }
    }

    public boolean hasGroup(String name) {
        groupsLock.readLock().lock();
        try {
            return groups.containsKey(name);
        } finally {
            groupsLock.readLock().unlock();
        }
    }

    public AdvancedConfigGroup getGroup(String name) {
        groupsLock.readLock().lock();
        try {
            return groups.get(name);
        } finally {
            groupsLock.readLock().unlock();
        }
    }

    public Map<String, AdvancedConfigGroup> getGroups() {
        groupsLock.readLock().lock();
        try {
            return Collections.unmodifiableMap(groups);
        } finally {
            groupsLock.readLock().unlock();
        }
    }

    public void registerGlobalModule(BaseConfigModule module) {
        if (module == null) {
            return;
        }

        modulesLock.writeLock().lock();
        try {
            globalModules.put(module.getName(), module);

            // Register to all existing configs
            configsLock.readLock().lock();
            try {
                configs.values().forEach(config -> {
                    if (!config.hasModule(module)) {
                        config.registerModule(module);
                    }
                });
            } finally {
                configsLock.readLock().unlock();
            }
        } finally {
            modulesLock.writeLock().unlock();
        }
    }

    public void unregisterGlobalModule(String name) {
        modulesLock.writeLock().lock();
        try {
            globalModules.remove(name);
        } finally {
            modulesLock.writeLock().unlock();
        }
    }

    public boolean hasGlobalModule(String name) {
        modulesLock.readLock().lock();
        try {
            return globalModules.containsKey(name);
        } finally {
            modulesLock.readLock().unlock();
        }
    }

    public BaseConfigModule getGlobalModule(String name) {
        modulesLock.readLock().lock();
        try {
            return globalModules.get(name);
        } finally {
            modulesLock.readLock().unlock();
        }
    }

    public Map<String, BaseConfigModule> getGlobalModules() {
        modulesLock.readLock().lock();
        try {
            return Collections.unmodifiableMap(globalModules);
        } finally {
            modulesLock.readLock().unlock();
        }
    }

    public void registerGlobalGroupModule(GroupConfigModule module) {
        if (module == null) {
            return;
        }

        groupModulesLock.writeLock().lock();
        try {
            globalGroupModules.put(module.getName(), module);

            // Register to all existing groups
            groupsLock.readLock().lock();
            try {
                for (AdvancedConfigGroup group : groups.values()) {
                    if (!group.hasGroupModule(module.getName())) {
                        group.registerGroupModule(module.getName(), module);
                    }
                }
            } finally {
                groupsLock.readLock().unlock();
            }
        } finally {
            groupModulesLock.writeLock().unlock();
        }
    }

    public void unregisterGlobalGroupModule(String name) {
        groupModulesLock.writeLock().lock();
        try {
            globalGroupModules.remove(name);
        } finally {
            groupModulesLock.writeLock().unlock();
        }
    }

    public boolean hasGlobalGroupModule(String name) {
        groupModulesLock.readLock().lock();
        try {
            return globalGroupModules.containsKey(name);
        } finally {
            groupModulesLock.readLock().unlock();
        }
    }

    public GroupConfigModule getGlobalGroupModule(String name) {
        groupModulesLock.readLock().lock();
        try {
            return globalGroupModules.get(name);
        } finally {
            groupModulesLock.readLock().unlock();
        }
    }

    public Map<String, GroupConfigModule> getGlobalGroupModules() {
        groupModulesLock.readLock().lock();
        try {
            return Collections.unmodifiableMap(globalGroupModules);
        } finally {
            groupModulesLock.readLock().unlock();
        }
    }

    private void registerGlobalModulesToConfig(AdvancedConfig config) {
        if (config == null) {
            return;
        }

        modulesLock.readLock().lock();
        try {
            for (BaseConfigModule module : globalModules.values()) {
                if (!config.hasModule(module)) {
                    config.registerModule(module);
                }
            }
        } finally {
            modulesLock.readLock().unlock();
        }
    }

    private void registerGlobalGroupModulesToGroup(AdvancedConfigGroup group) {
        if (group == null) {
            return;
        }

        groupModulesLock.readLock().lock();
        try {
            for (Map.Entry<String, GroupConfigModule> entry : globalGroupModules.entrySet()) {
                if (!group.hasGroupModule(entry.getKey())) {
                    group.registerGroupModule(entry.getKey(), entry.getValue());
                }
            }
        } finally {
            groupModulesLock.readLock().unlock();
        }
    }

    public void saveAll() {
        configsLock.readLock().lock();
        try {
            for (AdvancedConfig config : configs.values()) {
                config.save();
            }
        } finally {
            configsLock.readLock().unlock();
        }
    }

    public void reloadAll() {
        configsLock.readLock().lock();
        try {
            for (AdvancedConfig config : configs.values()) {
                config.reload();
            }
        } finally {
            configsLock.readLock().unlock();
        }
    }

    public void clear() {
        configsLock.writeLock().lock();
        try {
            configs.clear();
        } finally {
            configsLock.writeLock().unlock();
        }

        modulesLock.writeLock().lock();
        try {
            globalModules.clear();
        } finally {
            modulesLock.writeLock().unlock();
        }

        groupsLock.writeLock().lock();
        try {
            groups.clear();
        } finally {
            groupsLock.writeLock().unlock();
        }

        groupModulesLock.writeLock().lock();
        try {
            globalGroupModules.clear();
        } finally {
            groupModulesLock.writeLock().unlock();
        }
    }

    public Set<String> getConfigsByPrefix(String s) {
        configsLock.readLock().lock();
        try {
            return configs.keySet().stream()
                    .filter(name -> name.startsWith(s))
                    .collect(Collectors.toSet());
        } finally {
            configsLock.readLock().unlock();
        }
    }
}
