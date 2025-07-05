package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfigGroup;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.BaseConfigModule;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.GroupConfigModule;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Default implementation of AdvancedConfigGroup that provides comprehensive configuration
 * group management with automatic default module registration and group-level operations.</p>
 *
 * <p>This implementation extends basic group functionality with:</p>
 * <ul>
 * <li>Automatic registration of default modules to new configurations</li>
 * <li>Thread-safe configuration management with concurrent access</li>
 * <li>Group-level module management and lifecycle handling</li>
 * <li>Bulk operations across all configurations in the group</li>
 * </ul>
 *
 * <pre><code>
 * BaseConfigModule[] defaults = {new ValidationModule(), new BackupModule()};
 * DefaultAdvancedConfigGroup group = new DefaultAdvancedConfigGroup("app", defaults);
 * group.addConfig(databaseConfig); // Automatically gets default modules
 * </code></pre>
 */
class DefaultAdvancedConfigGroup implements AdvancedConfigGroup {
    private final String name;
    private final Map<String, AdvancedConfig> configs = new ConcurrentHashMap<>();
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
    public Map<String, AdvancedConfig> getConfigs() {
        return Collections.unmodifiableMap(configs);
    }

    /**
     * <p>Adds a configuration to this group and automatically registers all default modules.
     * Default modules are initialized and enabled if they're in the correct state.</p>
     *
     * <pre><code>
     * group.addConfig(userConfig);
     * // userConfig now has all default modules registered and enabled
     * </code></pre>
     *
     * @param config the configuration to add to the group
     */
    @Override
    public void addConfig(AdvancedConfig config) {
        configs.put(config.getName(), config);
        // Register default modules if specified
        for (String moduleName : defaultModules.keySet()) {
            if (!config.hasModule(moduleName)) {
                BaseConfigModule module = defaultModules.get(moduleName);
                if (module != null) {
                    config.registerModule(module);
                    module.initialize(config);
                    if (module.getState() == BaseConfigModule.ModuleState.INITIALIZED) {
                        module.enable();
                    }
                }
            }
        }
        groupModules.values().forEach(m -> m.onConfigAdded(config));
    }

    /**
     * <p>Removes a configuration from this group by reference and notifies group modules.</p>
     *
     * <pre><code>
     * group.removeConfig(databaseConfig);
     * </code></pre>
     *
     * @param config the configuration instance to remove
     */
    @Override
    public void removeConfig(AdvancedConfig config) {
        removeConfig(config.getName());
    }

    /**
     * <p>Removes a configuration from this group by name and notifies group modules.</p>
     *
     * <pre><code>
     * group.removeConfig("database");
     * </code></pre>
     *
     * @param configName the name of the configuration to remove
     */
    @Override
    public void removeConfig(String configName) {
        AdvancedConfig config = configs.remove(configName);
        if (config != null) {
            groupModules.values().forEach(m -> m.onConfigRemoved(config));
        }
    }

    /**
     * <p>Retrieves a configuration from this group by its name.</p>
     *
     * <pre><code>
     * AdvancedConfig dbConfig = group.getConfig("database");
     * </code></pre>
     *
     * @param name the name of the configuration to retrieve
     * @return the configuration instance, or null if not found
     */
    @Override
    public AdvancedConfig getConfig(String name) {
        return configs.get(name);
    }

    /**
     * <p>Checks if this group contains a configuration with the specified name.</p>
     *
     * <pre><code>
     * if (group.containsConfig("cache")) {
     *     // Cache configuration exists
     * }
     * </code></pre>
     *
     * @param name the name to check for
     * @return true if a configuration with this name exists, false otherwise
     */
    @Override
    public boolean containsConfig(String name) {
        return configs.containsKey(name);
    }

    /**
     * <p>Checks if this group contains the specified configuration instance.</p>
     *
     * <pre><code>
     * if (group.containsConfig(myConfig)) {
     *     // Configuration is in this group
     * }
     * </code></pre>
     *
     * @param config the configuration instance to check for
     * @return true if this configuration is in the group, false otherwise
     */
    @Override
    public boolean containsConfig(AdvancedConfig config) {
        return configs.containsValue(config);
    }

    /**
     * <p>Retrieves a typed value from a specific configuration with type casting.</p>
     *
     * <pre><code>
     * Integer port = group.getConfigValue("database", "port", Integer.class);
     * </code></pre>
     *
     * @param configName the name of the configuration to read from
     * @param path the configuration path to retrieve
     * @param type the expected type class
     * @param <T> the type parameter
     * @return the typed value, or null if not found or wrong type
     */
    @Override
    public <T> T getConfigValue(String configName, String path, Class<T> type) {
        AdvancedConfig config = getConfig(configName);
        if (config != null) {
            Object value = config.get(path);
            if (type.isInstance(value)) {
                return type.cast(value);
            }
        }
        return null;
    }

