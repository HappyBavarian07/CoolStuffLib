package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfigGroup;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.GroupConfigModule;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Implementation of AdvancedConfigGroup that manages multiple advanced configurations
 * as a cohesive unit with support for group-level operations and modular extensions.</p>
 *
 * <p>This implementation provides:</p>
 * <ul>
 * <li>Thread-safe configuration management with concurrent access</li>
 * <li>Group-level value operations across all configurations</li>
 * <li>Modular extension system for group-specific functionality</li>
 * <li>Event notifications for configuration lifecycle changes</li>
 * </ul>
 *
 * <pre><code>
 * AdvancedConfigGroup group = new AdvancedConfigGroupImpl("app-configs");
 * group.addConfig(databaseConfig);
 * group.addConfig(cacheConfig);
 * group.setValueInAll("debug.enabled", true);
 * </code></pre>
 */
public class AdvancedConfigGroupImpl implements AdvancedConfigGroup {
    private final String name;
    private final Map<String, AdvancedConfig> configs;
    private final Map<String, GroupConfigModule> groupModules;

    public AdvancedConfigGroupImpl(String name) {
        this.name = name;
        this.configs = new ConcurrentHashMap<>();
        this.groupModules = new ConcurrentHashMap<>();
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
     * <p>Adds a configuration to this group and notifies registered modules
     * of the addition. Configurations are indexed by their names.</p>
     *
     * <pre><code>
     * AdvancedConfig dbConfig = new AdvancedPersistentConfig("database", dbFile);
     * group.addConfig(dbConfig);
     * </code></pre>
     *
     * @param config the configuration to add to the group
     */
    @Override
    public void addConfig(AdvancedConfig config) {
        if (config != null) {
            configs.put(config.getName(), config);
            notifyModulesConfigAdded(config);
        }
    }

    /**
     * <p>Removes a configuration from this group by reference and notifies
     * registered modules of the removal.</p>
     *
     * <pre><code>
     * group.removeConfig(databaseConfig);
     * </code></pre>
     *
     * @param config the configuration instance to remove
     */
    @Override
    public void removeConfig(AdvancedConfig config) {
        if (config != null) {
            removeConfig(config.getName());
        }
    }

    /**
     * <p>Removes a configuration from this group by name and notifies
     * registered modules of the removal.</p>
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
            notifyModulesConfigRemoved(config);
        }
    }

    /**
     * <p>Retrieves a configuration from this group by its name.</p>
     *
     * <pre><code>
     * AdvancedConfig dbConfig = group.getConfig("database");
     * if (dbConfig != null) {
     *     String host = dbConfig.getString("host");
     * }
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
     * if (group.containsConfig("database")) {
     *     // Database configuration is present
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
     *     // Configuration instance is in this group
     * }
     * </code></pre>
     *
     * @param config the configuration instance to check for
     * @return true if this configuration is in the group, false otherwise
     */
    @Override
    public boolean containsConfig(AdvancedConfig config) {
        return config != null && configs.containsValue(config);
    }

    /**
     * <p>Retrieves a typed value from a specific configuration in this group.</p>
     *
     * <pre><code>
     * Integer timeout = group.getConfigValue("database", "connection.timeout", Integer.class);
     * </code></pre>
     *
     * @param configName the name of the configuration to read from
     * @param path the configuration path to retrieve
     * @param type the expected type of the value
     * @param <T> the type parameter
     * @return the typed value, or null if not found or wrong type
     */
    @Override
    public <T> T getConfigValue(String configName, String path, Class<T> type) {
        AdvancedConfig config = getConfig(configName);
        return config != null ? config.getValue(path, type) : null;
    }

    /**
     * <p>Retrieves a typed value from a specific configuration with a default value.</p>
     *
     * <pre><code>
     * Integer timeout = group.getConfigValue("database", "timeout", 30, Integer.class);
     * </code></pre>
     *
     * @param configName the name of the configuration to read from
     * @param path the configuration path to retrieve
     * @param defaultValue the value to return if not found
     * @param type the expected type of the value
     * @param <T> the type parameter
     * @return the typed value, or defaultValue if not found
     */
    @Override
    public <T> T getConfigValue(String configName, String path, T defaultValue, Class<T> type) {
        T value = getConfigValue(configName, path, type);
        return value != null ? value : defaultValue;
    }

    /**
     * <p>Retrieves values at the specified path from all configurations in this group
     * that contain the path, returning a map of configuration names to values.</p>
     *
     * <pre><code>
     * Map&lt;String, Object&gt; allTimeouts = group.getValuesFromAll("connection.timeout");
     * // Returns {"database": 30, "cache": 15, ...}
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
     * <p>Retrieves typed values at the specified path from all configurations
     * in this group, filtering by the specified type.</p>
     *
     * <pre><code>
     * Map&lt;String, Integer&gt; timeouts = group.getValuesFromAll("timeout", Integer.class);
     * </code></pre>
     *
     * @param path the configuration path to retrieve from all configs
     * @param type the expected type of values
     * @param <T> the type parameter
     * @return map of configuration names to their typed values at the path
     */
    @Override
    public <T> Map<String, T> getValuesFromAll(String path, Class<T> type) {
        Map<String, T> result = new HashMap<>();
        for (Map.Entry<String, AdvancedConfig> entry : configs.entrySet()) {
            T value = entry.getValue().getValue(path, type);
            if (value != null) {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    /**
     * <p>Retrieves the first non-null typed value found at the specified path
     * across all configurations in this group.</p>
     *
     * <pre><code>
     * String defaultHost = group.getFirstValue("fallback.host", String.class);
     * </code></pre>
     *
     * @param path the configuration path to search for
     * @param type the expected type of the value
     * @param <T> the type parameter
     * @return the first typed value found, or null if none exist
     */
    @Override
    public <T> T getFirstValue(String path, Class<T> type) {
        for (AdvancedConfig config : configs.values()) {
            T value = config.getValue(path, type);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * <p>Retrieves the first non-null typed value found at the specified path
     * across all configurations, with a fallback default value.</p>
     *
     * <pre><code>
     * String host = group.getFirstValue("host", "localhost", String.class);
     * </code></pre>
     *
     * @param path the configuration path to search for
     * @param defaultValue the value to return if no value is found
     * @param type the expected type of the value
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
     * if (group.containsKeyInAny("debug.enabled")) {
     *     // At least one config has this debug setting
     * }
     * </code></pre>
     *
     * @param path the configuration path to check for
     * @return true if any configuration contains this path, false otherwise
     */
    @Override
    public boolean containsKeyInAny(String path) {
        for (AdvancedConfig config : configs.values()) {
            if (config.containsKey(path)) {
                return true;
            }
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
     * <p>Sets the specified value at the given path in all configurations
     * within this group. Useful for applying global settings.</p>
     *
     * <pre><code>
     * group.setValueInAll("debug.enabled", true);
     * // All configs now have debug.enabled = true
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
     * <p>Registers a group module that can extend functionality across all
     * configurations in this group. Modules are initialized upon registration.</p>
     *
     * <pre><code>
     * ValidationGroupModule validator = new ValidationGroupModule();
     * group.registerGroupModule("validation", validator);
     * </code></pre>
     *
     * @param name the unique name for this module
     * @param module the group module to register
     */
    @Override
    public void registerGroupModule(String name, GroupConfigModule module) {
        if (name != null && module != null && !groupModules.containsKey(name)) {
            module.initialize(this);
            groupModules.put(name, module);
        }
    }

    /**
     * <p>Unregisters and cleans up a group module by name.</p>
     *
     * <pre><code>
     * group.unregisterGroupModule("validation");
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
     * GroupConfigModule validator = group.getGroupModule("validation");
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
     * if (group.hasGroupModule("validation")) {
     *     // Validation module is available
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
     * <p>Enables a registered group module, allowing it to process group events.</p>
     *
     * <pre><code>
     * group.enableGroupModule("validation");
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
     * <p>Disables a registered group module, preventing it from processing group events.</p>
     *
     * <pre><code>
     * group.disableGroupModule("validation");
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

    private void notifyModulesConfigAdded(AdvancedConfig config) {
        for (GroupConfigModule module : groupModules.values()) {
            if (module.isEnabled() && module.appliesTo(config)) {
                module.onConfigAdded(config);
            }
        }
    }

    private void notifyModulesConfigRemoved(AdvancedConfig config) {
        for (GroupConfigModule module : groupModules.values()) {
            if (module.isEnabled() && module.appliesTo(config)) {
                module.onConfigRemoved(config);
            }
        }
    }

    /**
     * <p>Compares this configuration group with another object for equality.
     * Groups are considered equal if they have the same name.</p>
     *
     * <pre><code>
     * boolean same = group1.equals(group2);
     * </code></pre>
     *
     * @param o the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdvancedConfigGroupImpl)) return false;
        AdvancedConfigGroupImpl that = (AdvancedConfigGroupImpl) o;
        return name.equals(that.name);
    }

    /**
     * <p>Returns the hash code for this configuration group based on its name.</p>
     *
     * <pre><code>
     * int hash = group.hashCode();
     * </code></pre>
     *
     * @return the hash code value for this group
     */
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    /**
     * <p>Returns a string representation of this configuration group including
     * its name and the number of configurations it contains.</p>
     *
     * <pre><code>
     * String info = group.toString();
     * // Returns "ConfigGroup[name=app-configs, configs=3]"
     * </code></pre>
     *
     * @return a string representation of this group
     */
    @Override
    public String toString() {
        return "ConfigGroup[name=" + name + ", configs=" + configs.size() + "]";
    }
}
