package de.happybavarian07.coolstufflib.configstuff.advanced.interfaces;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEventBus;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.ConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.migration.MigrationContext;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * <p>Core interface for advanced configuration management with hierarchical sections,
 * module system, thread safety, and event handling capabilities.</p>
 *
 * <p>This interface provides comprehensive configuration management including:</p>
 * <ul>
 *   <li>Hierarchical section-based organization</li>
 *   <li>Type-safe value access with defaults</li>
 *   <li>Module system for extensibility</li>
 *   <li>Thread-safe operations with explicit locking</li>
 *   <li>Event-driven architecture for change notifications</li>
 *   <li>File-based persistence with multiple format support</li>
 * </ul>
 *
 * <pre><code>
 * AdvancedConfig config = new AdvancedPersistentConfig("myConfig",
 *     new File("config.yml"), ConfigFileType.YAML);
 *
 * config.set("database.host", "localhost");
 * config.set("database.port", 5432);
 * String host = config.getString("database.host", "127.0.0.1");
 *
 * config.save();
 * </code></pre>
 */
public interface AdvancedConfig {

    /**
     * <p>Gets the unique name identifier for this configuration instance.</p>
     *
     * <pre><code>
     * String configName = config.getName();
     * </code></pre>
     *
     * @return the configuration name, never null
     */
    String getName();

    /**
     * <p>Gets the file associated with this configuration for persistence operations.</p>
     *
     * <pre><code>
     * File configFile = config.getFile();
     * if (configFile != null && configFile.exists()) {
     *     // File-based configuration
     * }
     * </code></pre>
     *
     * @return the configuration file, or null for in-memory configurations
     */
    File getFile();

    /**
     * <p>Gets the file handler responsible for serialization and deserialization operations.</p>
     *
     * <pre><code>
     * ConfigFileHandler handler = config.getConfigFileHandler();
     * boolean supportsComments = handler.supportsComments();
     * </code></pre>
     *
     * @return the file handler instance, never null
     */
    ConfigFileHandler getConfigFileHandler();

    /**
     * <p>Gets the event bus for configuration change notifications and module communication.</p>
     *
     * <pre><code>
     * ConfigEventBus eventBus = config.getEventBus();
     * eventBus.subscribe(ConfigValueEvent.class, this::onValueChange);
     * </code></pre>
     *
     * @return the event bus instance, never null
     */
    ConfigEventBus getEventBus();

    /**
     * <p>Acquires a read lock for module operations to ensure thread-safe access.</p>
     *
     * <pre><code>
     * config.lockModuleRead();
     * try {
     *     BaseConfigModule module = config.getModuleByName("ValidationModule");
     * } finally {
     *     config.unlockModuleRead();
     * }
     * </code></pre>
     */
    void lockModuleRead();

    /**
     * <p>Releases a read lock for module operations.</p>
     *
     * <pre><code>
     * config.lockModuleRead();
     * try {
     *     // Module operations
     * } finally {
     *     config.unlockModuleRead();
     * }
     * </code></pre>
     */
    void unlockModuleRead();

    /**
     * <p>Acquires a write lock for module operations to ensure thread-safe modifications.</p>
     *
     * <pre><code>
     * config.lockModuleWrite();
     * try {
     *     config.registerModule(new ValidationModule());
     * } finally {
     *     config.unlockModuleWrite();
     * }
     * </code></pre>
     */
    void lockModuleWrite();

    /**
     * <p>Releases a write lock for module operations.</p>
     *
     * <pre><code>
     * config.lockModuleWrite();
     * try {
     *     // Module modifications
     * } finally {
     *     config.unlockModuleWrite();
     * }
     * </code></pre>
     */
    void unlockModuleWrite();

    /**
     * <p>Acquires a read lock for value operations to ensure thread-safe access.</p>
     *
     * <pre><code>
     * config.lockValuesRead();
     * try {
     *     String value = config.getString("key");
     * } finally {
     *     config.unlockValuesRead();
     * }
     * </code></pre>
     */
    void lockValuesRead();

