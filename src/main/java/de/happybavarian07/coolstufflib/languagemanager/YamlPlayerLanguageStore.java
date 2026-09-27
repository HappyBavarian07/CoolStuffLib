package de.happybavarian07.coolstufflib.languagemanager;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Stores {@code playerdata.<uuid>.language} in the plugin's data file, the layout used since 1.x. */
public final class YamlPlayerLanguageStore implements PlayerLanguageStore {
    private final File dataFile;
    private final FileConfiguration data;

    public YamlPlayerLanguageStore(File dataFile, FileConfiguration data) {
        this.dataFile = dataFile;
        this.data = data;
    }

    @Override
    public @Nullable String get(UUID player) {
        return data.getString("playerdata." + player + ".language");
    }

    @Override
    public void set(UUID player, String language) {
        data.set("playerdata." + player + ".language", language);
        save();
    }

    @Override
    public void remove(UUID player) {
        data.set("playerdata." + player, null);
        save();
    }

    @Override
    public Map<UUID, String> all() {
        Map<UUID, String> languages = new HashMap<>();
        ConfigurationSection section = data.getConfigurationSection("playerdata");
        if (section == null) return languages;
        for (String key : section.getKeys(false)) {
            String language = section.getString(key + ".language");
            if (language == null) continue;
            try {
                languages.put(UUID.fromString(key), language);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return languages;
    }

    public void save() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            try {
                CoolStuffLib.logError("Failed to save per-player language data", e);
            } catch (RuntimeException notSetUp) {
                LanguageManager.getLogger().log(Level.SEVERE, "Failed to save per-player language data", e);
            }
        }
    }
}
