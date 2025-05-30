package de.happybavarian07.coolstufflib.configstuff;

import de.happybavarian07.coolstufflib.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

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

    public Config getConfig(String configName) {
        return configs.get(configName);
    }

    public void reloadConfig(String configName) {
        if (configs.containsKey(configName)) {
            configs.get(configName).reload();
        }
    }

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

    public void deleteConfig(String configName) {
        if (configs.containsKey(configName)) {
            configs.get(configName).delete();
            configs.remove(configName);
        }
    }

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

    public void loadConfigFromResource(String configName, String fileName) {
        String fileNameWithoutExtension = fileName.endsWith(".yml") ? fileName : fileName + ".yml";
        File configFile = new File(configFolder, fileNameWithoutExtension);
        if (!configFile.exists()) {
            Utils.saveResource(configFolder, fileNameWithoutExtension, false);
        }
        configs.put(configName, new Config(configFile));
    }

    public void saveConfig(String configName) {
        if (configs.containsKey(configName)) {
            try {
                configs.get(configName).save();
            } catch (IOException e) {
                throw new RuntimeException("Could not save config: " + configName, e);
            }
        }
    }

    public void setConfigValue(String configName, String path, Object value) {
        if (configs.containsKey(configName)) {
            configs.get(configName).set(path, value);
        }
    }

    public <T> T getConfigValue(String configName, String path, T defaultValue) {
        if (configs.containsKey(configName)) {
            return configs.get(configName).get(path, defaultValue);
        }
        return null;
    }

    public void reloadAllConfigs() {
        for (Config config : configs.values()) {
            config.reload();
        }
    }

    public void saveAllConfigs() {
        for (Config config : configs.values()) {
            try {
                config.save();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void deleteAllConfigs() {
        for (Config config : configs.values()) {
            config.delete();
        }
        configs.clear();
    }


}