    /**
     * <p>Releases a read lock for value operations.</p>
     *
     * <pre><code>
     * config.lockValuesRead();
     * try {
     *     // Value reading operations
     * } finally {
     *     config.unlockValuesRead();
     * }
     * </code></pre>
     */
    void unlockValuesRead();

    /**
     * <p>Acquires a write lock for value operations to ensure thread-safe modifications.</p>
     *
     * <pre><code>
     * config.lockValuesWrite();
     * try {
     *     config.set("key", "value");
     * } finally {
     *     config.unlockValuesWrite();
     * }
     * </code></pre>
     */
    void lockValuesWrite();

    /**
     * <p>Releases a write lock for value operations.</p>
     *
     * <pre><code>
     * config.lockValuesWrite();
     * try {
     *     // Value modification operations
     * } finally {
     *     config.unlockValuesWrite();
     * }
     * </code></pre>
     */
    void unlockValuesWrite();

    /**
     * <p>Executes an operation with a module read lock, automatically handling lock acquisition and release.</p>
     *
     * <pre><code>
     * BaseConfigModule module = config.withModuleReadLock(() ->
     *     config.getModuleByName("ValidationModule"));
     * </code></pre>
     *
     * @param operation the operation to execute under lock
     * @param <T>       the return type of the operation
     * @return the result of the operation
     */
    <T> T withModuleReadLock(Supplier<T> operation);

    /**
     * <p>Executes an operation with a values read lock, automatically handling lock acquisition and release.</p>
     *
     * <pre><code>
     * String value = config.withValuesReadLock(() ->
     *     config.getString("database.host"));
     * </code></pre>
     *
     * @param operation the operation to execute under lock
     * @param <T>       the return type of the operation
     * @return the result of the operation
     */
    <T> T withValuesReadLock(Supplier<T> operation);

    /**
     * <p>Executes an operation with a modules write lock, automatically handling lock acquisition and release.</p>
     *
     * <pre><code>
     * config.withModulesWriteLock(() -> {
     *     config.registerModule(new ValidationModule());
     *     return null;
     * });
     * </code></pre>
     *
     * @param operation the operation to execute under lock
     * @param <T>       the return type of the operation
     * @return the result of the operation
     */
    <T> T withModulesWriteLock(Supplier<T> operation);

    /**
     * <p>Executes an operation with a values write lock, automatically handling lock acquisition and release.</p>
     *
     * <pre><code>
     * config.withValuesWriteLock(() -> {
     *     config.set("database.host", "localhost");
     *     config.set("database.port", 5432);
     *     return null;
     * });
     * </code></pre>
     *
     * @param operation the operation to execute under lock
     * @param <T>       the return type of the operation
     * @return the result of the operation
     */
    <T> T withValuesWriteLock(Supplier<T> operation);

    /**
     * <p>Gets the root configuration section that contains all configuration data.</p>
     *
     * <pre><code>
     * ConfigSection root = config.getRootSection();
     * Map<String, Object> allData = root.toMap();
     * </code></pre>
     *
     * @return the root section, never null
     */
    ConfigSection getRootSection();

    /**
     * <p>Gets an existing configuration section by its hierarchical path.</p>
     *
     * <pre><code>
     * ConfigSection dbSection = config.getSection("database");
     * if (dbSection != null) {
     *     String host = dbSection.getString("host");
     * }
     * </code></pre>
     *
     * @param path the dot-separated path to the section
     * @return the section if it exists, null otherwise
     */
    ConfigSection getSection(String path);

    /**
     * <p>Creates a new configuration section at the specified path, including any missing parent sections.</p>
     *
     * <pre><code>
     * ConfigSection dbSection = config.createSection("database.credentials");
     * dbSection.set("username", "admin");
     * </code></pre>
     *
     * @param path the dot-separated path for the new section
     * @return the created section, never null
     */
    ConfigSection createSection(String path);

