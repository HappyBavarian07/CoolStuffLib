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
 * <pre><code>
 * AdvancedConfigGroup serverGroup = new AdvancedConfigGroupImpl("servers");
 * serverGroup.addConfig(webConfig);
 * serverGroup.addConfig(dbConfig);
 *
 * Map<String, Object> allPorts = serverGroup.getValuesFromAll("server.port");
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
     * Map<String, AdvancedConfig> configs = group.getConfigs();
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

    void removeConfig(AdvancedConfig config);

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

    boolean containsConfig(String name);

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

    <T> T getConfigValue(String configName, String path, T defaultValue, Class<T> type);

    /**
     * <p>Retrieves values at the same path from all configurations in the group.</p>
     *
     * <pre><code>
     * Map<String, Object> allPorts = group.getValuesFromAll("server.port");
     * // Returns {"web": 8080, "api": 8081, "admin": 8082}
     * </code></pre>
     *
     * @param path the configuration path to query
     * @return map of configuration names to their values at the specified path
     */
    Map<String, Object> getValuesFromAll(String path);

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

    <T> T getFirstValue(String path, T defaultValue, Class<T> type);

    boolean containsKeyInAny(String path);

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

    void unregisterGroupModule(String name);

    GroupConfigModule getGroupModule(String name);

    boolean hasGroupModule(String name);

    void enableGroupModule(String name);

    void disableGroupModule(String name);
}
