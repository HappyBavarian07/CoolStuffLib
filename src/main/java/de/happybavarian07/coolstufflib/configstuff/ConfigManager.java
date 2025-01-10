package de.happybavarian07.coolstufflib.configstuff;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final JavaPlugin plugin;
    private final Map<String, Config> configs = new HashMap<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
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
        File configFile = new File(plugin.getDataFolder(), fileName + ".yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource(fileName + ".yml", false);
        }
        Config config = new Config(configFile);
        configs.put(configName, config);
        return config;
    }

    public Config createConfig(String configName, File configFile) {
        configFile = new File(plugin.getDataFolder(), configFile.getPath());
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
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
        File configFile = new File(plugin.getDataFolder(), fileName + ".yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        configs.put(configName, new Config(configFile));
    }

    public void loadConfig(String configName, File configFile) {
        configFile = new File(plugin.getDataFolder(), configFile.getPath());
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        configs.put(configName, new Config(configFile));
    }

    public void loadConfigFromResource(String configName, String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName + ".yml");
        if (!configFile.exists()) {
            plugin.saveResource(fileName + ".yml", false);
        }
        configs.put(configName, new Config(configFile));
    }

    public void saveConfig(String configName) {
        if (configs.containsKey(configName)) {
            try {
                configs.get(configName).save();
            } catch (IOException e) {
                e.printStackTrace();
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
                e.printStackTrace();
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