    /**
     * <p>Checks if a configuration section exists at the specified path.</p>
     *
     * <pre><code>
     * if (config.hasSection("database")) {
     *     ConfigSection dbSection = config.getSection("database");
     * }
     * </code></pre>
     *
     * @param path the dot-separated path to check
     * @return true if the section exists, false otherwise
     */
    boolean hasSection(String path);

    /**
     * <p>Removes a configuration section and all its contents at the specified path.</p>
     *
     * <pre><code>
     * config.removeSection("database.credentials");
     * </code></pre>
     *
     * @param path the dot-separated path of the section to remove
     */
    void removeSection(String path);

    /**
     * <p>Gets a configuration value by its hierarchical path.</p>
     *
     * <pre><code>
     * Object value = config.get("database.host");
     * if (value instanceof String host) {
     *     // Use the host value
     * }
     * </code></pre>
     *
     * @param path the dot-separated path to the value
     * @return the value if present, null otherwise
     */
    Object get(String path);

    /**
     * <p>Gets a configuration value with a fallback default if the value is not present.</p>
     *
     * <pre><code>
     * String host = (String) config.get("database.host", "localhost");
     * </code></pre>
     *
     * @param path         the dot-separated path to the value
     * @param defaultValue the fallback value if path is not found
     * @return the configuration value or the default value
     */
    Object get(String path, Object defaultValue);

    /**
     * <p>Gets a string value with type safety and null protection.</p>
     *
     * <pre><code>
     * String username = config.getString("database.username");
     * </code></pre>
     *
     * @param path the dot-separated path to the value
     * @return the string value, or null if not found or not a string
     */
    String getString(String path);

    /**
     * <p>Gets a string value with a fallback default if the value is not present or not a string.</p>
     *
     * <pre><code>
     * String host = config.getString("database.host", "localhost");
     * </code></pre>
     *
     * @param path the dot-separated path to the value
     * @param defaultValue the fallback value if not found or not a string
     * @return the string value or the default value
     */
    String getString(String path, String defaultValue);

    /**
     * <p>Gets a boolean value from the configuration.</p>
     *
     * <pre><code>
     * boolean enabled = config.getBoolean("features.authentication");
     * </code></pre>
     *
     * @param path the dot-separated path to the value
     * @return the boolean value, or false if not found or not a boolean
     */
    boolean getBoolean(String path);

    /**
     * <p>Gets a boolean value with a fallback default if the value is not present or not a boolean.</p>
     *
     * <pre><code>
     * boolean enabled = config.getBoolean("features.authentication", true);
     * </code></pre>
     *
     * @param path the dot-separated path to the value
     * @param defaultValue the fallback value if not found or not a boolean
     * @return the boolean value or the default value
     */
    boolean getBoolean(String path, boolean defaultValue);

    /**
     * <p>Gets an integer value from the configuration.</p>
     *
     * <pre><code>
     * int port = config.getInt("database.port");
     * </code></pre>
     *
     * @param path the dot-separated path to the value
     * @return the integer value, or 0 if not found or not an integer
     */
    int getInt(String path);

    /**
     * <p>Gets an integer value with a fallback default if the value is not present or not an integer.</p>
     *
     * <pre><code>
     * int port = config.getInt("database.port", 3306);
     * </code></pre>
     *
     * @param path the dot-separated path to the value
     * @param defaultValue the fallback value if not found or not an integer
     * @return the integer value or the default value
     */
    int getInt(String path, int defaultValue);

    /**
     * <p>Gets a long value from the configuration.</p>
     *
     * <pre><code>
     * long timestamp = config.getLong("lastModified");
     * </code></pre>
     *
     * @param path the dot-separated path to the value
     * @return the long value, or 0 if not found or not a long
     */
    long getLong(String path);

