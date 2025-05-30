package de.happybavarian07.coolstufflib.configstuff;

import com.google.common.base.Charsets;
import de.happybavarian07.coolstufflib.utils.Utils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.List;
import java.util.UUID;

public class Config implements Serializable, Comparable<Config> {
    private final UUID configUUID;
    private final File configFile;
    private FileConfiguration fileConfiguration;

    public Config(File configFile) {
        this.configUUID = UUID.randomUUID();
        this.configFile = configFile;
        this.fileConfiguration = YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getFileConfiguration() {
        return fileConfiguration;
    }

    public UUID getConfigUUID() {
        return configUUID;
    }

    public File getConfigFile() {
        return configFile;
    }

    public void reload() {
        this.fileConfiguration = YamlConfiguration.loadConfiguration(configFile);

        final InputStream defConfigStream = Utils.getResource("config.yml");
        if (defConfigStream == null) {
            return;
        }

        fileConfiguration.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, Charsets.UTF_8)));
    }

    public void save() throws IOException {
        fileConfiguration.save(configFile);
    }

    /**
     * Sets a value in the configuration file at the specified path.
     * You will have to call {@link #save()} to persist the changes to the file.
     *
     * @param path  the path in the configuration file where the value should be set.
     * @param value the value to set at the specified path
     */
    public void set(String path, Object value) {
        fileConfiguration.set(path, value);
    }

    public boolean contains(String path) {
        return fileConfiguration.contains(path);
    }

    public String getString(String path, String defaultValue) {
        return fileConfiguration.getString(path, defaultValue);
    }

    public int getInt(String path, int defaultValue) {
        return fileConfiguration.getInt(path, defaultValue);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        return fileConfiguration.getBoolean(path, defaultValue);
    }

    public double getDouble(String path, double defaultValue) {
        return fileConfiguration.getDouble(path, defaultValue);
    }

    public long getLong(String path, long defaultValue) {
        return fileConfiguration.getLong(path, defaultValue);
    }

    public List<String> getStringList(String path, List<String> defaultValue) {
        List<String> value = fileConfiguration.getStringList(path);
        if (!value.isEmpty()) {
            return value;
        }
        return defaultValue;
    }

    public List<Integer> getIntList(String path, List<Integer> defaultValue) {
        List<Integer> value = fileConfiguration.getIntegerList(path);
        if (!value.isEmpty()) {
            return value;
        }
        return defaultValue;
    }


    public <T> T get(String path, T defaultValue) {
        Object value = fileConfiguration.get(path, defaultValue);
        if (value != null && value.getClass().isAssignableFrom(defaultValue.getClass())) {
            return (T) value;
        }
        if (defaultValue != null) {
            return defaultValue;
        }
        throw new IllegalArgumentException("Value is null or CCE happened and no default value was provided.");
    }

    public void delete() {
        configFile.delete();
    }

    @Override
    public int compareTo(@NotNull Config o) {
        int result = this.configUUID.compareTo(o.configUUID);
        if (result == 0) {
            result = this.configFile.getName().compareTo(o.configFile.getName());
            if (result == 0) {
                result = this.configFile.getPath().compareTo(o.configFile.getPath());
            }
        }
        return result;
    }
}