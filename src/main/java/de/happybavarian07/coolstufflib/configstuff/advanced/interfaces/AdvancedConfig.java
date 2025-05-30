package de.happybavarian07.coolstufflib.configstuff.advanced.interfaces;

import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigFileType;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * An advanced config interface that provides more features than the standard {@link de.happybavarian07.coolstufflib.configstuff.Config} interface.
 * This interface is designed to be used for more complex configuration scenarios.
 * It provides methods for registering modules, saving and reloading the config, and retrieving the state of the config.
 *
 * <p>
 * This interface allows for more flexibility and extensibility in managing configurations.
 * It is recommended to use this interface instead of the standard {@link de.happybavarian07.coolstufflib.configstuff.Config} interface for more complex configuration scenarios.
 * It supports various file types and allows for the registration of modules that can perform actions when the config is loaded or saved.
 * <p>
 * The methods provided in this interface allow for easy manipulation of configuration data,
 * including saving and reloading the config, retrieving the state of the config, and registering modules that can perform actions when the config is loaded or saved.
 * <p>
 * Example usage via the {@link de.happybavarian07.coolstufflib.configstuff.advanced.AdvancedConfigManager} class:
 * using the {@link de.happybavarian07.coolstufflib.configstuff.advanced.AdvancedConfigManager#createPersistentConfig(String, File, ConfigFileType)} method:
 * <pre>
 *     {@code
 *     AdvancedConfigManager configManager = new AdvancedConfigManager();
 *     AdvancedConfig config = configManager.createPersistentConfig("myConfig", new File("path/to/config.yml"), ConfigFileType.YML);
 *     config.setValue("key", "value");
 *     config.save();
 *     }
 *     </pre>
 * This example creates a new config with a name of "myConfig", a file at "path/to/config.yml", and a YML file type.
 * It then sets the value of the key "key" to "value" and saves the config.
 * This is for persistent storage. There is also a {@link de.happybavarian07.coolstufflib.configstuff.advanced.AdvancedConfigManager#createInMemoryConfig(String)} interface for in-memory-only storage.
 * The Methods are the same because of a common interface {@link de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig}.
 * * <p>
 * </p>
 *
 * @author HappyBavarian07
 * @since 1.0
 */
public interface AdvancedConfig {

    /**
     * Gets the name of the config.
     *
     * @return the name of the config
     */
    String getName();

    /**
     * Gets the type of the config file.
     *
     * @return the type of the config file
     */
    File getFile();

    /**
     * Gets a value from the config by its key.
     * Example: If the config contains a key "database.url", you can retrieve it with get("database.url").
     * This method returns the value associated with the specified key.
     * If the key does not exist, it may return null or throw an exception depending on the implementation.
     *
     * @param key the key to look up in the config
     * @return the value associated with the key, or null if the key does not exist
     */
    Object get(String key);

    /**
     * Gets a value from the config by its key, with a default value if the key does not exist.
     * This method is useful for providing a fallback value when the key is not found in the config.
     *
     * @param key          the key to look up in the config
     * @param defaultValue the default value to return if the key does not exist
     * @return the value associated with the key, or the default value if the key does not exist
     */
    Object get(String key, Object defaultValue);

    /**
     * Gets a value from the config by its key, with a default value if the key does not exist.
     * This method is useful for providing a fallback value when the key is not found in the config.
     * It also allows specifying the expected type of the value.
     *
     * @param key          the key to look up in the config
     * @param defaultValue the default value to return if the key does not exist
     * @param type         the expected type of the value
     * @return the value associated with the key, or the default value if the key does not exist
     */
    <T> T get(String key, T defaultValue, Class<T> type);

    /**
     * Sets a value in the config for the specified key.
     * If the key already exists, it updates the value; otherwise, it adds a new key-value pair.
     *
     * @param key   the key to set in the config
     * @param value the value to associate with the key
     */
    void setValue(String key, Object value);

    /**
     * Sets multiple key-value pairs in the config.
     * This method allows you to set multiple values at once, which can be more efficient than setting them one by one.
     *
     * @param values a map of key-value pairs to set in the config
     */
    void setValueBulk(Map<String, Object> values);

    /**
     * Removes a key and its associated value from the config.
     * If the key does not exist, this method may do nothing or throw an exception depending on the implementation.
     *
     * @param key the key to remove from the config
     */
    void remove(String key);

    /**
     * Checks if the config contains a specific key.
     * This method returns true if the key exists in the config, false otherwise.
     *
     * @param key the key to check for existence in the config
     * @return true if the key exists, false otherwise
     */
    boolean containsKey(String key);

    /**
     * Saves the current state of the config to the file.
     * This method should be called after making changes to ensure they are persisted.
     */
    void save();

    /**
     * Reloads the config from the file.
     * This method discards any unsaved changes and reloads the config data from the file.
     * But only for the persistent configs, not for in-memory configs.
     */
    void reload();

    /**
     * Registers a module to this config.
     * The module will be attached to the config and can perform actions when the config is loaded or saved.
     *
     * @param module the module to register
     */
    void registerModule(BaseConfigModule module);

    /**
     * Unregisters a module from this config.
     * The module will no longer be attached to the config and will not perform actions on config load or save.
     *
     * @param module the module to unregister
     */
    void unregisterModule(BaseConfigModule module);

    /**
     * Unregisters a module from this config by its name.
     * The module will no longer be attached to the config and will not perform actions on config load or save.
     *
     * @param name the name of the module to unregister
     */
    void unregisterModule(String name);

    /**
     * Checks if a module is registered with this config.
     * This method returns true if the module is registered, false otherwise.
     * This can be useful for checking if a specific module is active before performing operations that depend on it.
     *
     * @param module the module to check for registration
     * @return true if the module is registered, false otherwise
     */
    boolean hasModule(BaseConfigModule module);

    /**
     * Checks if a module is registered with this config by its name.
     * This method returns true if a module with the specified name is registered, false otherwise.
     * This can be useful for checking if a specific module is active before performing operations that depend on it.
     *
     * @param moduleName the name of the module to check for registration
     * @return true if the module is registered, false otherwise
     */
    boolean hasModule(String moduleName);

    /**
     * Gets the module registered with the given name.
     *
     * @param name the module name
     * @return the module, or null if not registered
     */
    BaseConfigModule getModuleByName(String name);

    /**
     * Enables a module by its name.
     * This method will call the enable method of the module if it is registered.
     * If the module is not registered, it will do nothing.
     *
     * @param moduleName the name of the module to enable
     */
    void enableModule(String moduleName);

    /**
     * Disables a module by its name.
     * This method will call the disable method of the module if it is registered.
     * If the module is not registered, it will do nothing.
     *
     * @param moduleName the name of the module to disable
     */
    void disableModule(String moduleName);

    /**
     * Gets a list of all registered modules for this config.
     * This method returns a list of ConfigModule objects that are currently registered with the config.
     *
     * @return a map of registered modules
     */
    Map<String, BaseConfigModule> getModules();

    /**
     * Gets a list of all keys present in the config.
     *
     * @return a list of all keys in the config
     */
    List<String> getKeys();

    /**
     * Gets a map of all key-value pairs in the config.
     * This method returns a map containing all keys and their associated values in the config.
     * It can be useful for iterating over all configuration entries or exporting the config data.
     *
     * @return a map of all key-value pairs in the config
     */
    Map<String, Object> getValueMap();
}