    /**
     * <p>Gets a long value with a fallback default if the value is not present or not a long.</p>
     *
     * <pre><code>
     * long timestamp = config.getLong("lastModified", System.currentTimeMillis());
     * </code></pre>
     *
     * @param path the dot-separated path to the value
     * @param defaultValue the fallback value if not found or not a long
     * @return the long value or the default value
     */
    long getLong(String path, long defaultValue);

    /**
     * <p>Gets a double value from the configuration.</p>
     *
     * <pre><code>
     * double ratio = config.getDouble("settings.ratio");
     * </code></pre>
     *
     * @param path the dot-separated path to the value
     * @return the double value, or 0.0 if not found or not a double
     */
    double getDouble(String path);

    /**
     * <p>Gets a double value with a fallback default if the value is not present or not a double.</p>
     *
     * <pre><code>
     * double ratio = config.getDouble("settings.ratio", 1.0);
     * </code></pre>
     *
     * @param path the dot-separated path to the value
     * @param defaultValue the fallback value if not found or not a double
     * @return the double value or the default value
     */
    double getDouble(String path, double defaultValue);

    /**
     * <p>Gets a float value from the configuration.</p>
     *
     * <pre><code>
     * float multiplier = config.getFloat("settings.multiplier");
     * </code></pre>
     *
     * @param path the dot-separated path to the value
     * @return the float value, or 0.0f if not found or not a float
     */
    float getFloat(String path);

    /**
     * <p>Gets a float value with a fallback default if the value is not present or not a float.</p>
     *
     * <pre><code>
     * float multiplier = config.getFloat("settings.multiplier", 1.0f);
     * </code></pre>
     *
     * @param path the dot-separated path to the value
     * @param defaultValue the fallback value if not found or not a float
     * @return the float value or the default value
     */
    float getFloat(String path, float defaultValue);

    /**
     * <p>Gets a list value from the configuration.</p>
     *
     * <pre><code>
     * List<?> items = config.getList("inventory.items");
     * </code></pre>
     *
     * @param path the dot-separated path to the value
     * @return the list value, or null if not found or not a list
     */
    List<?> getList(String path);

    /**
     * <p>Gets a list value with a fallback default if the value is not present or not a list.</p>
     *
     * <pre><code>
     * List<?> items = config.getList("inventory.items", new ArrayList<>());
     * </code></pre>
     *
     * @param path the dot-separated path to the value
     * @param defaultValue the fallback value if not found or not a list
     * @return the list value or the default value
     */
    List<?> getList(String path, List<?> defaultValue);

    /**
     * <p>Gets a string list value from the configuration.</p>
     *
     * <pre><code>
     * List<String> names = config.getStringList("players.names");
     * </code></pre>
     *
     * @param path the dot-separated path to the value
     * @return the string list value, or null if not found or not a string list
     */
    List<String> getStringList(String path);

    /**
     * <p>Gets a string list value with a fallback default if the value is not present or not a string list.</p>
     *
     * <pre><code>
     * List<String> names = config.getStringList("players.names", new ArrayList<>());
     * </code></pre>
     *
     * @param path the dot-separated path to the value
     * @param defaultValue the fallback value if not found or not a string list
     * @return the string list value or the default value
     */
    List<String> getStringList(String path, List<String> defaultValue);

    <T> T get(String path, T defaultValue, Class<T> type);

    /**
     * <p>Gets a strongly-typed value with automatic type conversion.</p>
     *
     * <pre><code>
     * Integer port = config.getValue("database.port", Integer.class);
     * </code></pre>
     *
     * @param path the dot-separated path to the value
     * @param type the expected type class
     * @param <T>  the value type
     * @return the typed value, or null if not found or conversion fails
     */
    <T> T getValue(String path, Class<T> type);

    /**
     * <p>Gets a strongly-typed value wrapped in an Optional for null safety.</p>
     *
     * <pre><code>
     * Optional<Integer> port = config.getOptionalValue("database.port", Integer.class);
     * if (port.isPresent()) {
     *     // Use the port value
     * }
     * </code></pre>
     *
     * @param path the dot-separated path to the value
     * @param type the expected type class
     * @param <T>  the value type
     * @return an Optional containing the typed value, or empty if not found
     */
    <T> Optional<T> getOptionalValue(String path, Class<T> type);