    /**
     * <p>Retrieves a typed value from a specific configuration with fallback default.</p>
     *
     * <pre><code>
     * Integer timeout = group.getConfigValue("database", "timeout", 30, Integer.class);
     * </code></pre>
     *
     * @param configName the name of the configuration to read from
     * @param path the configuration path to retrieve
     * @param defaultValue the value to return if not found
     * @param type the expected type class
     * @param <T> the type parameter
     * @return the typed value, or defaultValue if not found
     */
    @Override
    public <T> T getConfigValue(String configName, String path, T defaultValue, Class<T> type) {
        T value = getConfigValue(configName, path, type);
        return value != null ? value : defaultValue;
    }

    /**
     * <p>Retrieves values at the specified path from all configurations in the group.</p>
     *
     * <pre><code>
     * Map&lt;String, Object&gt; allPorts = group.getValuesFromAll("server.port");
     * </code></pre>
     *
     * @param path the configuration path to retrieve from all configs
     * @return map of configuration names to their values at the path
     */
    @Override
    public Map<String, Object> getValuesFromAll(String path) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, AdvancedConfig> entry : configs.entrySet()) {
            Object value = entry.getValue().get(path);
            if (value != null) {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    /**
     * <p>Retrieves typed values at the specified path from all configurations with type filtering.</p>
     *
     * <pre><code>
     * Map&lt;String, Integer&gt; ports = group.getValuesFromAll("port", Integer.class);
     * </code></pre>
     *
     * @param path the configuration path to retrieve from all configs
     * @param type the expected type class for filtering
     * @param <T> the type parameter
     * @return map of configuration names to their typed values at the path
     */
    @Override
    public <T> Map<String, T> getValuesFromAll(String path, Class<T> type) {
        Map<String, T> result = new HashMap<>();
        for (Map.Entry<String, AdvancedConfig> entry : configs.entrySet()) {
            Object value = entry.getValue().get(path);
            if (type.isInstance(value)) {
                result.put(entry.getKey(), type.cast(value));
            }
        }
        return result;
    }

    /**
     * <p>Retrieves the first typed value found at the specified path across all configurations.</p>
     *
     * <pre><code>
     * String defaultTheme = group.getFirstValue("ui.theme", String.class);
     * </code></pre>
     *
     * @param path the configuration path to search for
     * @param type the expected type class
     * @param <T> the type parameter
     * @return the first typed value found, or null if none exist
     */
    @Override
    public <T> T getFirstValue(String path, Class<T> type) {
        for (AdvancedConfig config : configs.values()) {
            Object value = config.get(path);
            if (type.isInstance(value)) {
                return type.cast(value);
            }
        }
        return null;
    }

    /**
     * <p>Retrieves the first typed value found with a fallback default value.</p>
     *
     * <pre><code>
     * String theme = group.getFirstValue("theme", "dark", String.class);
     * </code></pre>
     *
     * @param path the configuration path to search for
     * @param defaultValue the value to return if no value is found
     * @param type the expected type class
     * @param <T> the type parameter
     * @return the first typed value found, or defaultValue if none exist
     */
    @Override
    public <T> T getFirstValue(String path, T defaultValue, Class<T> type) {
        T value = getFirstValue(path, type);
        return value != null ? value : defaultValue;
    }

    /**
     * <p>Checks if any configuration in this group contains the specified path.</p>
     *
     * <pre><code>
     * if (group.containsKeyInAny("experimental.feature")) {
     *     // At least one config has this setting
     * }
     * </code></pre>
     *
     * @param path the configuration path to check for
     * @return true if any configuration contains this path, false otherwise
     */
    @Override
    public boolean containsKeyInAny(String path) {
        for (AdvancedConfig config : configs.values()) {
            if (config.containsKey(path)) return true;
        }
        return false;
    }

    @Override
    public Set<String> getAllKeys() {
        Set<String> keys = new HashSet<>();
        for (AdvancedConfig config : configs.values()) {
            keys.addAll(config.getKeys(true));
        }
        return keys;
    }

    /**
     * <p>Sets the specified value at the given path in all configurations within this group.</p>
     *
     * <pre><code>
     * group.setValueInAll("maintenance.mode", true);
     * </code></pre>
     *
     * @param path the configuration path to set in all configs
     * @param value the value to set at the specified path
     */
    @Override
    public void setValueInAll(String path, Object value) {
        for (AdvancedConfig config : configs.values()) {
            config.set(path, value);
        }
    }

    @Override
    public Map<String, GroupConfigModule> getGroupModules() {
        return Collections.unmodifiableMap(groupModules);
    }

    /**
     * <p>Registers a group module for managing cross-configuration functionality.</p>
     *
     * <pre><code>
     * SyncGroupModule syncModule = new SyncGroupModule();
     * group.registerGroupModule("sync", syncModule);
     * </code></pre>
     *
     * @param name the unique name for this module
     * @param module the group module to register
     */
    @Override
    public void registerGroupModule(String name, GroupConfigModule module) {
        if (module != null) {
            groupModules.put(name, module);
            module.initialize(this);
        }
    }

    /**
     * <p>Unregisters and cleans up a group module by name.</p>
     *
     * <pre><code>
     * group.unregisterGroupModule("sync");
     * </code></pre>
     *
     * @param name the name of the module to unregister
     */
    @Override
    public void unregisterGroupModule(String name) {
        GroupConfigModule module = groupModules.remove(name);
        if (module != null) {
            module.cleanup();
        }
    }

    /**
     * <p>Retrieves a registered group module by name.</p>
     *
     * <pre><code>
     * GroupConfigModule syncModule = group.getGroupModule("sync");
     * </code></pre>
     *
     * @param name the name of the module to retrieve
     * @return the group module, or null if not found
     */
    @Override
    public GroupConfigModule getGroupModule(String name) {
        return groupModules.get(name);
    }

    /**
     * <p>Checks if a group module with the specified name is registered.</p>
     *
     * <pre><code>
     * if (group.hasGroupModule("sync")) {
     *     // Sync module is available
     * }
     * </code></pre>
     *
     * @param name the name of the module to check for
     * @return true if the module is registered, false otherwise
     */
    @Override
    public boolean hasGroupModule(String name) {
        return groupModules.containsKey(name);
    }

    /**
     * <p>Enables a registered group module.</p>
     *
     * <pre><code>
     * group.enableGroupModule("sync");
     * </code></pre>
     *
     * @param name the name of the module to enable
     */
    @Override
    public void enableGroupModule(String name) {
        GroupConfigModule module = getGroupModule(name);
        if (module != null) {
            module.enable();
        }
    }

    /**
     * <p>Disables a registered group module.</p>
     *
     * <pre><code>
     * group.disableGroupModule("sync");
     * </code></pre>
     *
     * @param name the name of the module to disable
     */
    @Override
    public void disableGroupModule(String name) {
        GroupConfigModule module = getGroupModule(name);
        if (module != null) {
            module.disable();
        }
    }

    /**
     * <p>Registers a default module that will be automatically added to all new configurations.</p>
     *
     * <pre><code>
     * ValidationModule validator = new ValidationModule();
     * group.registerDefaultModule(validator);
     * // All future configs will automatically get the validator
     * </code></pre>
     *
     * @param module the module to register as a default for new configurations
     */
    public void registerDefaultModule(BaseConfigModule module) {
        if (module != null) {
            defaultModules.put(module.getName(), module);
            // Register this module for all existing configs
            for (AdvancedConfig config : configs.values()) {
                if (!config.hasModule(module.getName())) {
                    config.registerModule(module);
                    module.initialize(config);
                    if (module.getState() == BaseConfigModule.ModuleState.INITIALIZED) {
                        module.enable();
                    }
                }
            }
        }
    }

    /**
     * <p>Unregisters a default module by name.</p>
     *
     * <pre><code>
     * group.unregisterDefaultModule("validation");
     * </code></pre>
     *
     * @param name the name of the default module to unregister
     */
    public void unregisterDefaultModule(String name) {
        defaultModules.remove(name);
    }

    /**
     * <p>Retrieves a default module by name.</p>
     *
     * <pre><code>
     * BaseConfigModule validator = group.getDefaultModuleByName("validation");
     * </code></pre>
     *
     * @param name the name of the default module to retrieve
     * @return the default module, or null if not found
     */
    public BaseConfigModule getDefaultModuleByName(String name) {
        return defaultModules.get(name);
    }

    public Set<String> getDefaultModuleNames() {
        return Collections.unmodifiableSet(defaultModules.keySet());
    }

    /**
     * <p>Checks if a default module with the specified name is registered.</p>
     *
     * <pre><code>
     * if (group.hasDefaultModule("backup")) {
     *     // Backup module is set as default
     * }
     * </code></pre>
     *
     * @param name the name of the default module to check for
     * @return true if the default module is registered, false otherwise
     */
    public boolean hasDefaultModule(String name) {
        return defaultModules.containsKey(name);
    }

    /**
     * <p>Returns a string representation of this configuration group with summary information.</p>
     *
     * <pre><code>
     * String info = group.toString();
     * // Returns "AdvancedConfigGroup{name='app', configs=3, groupModules=2}"
     * </code></pre>
     *
     * @return a string representation of this group
     */
    @Override
    public String toString() {
        return "AdvancedConfigGroup{name='" + name + "', configs=" + configs.size() + ", groupModules=" + groupModules.size() + "}";
    }
}
