package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.CoolStuffLib;
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

public class AdvancedConfigManager {
    private final Map<UUID, AdvancedConfig> configs = new HashMap<>();
    private final Map<String, UUID> nameToConfigIdMap = new HashMap<>();
    private final Map<UUID, BaseConfigModule> globalModules = new HashMap<>();
    private final Map<String, UUID> nameToModuleIdMap = new HashMap<>();
    private final Map<String, AdvancedConfigGroup> groups = new HashMap<>();
    private static boolean initialized = false;
    private final File rootDirectory;

    public AdvancedConfigManager(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        if (!initialized) {
            ConfigLogger.initialize(rootDirectory);
            initialized = true;
            ConfigLogger.info("AdvancedConfigManager initialized", "AdvancedConfigManager", true);
        }
    }

    public AdvancedConfigManager(JavaPlugin plugin) {
        this.rootDirectory = plugin.getDataFolder();
        if (!initialized) {
            ConfigLogger.initialize(plugin.getDataFolder());
            initialized = true;
            ConfigLogger.info("AdvancedConfigManager initialized", "AdvancedConfigManager", true);
        }
    }

    public AdvancedConfigManager() {
        this.rootDirectory = Path.of("").toFile();
        if (!initialized) {
            ConfigLogger.initialize(rootDirectory);
            initialized = true;
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
    public AdvancedConfig createPersistentConfig(String name, File file, ConfigFileType type) {
        AdvancedConfig config = new AdvancedPersistentConfig(name, file, getHandlerForType(type));
        for (BaseConfigModule module : globalModules.values()) {
            config.registerModule(module);
        }
        UUID id = UUID.randomUUID();
        configs.put(id, config);
        nameToConfigIdMap.put(name, id);
        return config;
    }

    /**
     * Creates a new persistent config with the given name, file, type, and handler.
     *
     * @param name    the name of the config
     * @param file    the file to save the config to
     * @param handler the handler for the config file
     * @return the created config
     */
    public AdvancedConfig createPersistentConfig(String name, File file, ConfigFileHandler handler) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        AdvancedConfig config = new AdvancedPersistentConfig(name, file, handler);
        for (BaseConfigModule module : globalModules.values()) {
            config.registerModule(module);
        }
        UUID id = UUID.randomUUID();
        configs.put(id, config);
        nameToConfigIdMap.put(name, id);
        return config;
    }

    /**
     * Creates a new in-memory config with the given name.
     *
     * @param name the name of the config
     * @return the created config
     */
    public AdvancedConfig createInMemoryConfig(String name) {
        AdvancedConfig config = new AdvancedInMemoryConfig(name);
        for (BaseConfigModule module : globalModules.values()) {
            config.registerModule(module);
        }
        UUID id = UUID.randomUUID();
        configs.put(id, config);
        nameToConfigIdMap.put(name, id);
        return config;
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
    }

    /**
     * Removes a config by its name.
     *
     * @param name the name of the config
     */
    public void unregisterConfig(String name) {
        if (nameToConfigIdMap.containsKey(name)) {
            UUID id = nameToConfigIdMap.get(name);
            configs.remove(id);
            nameToConfigIdMap.remove(name);
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
        if (nameToConfigIdMap.containsKey(name)) {
            UUID id = nameToConfigIdMap.get(name);
            return configs.get(id);
        }
        return null;
    }

    /**
     * Gets all configs.
     *
     * @return a list of all configs
     */
    public List<AdvancedConfig> getAllConfigs() {
        return new ArrayList<>(configs.values());
    }

    /**
     * Registers a module globally (all configs, present and future).
     *
     * @param module the module to register
     */
    public void registerGlobalModule(BaseConfigModule module) {
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
    }

    /**
     * Unregisters a module globally (all configs, present and future).
     *
     * @param module the module to unregister
     */
    public void unregisterGlobalModule(BaseConfigModule module) {
        if (globalModules.containsValue(module)) {
            UUID id = globalModules.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(module))
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(null);
            if (id != null) {
                if (module.isEnabled()) {
                    module.disable();
                    module.setEnabled(false);
                }
                globalModules.remove(id);
                for (AdvancedConfig config : configs.values()) {
                    config.unregisterModule(module);
                }
                nameToModuleIdMap.remove(module.getName());
            }
        }
    }

    public Map<UUID, BaseConfigModule> getGlobalModules() {
        return Collections.unmodifiableMap(globalModules);
    }

    /**
     * Gets a global module by its name.
     *
     * @param name the name of the module
     * @return the module, or null if not found
     */
    public BaseConfigModule getGlobalModule(String name) {
        UUID id = nameToModuleIdMap.get(name);
        if (id != null) {
            return globalModules.get(id);
        }
        return null;
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
    }

    /**
     * Disables a global module for all configs.
     *
     * @param moduleName the name of the module to disable
     */
    public void disableGlobalModule(String moduleName) {
        UUID moduleId = nameToModuleIdMap.get(moduleName);
        if (moduleId != null) {
            BaseConfigModule module = globalModules.get(moduleId);
            if (module != null) {
                for (AdvancedConfig config : configs.values()) {
                    config.disableModule(moduleName);
                }
            }
        }
    }

    /**
     * Saves all configs.
     */
    public void saveAll() {
        for (AdvancedConfig config : configs.values()) config.save();
    }

    /**
     * Reloads all configs.
     */
    public void reloadAll() {
        for (AdvancedConfig config : configs.values()) config.reload();
    }

    /**
     * Returns the state of all configs and their modules.
     *
     * @return a map of config names to module states
     */
    public Map<String, Map<String, Object>> getAllConfigStates() {
        Map<String, Map<String, Object>> states = new HashMap<>();
        for (AdvancedConfig config : configs.values()) {
            Map<String, Object> moduleStates = new HashMap<>(config.getModules());
            states.put(config.getName(), moduleStates);
        }
        return states;
    }

    /**
     * Registers a config group.
     *
     * @param group the group to register
     */
    public void registerGroup(AdvancedConfigGroup group) {
        groups.put(group.getName(), group);
    }

    /**
     * Unregisters a config group.
     *
     * @param name the name of the group
     */
    public void unregisterGroup(String name) {
        groups.remove(name);
    }

    /**
     * Gets a config group by its name.
     *
     * @param name the name of the group
     * @return the group, or null if not found
     */
    public AdvancedConfigGroup getGroup(String name) {
        return groups.get(name);
    }

    /**
     * Gets all config groups.
     *
     * @return a collection of all groups
     */
    public Collection<AdvancedConfigGroup> getGroups() {
        return groups.values();
    }

    /**
     * Creates and registers a new config group with the given name.
     *
     * @param name the group name
     * @return the created group
     */
    public AdvancedConfigGroup createEmptyGroup(String name) {
        if (groups.containsKey(name)) {
            return groups.get(name);
        }
        AdvancedConfigGroup group = new DefaultAdvancedConfigGroup(name);
        registerGroup(group);
        return group;
    }

    /**
     * Creates and registers a new config group with the given name and initial configs.
     *
     * @param name    the group name
     * @param configs the configs to add
     * @return the created group
     */
    public AdvancedConfigGroup createGroup(String name, Collection<AdvancedConfig> configs) {
        if (groups.containsKey(name)) {
            return groups.get(name);
        }
        DefaultAdvancedConfigGroup group = new DefaultAdvancedConfigGroup(name);
        for (AdvancedConfig config : configs) {
            group.addConfig(config);
        }
        registerGroup(group);
        return group;
    }

    /**
     * Creates and registers a new config group with the given name, initial configs, and optional default module.
     *
     * @param name    the group name
     * @param configs the configs to add
     * @return the created group
     */
    public AdvancedConfigGroup createGroup(String name, Collection<AdvancedConfig> configs, BaseConfigModule... defaultConfigModules) {
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
    }

    public AdvancedConfigGroup getGroupByName(String groupName) {
        return groups.get(groupName);
    }

    public boolean hasGroup(String groupName) {
        return groups.containsKey(groupName);
    }

    /**
     * Registers a group module with the specified group.
     *
     * @param groupName the name of the group
     * @param module    the group config module to register
     */
    public void registerGroupModule(String groupName, GroupConfigModule module) {
        AdvancedConfigGroup group = groups.get(groupName);
        if (group != null) {
            group.registerGroupModule(module);
        }
    }

    /**
     * Unregisters a group module from the specified group by module name.
     *
     * @param groupName  the name of the group
     * @param moduleName the name of the module to unregister
     */
    public void unregisterGroupModule(String groupName, String moduleName) {
        AdvancedConfigGroup group = groups.get(groupName);
        if (group != null) {
            group.unregisterGroupModule(moduleName);
        }
    }

    /**
     * Gets all group modules registered with the specified group.
     *
     * @param groupName the name of the group
     * @return map of module names to group config modules, or empty if not found
     */
    public Map<String, GroupConfigModule> getGroupModules(String groupName) {
        AdvancedConfigGroup group = groups.get(groupName);
        if (group != null) {
            return group.getGroupModules();
        }
        return Map.of();
    }

    /**
     * Registers a normal config module as a group module for the specified group.
     * This will wrap the module name in a DefaultGroupConfigModule and register it in the group.
     *
     * @param groupName the name of the group
     * @param module    the config module to register
     */
    public void registerConfigModuleAsGroupModule(String groupName, BaseConfigModule module) {
        AdvancedConfigGroup group = groups.get(groupName);
        if (group != null) {
            group.registerGroupModule(new DefaultGroupConfigModule(module));
        }
    }
}