    /**
     * <p>Sets a configuration value at the specified path, creating parent sections as needed.</p>
     *
     * <pre><code>
     * config.set("database.host", "localhost");
     * config.set("database.port", 5432);
     * </code></pre>
     *
     * @param path  the dot-separated path for the value
     * @param value the value to set
     */
    void set(String path, Object value);

    /**
     * <p>Sets multiple configuration values in a single operation for better performance.</p>
     *
     * <pre><code>
     * Map<String, Object> dbConfig = Map.of(
     *     "database.host", "localhost",
     *     "database.port", 5432
     * );
     * config.setBulk(dbConfig);
     * </code></pre>
     *
     * @param values map of paths to values to set
     */
    void setBulk(Map<String, Object> values);

    /**
     * <p>Removes a configuration value at the specified path.</p>
     *
     * <pre><code>
     * config.remove("database.password");
     * </code></pre>
     *
     * @param path the dot-separated path of the value to remove
     */
    void remove(String path);

    /**
     * <p>Checks if a configuration key exists at the specified path.</p>
     *
     * <pre><code>
     * if (config.containsKey("database.host")) {
     *     String host = config.getString("database.host");
     * }
     * </code></pre>
     *
     * @param path the dot-separated path to check
     * @return true if the key exists, false otherwise
     */
    boolean containsKey(String path);

    /**
     * <p>Persists the current configuration state to the associated file.</p>
     *
     * <pre><code>
     * config.set("lastModified", System.currentTimeMillis());
     * config.save();
     * </code></pre>
     *
     * @throws RuntimeException if save operation fails
     */
    void save();

    /**
     * <p>Reloads configuration data from the associated file, discarding unsaved changes.</p>
     *
     * <pre><code>
     * config.reload();
     * </code></pre>
     *
     * @throws RuntimeException if reload operation fails
     */
    void reload();

    /**
     * <p>Clears all configuration data, removing all sections and values.</p>
     *
     * <pre><code>
     * config.clear();
     * </code></pre>
     */
    void clear();

    /**
     * <p>Registers a module to extend configuration functionality.</p>
     *
     * <pre><code>
     * ValidationModule validator = new ValidationModule();
     * config.registerModule(validator);
     * </code></pre>
     *
     * @param module the module to register
     * @throws IllegalArgumentException if module is already registered
     */
    void registerModule(BaseConfigModule module);

    /**
     * <p>Unregisters a module by name, removing its functionality from the configuration.</p>
     *
     * <pre><code>
     * config.unregisterModule("ValidationModule");
     * </code></pre>
     *
     * @param name the name of the module to unregister
     */
    void unregisterModule(String name);

    /**
     * <p>Checks if a specific module instance is registered with this configuration.</p>
     *
     * <pre><code>
     * ValidationModule validator = new ValidationModule();
     * if (config.hasModule(validator)) {
     *     // Module is registered
     * }
     * </code></pre>
     *
     * @param module the module instance to check
     * @return true if the module is registered, false otherwise
     */
    boolean hasModule(BaseConfigModule module);

    /**
     * <p>Checks if a module with the specified name is registered with this configuration.</p>
     *
     * <pre><code>
     * if (config.hasModule("ValidationModule")) {
     *     config.enableModule("ValidationModule");
     * }
     * </code></pre>
     *
     * @param moduleName the name of the module to check
     * @return true if a module with this name is registered, false otherwise
     */
    boolean hasModule(String moduleName);

    /**
     * <p>Gets a registered module by its name.</p>
     *
     * <pre><code>
     * BaseConfigModule validator = config.getModuleByName("ValidationModule");
     * if (validator != null) {
     *     // Use the module
     * }
     * </code></pre>
     *
     * @param name the name of the module to retrieve
     * @return the module instance, or null if not found
     */
    BaseConfigModule getModuleByName(String name);

