package de.happybavarian07.coolstufflib.configstuff;

import de.happybavarian07.coolstufflib.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Manages multiple configuration files within a designated folder, providing centralized
 * configuration operations including creation, loading, saving, and value manipulation across
 * multiple configuration instances.</p>
 *
 * <p>This class provides:</p>
 * <ul>
 * <li>Centralized management of multiple named configurations</li>
 * <li>Automatic file creation and folder structure management</li>
 * <li>Bulk operations across all managed configurations</li>
 * <li>Resource-based configuration loading from JAR files</li>
 * </ul>
 *
 * <pre><code>
 * ConfigManager manager = new ConfigManager(new File("configs"));
 * Config playerConfig = manager.createConfig("players", "players.yml");
 * manager.setConfigValue("players", "max-players", 100);
 * manager.saveConfig("players");
 * </code></pre>
 */
public class ConfigManager {
    private final File configFolder;
    private final Map<String, Config> configs = new HashMap<>();

    public ConfigManager(File configFolder) {
        this.configFolder = configFolder;
        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }
    }

    public Map<String, Config> getConfigs() {
        return configs;
    }

    /**
     * <p>Retrieves a managed configuration by its registered name.</p>
     *
     * <pre><code>
     * Config playerConfig = manager.getConfig("players");
     * if (playerConfig != null) {
     *     String serverName = playerConfig.getString("server.name", "Default");
     * }
     * </code></pre>
     *
     * @param configName the name under which the configuration was registered
     * @return the configuration instance, or null if not found
     */
    public Config getConfig(String configName) {
        return configs.get(configName);
    }

    /**
     * <p>Reloads a specific configuration from its file, discarding any unsaved changes
     * and loading fresh data from the file system.</p>
     *
     * <pre><code>
     * manager.reloadConfig("settings");
     * // Configuration "settings" now reflects current file contents
     * </code></pre>
     *
     * @param configName the name of the configuration to reload
     */
    public void reloadConfig(String configName) {
        if (configs.containsKey(configName)) {
            configs.get(configName).reload();
        }
    }

    /**
     * <p>Creates a new configuration with automatic resource extraction if the file
     * doesn't exist. The file will be created in the manager's designated folder.</p>
     *
     * <pre><code>
     * Config config = manager.createConfig("database", "database.yml");
     * config.set("host", "localhost");
     * config.set("port", 3306);
     * </code></pre>
     *
     * @param configName the name to register this configuration under
     * @param fileName   the filename for the configuration file (extension optional)
     * @return the newly created configuration instance
     */
    public Config createConfig(String configName, String fileName) {
        String fileNameWithoutExtension = fileName.endsWith(".yml") ? fileName : fileName + ".yml";
        File configFile = new File(configFolder, fileNameWithoutExtension);
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            Utils.saveResource(configFolder, fileNameWithoutExtension, false);
        }
        Config config = new Config(configFile);
        configs.put(configName, config);
        return config;
    }

    /**
     * <p>Creates a new configuration using an existing File object as the target.
     * The file will be created if it doesn't exist.</p>
     *
     * <pre><code>
     * File customFile = new File("custom/path/config.yml");
     * Config config = manager.createConfig("custom", customFile);
     * </code></pre>
     *
     * @param configName the name to register this configuration under
     * @param configFile the file object representing the configuration location
     * @return the newly created configuration instance
     * @throws RuntimeException if the file cannot be created
     */
    public Config createConfig(String configName, File configFile) {
        configFile = new File(configFolder, configFile.getPath());
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Could not create config file: " + configFile.getPath(), e);
            }
        }
        Config config = new Config(configFile);
        configs.put(configName, config);
        return config;
    }

    /**
     * <p>Permanently deletes a configuration file and removes it from management.
     * This operation cannot be undone.</p>
     *
     * <pre><code>
     * manager.deleteConfig("temporary-settings");
     * // File deleted and configuration removed from manager
     * </code></pre>
     *
     * @param configName the name of the configuration to delete
     */
    public void deleteConfig(String configName) {
        if (configs.containsKey(configName)) {
            configs.get(configName).delete();
            configs.remove(configName);
        }
    }

    /**
     * <p>Loads an existing configuration file into management. Creates an empty file
     * if it doesn't exist.</p>
     *
     * <pre><code>
     * manager.loadConfig("existing-config", "existing.yml");
     * Config loaded = manager.getConfig("existing-config");
     * </code></pre>
     *
     * @param configName the name to register this configuration under
     * @param fileName   the filename of the configuration to load
     * @throws RuntimeException if the file cannot be created
     */
    public void loadConfig(String configName, String fileName) {
        File configFile = new File(configFolder, fileName.endsWith(".yml") ? fileName : fileName + ".yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Could not create config file: " + configFile.getPath(), e);
            }
        }
        configs.put(configName, new Config(configFile));
    }

    /**
     * <p>Loads an existing configuration file using a File object. Creates an empty
     * file if it doesn't exist.</p>
     *
     * <pre><code>
     * File configFile = new File("data/settings.yml");
     * manager.loadConfig("settings", configFile);
     * </code></pre>
     *
     * @param configName the name to register this configuration under
     * @param configFile the file object representing the configuration location
     * @throws RuntimeException if the file cannot be created
     */
    public void loadConfig(String configName, File configFile) {
        configFile = new File(configFolder, configFile.getPath());
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Could not create config file: " + configFile.getPath(), e);
            }
        }
        configs.put(configName, new Config(configFile));
    }

    /**
     * <p>Loads a configuration from resources bundled with the application (JAR file).
     * Extracts the resource to the file system if it doesn't already exist.</p>
     *
     * <pre><code>
     * manager.loadConfigFromResource("default", "default-config.yml");
     * // Loads default-config.yml from JAR resources
     * </code></pre>
     *
     * @param configName the name to register this configuration under
     * @param fileName   the resource filename to extract and load
     */
    public void loadConfigFromResource(String configName, String fileName) {
        String fileNameWithoutExtension = fileName.endsWith(".yml") ? fileName : fileName + ".yml";
        File configFile = new File(configFolder, fileNameWithoutExtension);
        if (!configFile.exists()) {
            Utils.saveResource(configFolder, fileNameWithoutExtension, false);
        }
        configs.put(configName, new Config(configFile));
    }

    /**
     * <p>Saves a specific configuration to its file, persisting any changes made
     * since the last save operation.</p>
     *
     * <pre><code>
     * manager.setConfigValue("players", "max-players", 150);
     * manager.saveConfig("players");
     * // Changes are now persisted to file
     * </code></pre>
     *
     * @param configName the name of the configuration to save
     * @throws RuntimeException if the file cannot be written
     */
    public void saveConfig(String configName) {
        if (configs.containsKey(configName)) {
            try {
                configs.get(configName).save();
            } catch (IOException e) {
                throw new RuntimeException("Could not save config: " + configName, e);
            }
        }
    }

    /**
     * <p>Sets a value in a specific configuration at the given path. Changes are
     * made in memory and require calling saveConfig() to persist.</p>
     *
     * <pre><code>
     * manager.setConfigValue("database", "connection.timeout", 30);
     * manager.setConfigValue("database", "connection.host", "localhost");
     * manager.saveConfig("database");
     * </code></pre>
     *
     * @param configName the name of the configuration to modify
     * @param path       the configuration path where the value should be set
     * @param value      the value to set at the specified path
     */
    public void setConfigValue(String configName, String path, Object value) {
        if (configs.containsKey(configName)) {
            configs.get(configName).set(path, value);
        }
    }

    /**
     * <p>Retrieves a value from a specific configuration, returning the provided
     * default value if the configuration or path doesn't exist.</p>
     *
     * <pre><code>
     * String host = manager.getConfigValue("database", "host", "localhost");
     * Integer timeout = manager.getConfigValue("database", "timeout", 30);
     * </code></pre>
     *
     * @param configName   the name of the configuration to read from
     * @param path         the configuration path to retrieve
     * @param defaultValue the value to return if the path doesn't exist
     * @param <T>          the type of the value being retrieved
     * @return the value at the path, the default value, or null if config doesn't exist
     */
    public <T> T getConfigValue(String configName, String path, T defaultValue) {
        if (configs.containsKey(configName)) {
            return configs.get(configName).get(path, defaultValue);
        }
        return null;
    }

    /**
     * <p>Reloads all managed configurations from their respective files, discarding
     * any unsaved changes across all configurations.</p>
     *
     * <pre><code>
     * manager.reloadAllConfigs();
     * // All configurations now reflect current file contents
     * </code></pre>
     */
    public void reloadAllConfigs() {
        for (Config config : configs.values()) {
            config.reload();
        }
    }

    /**
     * <p>Saves all managed configurations to their respective files, persisting
     * any pending changes across all configurations.</p>
     *
     * <pre><code>
     * manager.setConfigValue("config1", "setting", "value1");
     * manager.setConfigValue("config2", "setting", "value2");
     * manager.saveAllConfigs();
     * // All changes are now persisted
     * </code></pre>
     *
     * @throws RuntimeException if any configuration cannot be saved
     */
    public void saveAllConfigs() {
        for (Config config : configs.values()) {
            try {
                config.save();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * <p>Permanently deletes all managed configuration files and clears the
     * configuration registry. This operation cannot be undone.</p>
     *
     * <pre><code>
     * manager.deleteAllConfigs();
     * // All configuration files deleted and manager cleared
     * </code></pre>
     */
    public void deleteAllConfigs() {
        for (Config config : configs.values()) {
            config.delete();
        }
        configs.clear();
    }
}

