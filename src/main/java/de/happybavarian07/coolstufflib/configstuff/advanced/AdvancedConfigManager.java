package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers.*;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.ConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigFileType;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfigGroup;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.BaseConfigModule;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.GroupConfigModule;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AdvancedConfigManager {
    private final ConcurrentMap<UUID, AdvancedConfig> configs = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> nameToConfigIdMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, BaseConfigModule> globalModules = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> nameToModuleIdMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AdvancedConfigGroup> groups = new ConcurrentHashMap<>();
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private final File rootDirectory;
    private final ReadWriteLock configMapLock = new ReentrantReadWriteLock();
    private final ReadWriteLock globalModuleMapLock = new ReentrantReadWriteLock();
    private final ReadWriteLock groupMapLock = new ReentrantReadWriteLock();

    public AdvancedConfigManager(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        if (!initialized.get()) {
            ConfigLogger.initialize(rootDirectory);
            initialized.set(true);
            ConfigLogger.info("AdvancedConfigManager initialized", "AdvancedConfigManager", true);
        }
    }

    public AdvancedConfigManager(JavaPlugin plugin) {
        this.rootDirectory = plugin.getDataFolder();
        if (!initialized.get()) {
            ConfigLogger.initialize(plugin.getDataFolder());
            initialized.set(true);
            ConfigLogger.info("AdvancedConfigManager initialized", "AdvancedConfigManager", true);
        }
    }

    public AdvancedConfigManager() {
        this.rootDirectory = Path.of("").toFile();
        if (!initialized.get()) {
            ConfigLogger.initialize(rootDirectory);
            initialized.set(true);
            ConfigLogger.info("AdvancedConfigManager initialized", "AdvancedConfigManager", true);
        }
    }


    /**
     * Creates a new config with the given name and file.
     *
     * @param name the name of the config
     * @param file the file to save the config to
     * @param type the type of the config filee
     * @return the created config
     */
    public AdvancedConfig createPersistentConfig(String name, File file, ConfigFileType type, boolean registerGlobalModules) {
        return createPersistentConfig(name, file, getHandlerForType(type), registerGlobalModules);
    }

    /**
     * Creates a new persistent config with the given name, file, type, and handler.
     *
     * @param name    the name of the config
     * @param file    the file to save the config to
     * @param handler the handler for the config file
     * @return the created config
     */
    public AdvancedConfig createPersistentConfig(String name, File file, ConfigFileHandler handler, boolean registerGlobalModules) {
        configMapLock.writeLock().lock();
        if (registerGlobalModules)
            globalModuleMapLock.readLock().lock();
        try {
            if (nameToConfigIdMap.containsKey(name)) {
                throw new IllegalStateException("Config with name " + name + " already exists");
            }
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create config file: " + file.getAbsolutePath(), e);
                }
            }
            AdvancedConfig config = new AdvancedPersistentConfig(name, file, handler);
            UUID id = UUID.randomUUID();
            if (registerGlobalModules) {
                for (BaseConfigModule module : globalModules.values()) {
                    config.registerModule(module);
                }
            }
            configs.put(id, config);
            nameToConfigIdMap.put(name, id);
            return config;
        } finally {
            configMapLock.writeLock().unlock();
            if (registerGlobalModules)
                globalModuleMapLock.readLock().unlock();
        }
    }

    /**
     * Creates a new in-memory config with the given name.
     *
     * @param name the name of the config
     * @return the created config
     */
    public AdvancedConfig createInMemoryConfig(String name, boolean registerGlobalModules) {
        configMapLock.writeLock().lock();
        if (registerGlobalModules)
            globalModuleMapLock.readLock().lock();
        try {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Config name cannot be null or empty");
            }
            if (nameToConfigIdMap.containsKey(name)) {
                throw new IllegalStateException("Config with name " + name + " already exists");
            }
            AdvancedConfig config = new AdvancedInMemoryConfig(name);
            if (registerGlobalModules) {
                for (BaseConfigModule module : globalModules.values()) {
                    config.registerModule(module);
                }
            }
            UUID id = UUID.randomUUID();
            configs.put(id, config);
            nameToConfigIdMap.put(name, id);
            return config;
        } finally {
            configMapLock.writeLock().unlock();
            if (registerGlobalModules)
                globalModuleMapLock.readLock().unlock();
        }
    }

    /**
     * Registers a config with the given name and file.
     *
     * @param config                the config to register
     * @param registerGlobalModules whether to register global modules for this config
     */
    public void registerConfig(AdvancedConfig config, boolean registerGlobalModules) {
        if (config == null) {
            throw new IllegalArgumentException("Config is null");
        }
        configMapLock.writeLock().lock();
        if (registerGlobalModules)
            globalModuleMapLock.readLock().lock();
        try {
            if (configs.containsKey(nameToConfigIdMap.get(config.getName()))) {
                throw new IllegalArgumentException("Config already exists with the name: " + config.getName());
            }
            if (registerGlobalModules) {
                for (BaseConfigModule module : globalModules.values()) {
                    config.registerModule(module);
                }
            }
            UUID id = UUID.randomUUID();
            configs.put(id, config);
            nameToConfigIdMap.put(config.getName(), id);
        } finally {
            configMapLock.writeLock().unlock();
            if (registerGlobalModules)
                globalModuleMapLock.readLock().unlock();
        }
    }

    /**
     * Removes a config by its name.
     *
     * @param name the name of the config
     */
    public void unregisterConfig(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Config name cannot be null or empty");
        }
        configMapLock.writeLock().lock();
        try {
            if (nameToConfigIdMap.containsKey(name)) {
                UUID id = nameToConfigIdMap.get(name);
                configs.remove(id);
                nameToConfigIdMap.remove(name);
            }
        } finally {
            configMapLock.writeLock().unlock();
        }
    }

    private ConfigFileHandler getHandlerForType(ConfigFileType type) {
        switch (type) {
            case YAML -> {
                return new YamlConfigFileHandler();
            }
            case JSON -> {
                return new JsonConfigFileHandler();
            }
            case PROPERTIES -> {
                return new PropertiesConfigFileHandler();
            }
            case INI -> {
                return new IniConfigFileHandler();
            }
            case TOML -> {
                return new TomlConfigFileHandler();
            }
            case JSON5 -> {
                return new Json5ConfigFileHandler();
            }
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    /**
     * Gets a config by its name.
     *
     * @param name the name of the config
     * @return the config, or null if not found
     */
    public AdvancedConfig getConfig(String name) {
        configMapLock.readLock().lock();
        try {
            if (nameToConfigIdMap.containsKey(name)) {
                UUID id = nameToConfigIdMap.get(name);
                return configs.get(id);
            }
            return null;
        } finally {
            configMapLock.readLock().unlock();
        }
    }

    /**
     * Gets all configs.
     *
     * @return a list of all configs
     */
    public List<AdvancedConfig> getAllConfigs() {
        configMapLock.readLock().lock();
        try {
            return new ArrayList<>(configs.values());
        } finally {
            configMapLock.readLock().unlock();
        }
    }

    /**
     * Registers a module globally (all configs, present and future).
     *
     * @param module the module to register
     */
    public void registerGlobalModule(BaseConfigModule module) {
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        }
        globalModuleMapLock.writeLock().lock();
        configMapLock.writeLock().lock();
        try {
            if (!globalModules.containsValue(module)) {
                UUID id = UUID.randomUUID();
                globalModules.put(id, module);
                for (AdvancedConfig config : configs.values()) {
                    config.registerModule(module);
                }
                nameToModuleIdMap.put(module.getName(), id);
                if (!module.isEnabled()) {
                    module.enable();
                    module.setEnabled(true);
                }
            }
        } finally {
            globalModuleMapLock.writeLock().unlock();
            configMapLock.writeLock().unlock();
        }
    }

    /**
     * Unregisters a module globally (all configs, present and future).
     *
     * @param moduleName the module to unregister
     */
    public void unregisterGlobalModule(String moduleName) {
        if (moduleName.isEmpty()) {
            throw new IllegalArgumentException("Module cannot be null");
        }
        globalModuleMapLock.writeLock().lock();
        configMapLock.writeLock().lock();
        try {
            if (nameToModuleIdMap.containsKey(moduleName)) {
                UUID id = nameToModuleIdMap.entrySet().stream()
                        .filter(entry -> entry.getKey().equals(moduleName))
                        .map(Map.Entry::getValue)
                        .findFirst().orElse(null);
                if (id != null) {
                    BaseConfigModule module = globalModules.get(id);
                    if (module == null) {
                        throw new IllegalStateException("Module not found for ID: " + id);
                    }
                    if (module.isEnabled()) {
                        module.disable();
                        module.setEnabled(false);
                    }
                    globalModules.remove(id);
                    for (AdvancedConfig config : configs.values()) {
                        config.unregisterModule(module.getName());
                    }
                    nameToModuleIdMap.remove(module.getName());
                }
            }
        } finally {
            globalModuleMapLock.writeLock().unlock();
            configMapLock.writeLock().unlock();
        }
    }

    public Map<UUID, BaseConfigModule> getGlobalModules() {
        globalModuleMapLock.readLock().lock();
        try {
            return Collections.unmodifiableMap(globalModules);
        } finally {
            globalModuleMapLock.readLock().unlock();
        }
    }

    /**
     * Gets a global module by its name.
     *
     * @param name the name of the module
     * @return the module, or null if not found
     */
    public BaseConfigModule getGlobalModule(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Module name cannot be null or empty");
        }
        globalModuleMapLock.readLock().lock();
        try {
            UUID id = nameToModuleIdMap.get(name);
            if (id != null) {
                return globalModules.get(id);
            }
            return null;
        } finally {
            globalModuleMapLock.readLock().unlock();
        }
    }

    /**
     * Enables a module for a specific config.
     *
     * @param configName the name of the config
     * @param moduleName the module to enable
     */
    public void enableModuleForConfig(String configName, String moduleName) {
        AdvancedConfig config = getConfig(configName);
        if (config != null) {
            config.enableModule(moduleName);
        }
    }

    /**
     * Disables a module for a specific config.
     *
     * @param configName the name of the config
     * @param moduleName the module to disable
     */
    public void disableModuleForConfig(String configName, String moduleName) {
        AdvancedConfig config = getConfig(configName);
        if (config != null) {
            config.disableModule(moduleName);
        }
    }

    // the two methods for enabling/disabling global modules for all configs if they still have it which they shuold because its obviously a global module

    /**
     * Enables a global module for all configs.
     *
     * @param moduleName the name of the module to enable
     */
    public void enableGlobalModule(String moduleName) {
        if (moduleName == null || moduleName.isEmpty()) {
            throw new IllegalArgumentException("Module name cannot be null or empty");
        }
        globalModuleMapLock.readLock().lock();
        configMapLock.readLock().lock();
        try {
            UUID moduleId = nameToModuleIdMap.get(moduleName);
            if (moduleId != null) {
                BaseConfigModule module = globalModules.get(moduleId);
                if (module != null) {
                    for (AdvancedConfig config : configs.values()) {
                        config.registerModule(module);
                        config.enableModule(moduleName);
                    }
                }
            }
        } finally {
            globalModuleMapLock.readLock().unlock();
            configMapLock.readLock().unlock();
        }
    }

    /**
     * Disables a global module for all configs.
     *
     * @param moduleName the name of the module to disable
     */
    public void disableGlobalModule(String moduleName) {
        if (moduleName == null || moduleName.isEmpty()) {
            throw new IllegalArgumentException("Module name cannot be null or empty");
        }
        globalModuleMapLock.readLock().lock();
        configMapLock.readLock().lock();
        try {
            UUID moduleId = nameToModuleIdMap.get(moduleName);
            if (moduleId != null) {
                BaseConfigModule module = globalModules.get(moduleId);
                if (module != null) {
                    for (AdvancedConfig config : configs.values()) {
                        config.disableModule(moduleName);
                    }
                }
            }
        } finally {
            globalModuleMapLock.readLock().unlock();
            configMapLock.readLock().unlock();
        }
    }

    /**
     * Saves all configs.
     */
    public void saveAll() {
        configMapLock.readLock().lock();
        try {
            for (AdvancedConfig config : configs.values()) {
                config.save();
            }
        } finally {
            configMapLock.readLock().unlock();
        }
    }

    /**
     * Reloads all configs.
     */
    public void reloadAll() {
        configMapLock.readLock().lock();
        try {
            for (AdvancedConfig config : configs.values()) {
                config.reload();
            }
        } finally {
            configMapLock.readLock().unlock();
        }
    }

    /**
     * Returns the state of all configs and their modules.
     *
     * @return a map of config names to module states
     */
    public Map<String, Map<String, Object>> getAllConfigStates() {
        configMapLock.readLock().lock();
        try {
            Map<String, Map<String, Object>> states = new HashMap<>();
            for (AdvancedConfig config : configs.values()) {
                Map<String, Object> moduleStates = new HashMap<>(config.getModules());
                states.put(config.getName(), moduleStates);
            }
            return states;
        } finally {
            configMapLock.readLock().unlock();
        }
    }

    /**
     * Registers a config group.
     *
     * @param group the group to register
     */
    public void registerGroup(AdvancedConfigGroup group) {
        if (group == null || group.getName() == null || group.getName().isEmpty()) {
            throw new IllegalArgumentException("Group cannot be null and must have a valid name");
        }
        groupMapLock.writeLock().lock();
        try {
            if (!groups.containsKey(group.getName())) {
                groups.put(group.getName(), group);
            }
        } finally {
            groupMapLock.writeLock().unlock();
        }
    }

    /**
     * Unregisters a config group.
     *
     * @param name the name of the group
     */
    public void unregisterGroup(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }
        groupMapLock.writeLock().lock();
        try {
            groups.remove(name);
        } finally {
            groupMapLock.writeLock().unlock();
        }
    }

    /**
     * Gets a config group by its name.
     *
     * @param name the name of the group
     * @return the group, or null if not found
     */
    public AdvancedConfigGroup getGroup(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }
        groupMapLock.readLock().lock();
        try {
            return groups.get(name);
        } finally {
            groupMapLock.readLock().unlock();
        }
    }

    /**
     * Gets all config groups.
     *
     * @return a collection of all groups
     */
    public Collection<AdvancedConfigGroup> getGroups() {
        groupMapLock.readLock().lock();
        try {
            return Collections.unmodifiableCollection(groups.values());
        } finally {
            groupMapLock.readLock().unlock();
        }
    }

    /**
     * Creates and registers a new config group with the given name.
     *
     * @param name the group name
     * @return the created group
     */
    public AdvancedConfigGroup createEmptyGroup(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }
        groupMapLock.writeLock().lock();
        try {
            if (groups.containsKey(name)) {
                return groups.get(name);
            }
            AdvancedConfigGroup group = new DefaultAdvancedConfigGroup(name);
            registerGroup(group);
            return group;
        } finally {
            groupMapLock.writeLock().unlock();
        }
    }

    /**
     * Creates and registers a new config group with the given name and initial configs.
     *
     * @param name    the group name
     * @param configs the configs to add
     * @return the created group
     */
    public AdvancedConfigGroup createGroup(String name, Collection<AdvancedConfig> configs) {
        return createGroup(name, configs, new BaseConfigModule[0]);
    }

    /**
     * Creates and registers a new config group with the given name, initial configs, and optional default module.
     *
     * @param name    the group name
     * @param configs the configs to add
     * @return the created group
     */
    public AdvancedConfigGroup createGroup(String name, Collection<AdvancedConfig> configs, BaseConfigModule... defaultConfigModules) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }
        groupMapLock.writeLock().lock();
        try {
            if (groups.containsKey(name)) {
                return groups.get(name);
            }
            DefaultAdvancedConfigGroup group = new DefaultAdvancedConfigGroup(name);
            for (AdvancedConfig config : configs) {
                group.addConfig(config);
            }
            for (BaseConfigModule module : defaultConfigModules) {
                if (module != null) {
                    group.registerGroupModule(new DefaultGroupConfigModule(module));
                }
            }

            registerGroup(group);
            return group;
        } finally {
            groupMapLock.writeLock().unlock();
        }
    }

    public AdvancedConfigGroup getGroupByName(String groupName) {
        if (groupName == null || groupName.isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }
        groupMapLock.readLock().lock();
        try {
            return groups.get(groupName);
        } finally {
            groupMapLock.readLock().unlock();
        }
    }

    public boolean hasGroup(String groupName) {
        if (groupName == null || groupName.isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }
        groupMapLock.readLock().lock();
        try {
            return groups.containsKey(groupName);
        } finally {
            groupMapLock.readLock().unlock();
        }
    }

    /**
     * Registers a group module with the specified group.
     *
     * @param groupName the name of the group
     * @param module    the group config module to register
     */
    public void registerGroupModule(String groupName, GroupConfigModule module) {
        if (groupName == null || groupName.isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        }
        groupMapLock.readLock().lock();
        try {
            AdvancedConfigGroup group = groups.get(groupName);
            if (group != null) {
                group.registerGroupModule(module);
            } else {
                throw new IllegalStateException("Group not found: " + groupName);
            }
        } finally {
            groupMapLock.readLock().unlock();
        }
    }

    /**
     * Unregisters a group module from the specified group by module name.
     *
     * @param groupName  the name of the group
     * @param moduleName the name of the module to unregister
     */
    public void unregisterGroupModule(String groupName, String moduleName) {
        if (groupName == null || groupName.isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }
        if (moduleName == null || moduleName.isEmpty()) {
            throw new IllegalArgumentException("Module name cannot be null or empty");
        }
        groupMapLock.readLock().lock();
        try {
            AdvancedConfigGroup group = groups.get(groupName);
            if (group != null) {
                group.unregisterGroupModule(moduleName);
            } else {
                throw new IllegalStateException("Group not found: " + groupName);
            }
        } finally {
            groupMapLock.readLock().unlock();
        }
    }

    /**
     * Gets all group modules registered with the specified group.
     *
     * @param groupName the name of the group
     * @return map of module names to group config modules, or empty if not found
     */
    public Map<String, GroupConfigModule> getGroupModules(String groupName) {
        if (groupName == null || groupName.isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }
        groupMapLock.readLock().lock();
        try {
            AdvancedConfigGroup group = groups.get(groupName);
            if (group != null) {
                return group.getGroupModules();
            } else {
                return Collections.emptyMap();
            }
        } finally {
            groupMapLock.readLock().unlock();
        }
    }

    /**
     * Registers a normal config module as a group module for the specified group.
     * This will wrap the module name in a DefaultGroupConfigModule and register it in the group.
     *
     * @param groupName the name of the group
     * @param module    the config module to register
     */
    public void registerConfigModuleAsGroupModule(String groupName, BaseConfigModule module) {
        if (groupName == null || groupName.isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        }
        groupMapLock.readLock().lock();
        try {
            AdvancedConfigGroup group = groups.get(groupName);
            if (group != null) {
                group.registerGroupModule(new DefaultGroupConfigModule(module));
            } else {
                throw new IllegalStateException("Group not found: " + groupName);
            }
        } finally {
            groupMapLock.readLock().unlock();
        }
    }
}