    /**
     * <p>Enables a module by name, allowing it to process configuration events.</p>
     *
     * <pre><code>
     * config.enableModule("ValidationModule");
     * </code></pre>
     *
     * @param moduleName the name of the module to enable
     */
    void enableModule(String moduleName);

    /**
     * <p>Disables a module by name, preventing it from processing configuration events.</p>
     *
     * <pre><code>
     * config.disableModule("ValidationModule");
     * </code></pre>
     *
     * @param moduleName the name of the module to disable
     */
    void disableModule(String moduleName);

    /**
     * <p>Gets all registered modules as a map of name to module instance.</p>
     *
     * <pre><code>
     * Map<String, BaseConfigModule> modules = config.getModules();
     * for (Map.Entry<String, BaseConfigModule> entry : modules.entrySet()) {
     *     String name = entry.getKey();
     *     BaseConfigModule module = entry.getValue();
     * }
     * </code></pre>
     *
     * @return a map of module names to module instances, never null
     */
    Map<String, BaseConfigModule> getModules();

    /**
     * <p>Gets all configuration keys, optionally including nested keys from subsections.</p>
     *
     * <pre><code>
     * List<String> allKeys = config.getKeys(true);
     * List<String> topLevelKeys = config.getKeys(false);
     * </code></pre>
     *
     * @param deep if true, includes keys from nested sections; if false, only top-level keys
     * @return a list of configuration keys, never null
     */
    List<String> getKeys(boolean deep);

    /**
     * <p>Creates a specialized configuration section with custom behavior.</p>
     *
     * <pre><code>
     * ListSection items = config.createCustomSection("inventory.items", ListSection.class);
     * items.add("sword");
     * items.add("shield");
     * </code></pre>
     *
     * @param path        the path for the new section
     * @param sectionType the specialized section type
     * @param <T>         the section type
     * @return the created specialized section
     */
    <T extends ConfigSection> T createCustomSection(String path, Class<T> sectionType);

    /**
     * <p>Copies all configuration data from another configuration instance.</p>
     *
     * <pre><code>
     * AdvancedConfig source = loadConfigFromFile("template.yml");
     * config.copyFrom(source);
     * </code></pre>
     *
     * @param config2 the source configuration to copy from
     */
    void copyFrom(AdvancedConfig config2);

    /**
     * <p>Checks if the configuration has metadata associated with it, such as version or additional information.</p>
     *
     * <pre><code>
     * if (config.hasMetadata("version")) {
     *     String version = config.getMetadata("version");
     * }
     * </code></pre>
     *
     * @param version the metadata key to check
     * @return true if metadata exists for the given key, false otherwise
     */
    boolean hasMetadata(String version);

    /**
     * <p>Removes metadata associated with the configuration, such as version or additional information.</p>
     *
     * <pre><code>
     * config.removeMetadata("version");
     * </code></pre>
     *
     * @param version the metadata key to remove
     */
    void removeMetadata(String version);

    /**
     * <p>Gets all metadata associated with the configuration.</p>
     *
     * <pre><code>
     * Map<String, Object> metadata = config.getMetadata();
     * </code></pre>
     *
     * @return a map of metadata key-value pairs, never null
     */
    Map<String, Object> getMetadata();

    /**
     * <p>Gets metadata associated with the configuration, such as version or additional information.</p>
     *
     * <pre><code>
     * String version = config.getMetadata("version");
     * </code></pre>
     *
     * @param name the metadata key
     * @param <T>  the type of the value
     * @return the metadata value, or null if not found
     */
    <T> T getMetadata(String name);

    /**
     * <p>Adds metadata to the configuration, which can be used for versioning or additional information.</p>
     *
     * <pre><code>
     * config.addMetadata("version", "1.0.0");
     * </code></pre>
     *
     * @param name  the metadata key
     * @param value the metadata value
     * @param <T>   the type of the value
     */
    <T> void addMetadata(String name, T value);

