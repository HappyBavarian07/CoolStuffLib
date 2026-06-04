package de.happybavarian07.coolstufflib.configstuff.advanced.interfaces;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>Interface for managing multiple AdvancedConfig instances as a cohesive group,
 * providing unified operations across configurations and group-level module management.</p>
 *
 * <p>Configuration groups enable:</p>
 * <ul>
 *   <li>Centralized management of related configurations</li>
 *   <li>Cross-configuration value queries and operations</li>
 *   <li>Group-level module system for shared functionality</li>
 *   <li>Bulk operations across multiple configurations</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre><code>
 * AdvancedConfigGroup serverGroup = configManager.getGroup("servers");
 * Map&lt;String, Object&gt; allPorts = serverGroup.getValuesFromAll("server.port");
 * serverGroup.setValueInAll("logging.level", "INFO");
 * </code></pre>
 */
public interface AdvancedConfigGroup {

    /**
     * <p>Gets the unique name identifier for this configuration group.</p>
     *
     * <pre><code>
     * String groupName = configGroup.getName();
     * </code></pre>
     *
     * @return the group name, never null
     */
    String getName();

    /**
     * <p>Gets all configurations managed by this group.</p>
     *
     * <pre><code>
     * Map&lt;String, AdvancedConfig&gt; configs = group.getConfigs();
     * for (AdvancedConfig config : configs.values()) {
     *     config.save();
     * }
     * </code></pre>
     *
     * @return map of configuration names to configuration instances
     */
    Map<String, AdvancedConfig> getConfigs();

    /**
     * <p>Adds a configuration to this group for unified management.</p>
     *
     * <pre><code>
     * AdvancedConfig webConfig = new AdvancedPersistentConfig("web", webFile, YAML);
     * configGroup.addConfig(webConfig);
     * </code></pre>
     *
     * @param config the configuration to add
     * @throws IllegalArgumentException if config with same name already exists
     */
    void addConfig(AdvancedConfig config);

    /**
     * <p>Removes a specific configuration from this group by reference.</p>
     *
     * <pre><code>
     * AdvancedConfig webConfig = group.getConfig("web");
     * if (webConfig != null) {
     *     group.removeConfig(webConfig);
     * }
     * </code></pre>
     *
     * @param config the configuration instance to remove
     */
    void removeConfig(AdvancedConfig config);

    /**
     * <p>Removes a specific configuration from this group by name.</p>
     *
     * <pre><code>
     * group.removeConfig("database");
     * </code></pre>
     *
     * @param configName the name of the configuration to remove
     */
    void removeConfig(String configName);

    /**
     * <p>Retrieves a specific configuration by name from this group.</p>
     *
     * <pre><code>
     * AdvancedConfig dbConfig = group.getConfig("database");
     * if (dbConfig != null) {
     *     String host = dbConfig.getString("host");
     * }
     * </code></pre>
     *
     * @param name the configuration name
     * @return the configuration instance, or null if not found
     */
    AdvancedConfig getConfig(String name);

    /**
     * <p>Checks if this group contains a configuration with the specified name.</p>
     *
     * <pre><code>
     * if (group.containsConfig("database")) {
     *     AdvancedConfig db = group.getConfig("database");
     * }
     * </code></pre>
     *
     * @param name the configuration name to check
     * @return true if a configuration with this name exists in the group, false otherwise
     */
    boolean containsConfig(String name);

    /**
     * <p>Checks if this group contains the specified configuration instance.</p>
     *
     * <pre><code>
     * AdvancedConfig db = new AdvancedPersistentConfig(...);
     * group.addConfig(db);
     * boolean exists = group.containsConfig(db); // true
     * </code></pre>
     *
     * @param config the configuration instance to check for
     * @return true if this configuration is managed by the group, false otherwise
     */
    boolean containsConfig(AdvancedConfig config);

    /**
     * <p>Gets a typed value from a specific configuration within the group.</p>
     *
     * <pre><code>
     * Integer webPort = group.getConfigValue("web", "server.port", Integer.class);
     * </code></pre>
     *
     * @param configName the name of the configuration
     * @param path the path to the value
     * @param type the expected value type
     * @param <T> the value type
     * @return the typed value, or null if not found
     */
    <T> T getConfigValue(String configName, String path, Class<T> type);

    /**
     * <p>Gets a typed value from a specific configuration within the group with a default fallback.</p>
     *
     * <pre><code>
     * Integer port = group.getConfigValue("web", "server.port", 80, Integer.class);
     * </code></pre>
     *
     * @param configName the name of the configuration
     * @param path the path to the value
     * @param defaultValue the default value if not found
     * @param type the expected value type
     * @param <T> the value type
     * @return the typed value, or the default value if not found
     */
    <T> T getConfigValue(String configName, String path, T defaultValue, Class<T> type);

    /**
     * <p>Retrieves values at the same path from all configurations in the group.</p>
     *
     * <pre><code>
     * Map&lt;String, Object&gt; allPorts = group.getValuesFromAll("server.port");
     * // Returns {"web": 8080, "api": 8081, "admin": 8082}
     * </code></pre>
     *
     * @param path the configuration path to query
     * @return map of configuration names to their values at the specified path
     */
    Map<String, Object> getValuesFromAll(String path);

