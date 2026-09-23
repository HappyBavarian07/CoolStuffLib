package de.happybavarian07.coolstufflib.languagemanager;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Utility class for updating older language file versions.</p>
 */
public class OldLanguageFileUpdater {

    /**
     * <p>Checks for missing keys in an old configuration file.</p>
     *
     * @param oldFile        The old file
     * @param newConfig      The new configuration
     * @param nonDefaultLang Whether the language is non-default
     * @return A map of updates
     */
    public Map<String, Object> checkForUpdates(File oldFile, FileConfiguration newConfig, boolean nonDefaultLang) {
        FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldFile);
        Map<String, Object> updates = new HashMap<>();

        if (!nonDefaultLang) {
            if (!oldConfig.contains("LanguageFullName") ||
                    !oldConfig.getString("LanguageFullName", "").equals(newConfig.getString("LanguageFullName", "")))
                updates.put("LanguageFullName", newConfig.getString("LanguageFullName"));
        }
        if (!oldConfig.contains("LanguageVersion") ||
                !oldConfig.getString("LanguageVersion", "").equals(newConfig.getString("LanguageVersion", "")))
            updates.put("LanguageVersion", newConfig.getString("LanguageVersion"));

        if(newConfig.getConfigurationSection("Messages") != null) {
            for (String path : newConfig.getConfigurationSection("Messages").getKeys(true)) {
                if(newConfig.isConfigurationSection("Messages." + path)) {
                    updates.put("Messages." + path, null);
                    continue;
                }
                if (!oldConfig.contains("Messages." + path)) {
                    updates.put("Messages." + path, newConfig.get("Messages." + path));
                }
            }
        }

        if(newConfig.getConfigurationSection("Items") != null) {
            for (String path : newConfig.getConfigurationSection("Items").getKeys(true)) {
                if(newConfig.isConfigurationSection("Items." + path)) {
                    updates.put("Items." + path, null);
                    continue;
                }
                if (!oldConfig.contains("Items." + path)) {
                    updates.put("Items." + path, newConfig.get("Items." + path));
                }
            }
        }

        if(newConfig.getConfigurationSection("MenuTitles") != null) {
            for (String path : newConfig.getConfigurationSection("MenuTitles").getKeys(true)) {
                if(newConfig.isConfigurationSection("MenuTitles." + path)) {
                    updates.put("MenuTitles." + path, null);
                    continue;
                }
                if (!oldConfig.contains("MenuTitles." + path)) {
                    updates.put("MenuTitles." + path, newConfig.get("MenuTitles." + path));
                }
            }
        }
        return updates;
    }

    /**
     * <p>Updates a file with missing configuration keys.</p>
     *
     * @param oldFile        The file to update
     * @param newConfig      The new configuration
     * @param langName       The language name
     * @param nonDefaultLang Whether the language is non-default
     */
    public void updateFile(File oldFile, FileConfiguration newConfig, String langName, boolean nonDefaultLang) {
        FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldFile);
        Map<String, Object> checkedUpdates = checkForUpdates(oldFile, newConfig, nonDefaultLang);
        if (checkedUpdates.isEmpty()) return;
        for (String path : checkedUpdates.keySet()) {
            if ((!oldConfig.contains(path) || oldConfig.get(path) == null)) {
                oldConfig.set(path, checkedUpdates.get(path));
            }
            if(newConfig.isConfigurationSection(path) && !oldConfig.isConfigurationSection(path)) {
                oldConfig.createSection(path);
            }
        }
        oldConfig.options().header(newConfig.options().header());
        try {
            oldConfig.save(oldFile);
        } catch (IOException e) {
            CoolStuffLib.logError("Failed to save updated language file", e);
        }
    }
}