    /**
     * <p>Gets a comment associated with the specified configuration key.</p>
     *
     * <pre><code>
     * String comment = config.getComment("database.host");
     * </code></pre>
     *
     * @param path the configuration path
     * @return the comment text if exists, null otherwise
     */
    Object getComment(String path);

    /**
     * <p>Sets a comment for the specified configuration path.</p>
     *
     * <pre><code>
     * config.setComment("database", "Database connection settings");
     * config.setComment("database.credentials", "Secure credential information");
     * </code></pre>
     *
     * @param path    the configuration path to associate the comment with
     * @param comment the comment text to set
     */
    void setComment(String path, String comment);

    /**
     * <p>Removes the comment associated with the specified configuration path.</p>
     *
     * <pre><code>
     * config.removeComment("database.host");
     * </code></pre>
     *
     * @param path the configuration path to remove comment from
     */
    void removeComment(String path);

    /**
     * <p>Returns all comments defined in the configuration.</p>
     *
     * <pre><code>
     * Map<String, String> comments = config.getAllComments();
     * for (Map.Entry<String, String> entry : comments.entrySet()) {
     *     String path = entry.getKey();
     *     String comment = entry.getValue();
     * }
     * </code></pre>
     *
     * @return a map of path-to-comment associations
     */
    Map<String, String> getAllComments();

    /**
     * <p>Checks if the specified configuration path has an associated comment.</p>
     *
     * <pre><code>
     * if (config.hasComment("database")) {
     *     String comment = config.getComment("database");
     * }
     * </code></pre>
     *
     * @param path the configuration path to check
     * @return true if a comment exists for the path, false otherwise
     */
    boolean hasComment(String path);

    /**
     * <p>Gets the migration context associated with this configuration, which can be used for version migrations.</p>
     *
     * <pre><code>
     * MigrationContext migrationContext = config.getMigrationContext();
     * if (migrationContext != null) {
     *     // Perform migration operations
     * }
     * </code></pre>
     *
     * @return the migration context, or null if not set
     */
    MigrationContext getMigrationContext();

    /**
     * <p>Sets the migration context for this configuration, which can be used for version migrations.</p>
     *
     * <pre><code>
     * MigrationContext migrationContext = new MigrationContext("1.0.0", "2.0.0");
     * config.setMigrationContext(migrationContext);
     * </code></pre>
     *
     * @param migrationContext the migration context to set
     */
    void setMigrationContext(MigrationContext migrationContext);

    /**
     * <p>Checks if the configuration is currently loaded and ready for use.</p>
     *
     * <pre><code>
     * if (config.isLoaded()) {
     *     // Configuration is ready to use
     * }
     * </code></pre>
     *
     * @return true if the configuration is loaded, false otherwise
     */
    boolean isLoaded();

    /**
     * <p>Detects and converts collection data structures to their appropriate section types.</p>
     *
     * <pre><code>
     * config.detectAndConvertCollections();
     * </code></pre>
     */
    void detectAndConvertCollections();

    /**
     * <p>Migrates configuration data from the given root section into this config.</p>
     *
     * <pre><code>
     * config.migrate(context, oldConfig.getRootSection(), true);
     * </code></pre>
     *
     * @param context the migration context
     * @param rootSection the root section of the old config
     * @param replace if true, replaces existing values; if false, merges
     */
    void migrate(MigrationContext context, ConfigSection rootSection, boolean replace);

    /**
     * <p>Migrates configuration data from the given map into this config.</p>
     *
     * <pre><code>
     * Map<String, Object> oldData = loadOldData();
     * config.migrate(context, oldData, false);
     * </code></pre>
     *
     * @param context the migration context
     * @param values the map of values to migrate
     * @param replace if true, replaces existing values; if false, merges
     */
    void migrate(MigrationContext context, Map<String, Object> values, boolean replace);
}
