package de.happybavarian07.coolstufflib.configstuff;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
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

    public void reload() {
        this.fileConfiguration = YamlConfiguration.loadConfiguration(configFile);
    }

    public void save() throws IOException {
        fileConfiguration.save(configFile);
    }

    public void set(String path, Object value) {
        fileConfiguration.set(path, value);
    }

    public <T> T get(String path, T defaultValue) {
        Object value = fileConfiguration.get(path, defaultValue);
        if(value != null && value.getClass().isAssignableFrom(defaultValue.getClass())) {
            return (T) value;
        }
        if(defaultValue != null) {
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