    /**
     * <p>Retrieves typed values at the same path from all configurations in the group.</p>
     *
     * <pre><code>
     * Map&lt;String, Integer&gt; allPorts = group.getValuesFromAll("server.port", Integer.class);
     * // Returns {"web": 8080, "api": 8081}
     * </code></pre>
     *
     * @param path the configuration path to query
     * @param type the expected value type for conversion
     * @param <T> the value type
     * @return map of configuration names to their typed values at the specified path
     */
    <T> Map<String, T> getValuesFromAll(String path, Class<T> type);

    /**
     * <p>Gets the first non-null value found at the specified path across all configurations.</p>
     *
     * <pre><code>
     * String logLevel = group.getFirstValue("logging.level", String.class);
     * </code></pre>
     *
     * @param path the configuration path to search
     * @param type the expected value type
     * @param <T> the value type
     * @return the first value found, or null if none exist
     */
    <T> T getFirstValue(String path, Class<T> type);

    /**
     * <p>Gets the first non-null value at the specified path across all configurations with a default fallback.</p>
     *
     * <pre><code>
     * String logLevel = group.getFirstValue("logging.level", "INFO", String.class);
     * </code></pre>
     *
     * @param path the configuration path to search
     * @param defaultValue the default value if no value is found in any config
     * @param type the expected value type
     * @param <T> the value type
     * @return the first non-null value found, or the default value if none exist
     */
    <T> T getFirstValue(String path, T defaultValue, Class<T> type);

    /**
     * <p>Checks if any configuration in this group contains a key at the specified path.</p>
     *
     * <pre><code>
     * if (group.containsKeyInAny("server.port")) {
     *     // At least one config has this setting
     * }
     * </code></pre>
     *
     * @param path the configuration path to check
     * @return true if any configuration in the group contains a value at this path, false otherwise
     */
    boolean containsKeyInAny(String path);

    /**
     * <p>Gets all keys from all configurations in this group.</p>
     *
     * <pre><code>
     * Set&lt;String&gt; allKeys = group.getAllKeys();
     * // Returns unique keys across all configs
     * </code></pre>
     *
     * @return a set of all configuration keys from all managed configurations
     */
    Set<String> getAllKeys();

    /**
     * <p>Sets the same value at the specified path in all configurations within the group.</p>
     *
     * <pre><code>
     * group.setValueInAll("logging.level", "DEBUG");
     * // Sets logging.level=DEBUG in all configurations
     * </code></pre>
     *
     * @param path the configuration path to set
     * @param value the value to set in all configurations
     */
    void setValueInAll(String path, Object value);

    /**
     * <p>Gets all group-level modules registered with this configuration group.</p>
     *
     * <pre><code>
     * Map&lt;String, GroupConfigModule&gt; modules = group.getGroupModules();
     * for (GroupConfigModule module : modules.values()) {
     *     module.onEvent(event);
     * }
     * </code></pre>
     *
     * @return map of module names to module instances
     */
    Map<String, GroupConfigModule> getGroupModules();

    /**
     * <p>Registers a group-level module that operates across all configurations in the group.</p>
     *
     * <pre><code>
     * GroupSyncModule syncModule = new GroupSyncModule();
     * group.registerGroupModule("sync", syncModule);
     * </code></pre>
     *
     * @param name the module name
     * @param module the group module to register
     */
    void registerGroupModule(String name, GroupConfigModule module);

    /**
     * <p>Unregisters a group-level module by name, removing it from this configuration group.</p>
     *
     * <pre><code>
     * group.unregisterGroupModule("sync");
     * </code></pre>
     *
     * @param name the name of the module to unregister
     */
    void unregisterGroupModule(String name);

    /**
     * <p>Gets a specific group-level module by its registered name.</p>
     *
     * <pre><code>
     * GroupConfigModule validator = group.getGroupModule("validation");
     * if (validator != null) {
     *     // Use the module
     * }
     * </code></pre>
     *
     * @param name the name of the module to retrieve
     * @return the module instance, or null if not found
     */
    GroupConfigModule getGroupModule(String name);

    /**
     * <p>Checks if a group-level module with the specified name is registered.</p>
     *
     * <pre><code>
     * if (group.hasGroupModule("validation")) {
     *     // Module is registered
     * }
     * </code></pre>
     *
     * @param name the module name to check
     * @return true if a module with this name exists, false otherwise
     */
    boolean hasGroupModule(String name);

    /**
     * <p>Enables a group-level module by name, allowing it to process configuration events.</p>
     *
     * <pre><code>
     * group.enableGroupModule("sync");
     * </code></pre>
     *
     * @param name the name of the module to enable
     */
    void enableGroupModule(String name);

    /**
     * <p>Disables a group-level module by name, preventing it from processing configuration events.</p>
     *
     * <pre><code>
     * group.disableGroupModule("sync");
     * </code></pre>
     *
     * @param name the name of the module to disable
     */
    void disableGroupModule(String name);
}
