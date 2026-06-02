package de.happybavarian07.coolstufflib.languagemanager;

import de.happybavarian07.coolstufflib.configstuff.KeyBuilder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>Utility class for updating language files while preserving comments.</p>
 */
public class LanguageFileUpdater {
    private static JavaPlugin javaPluginUsingThisLib;
    private static final char SEPARATOR = '.';

    /**
     * <p>Constructs the updater with the plugin instance.</p>
     *
     * @param javaPluginUsingThisLib The plugin instance
     */
    public LanguageFileUpdater(JavaPlugin javaPluginUsingThisLib) {
        LanguageFileUpdater.javaPluginUsingThisLib = javaPluginUsingThisLib;
    }

    /**
     * <p>Updates a language file based on resource content.</p>
     *
     * @param resourceName The path to the resource file
     * @param toUpdate     The target file to update
     * @throws IOException If an I/O error occurs during update
     */
    public static void update(String resourceName, File toUpdate) throws IOException {
        if (javaPluginUsingThisLib.getResource(resourceName) == null) {
            if (javaPluginUsingThisLib.getResource("languages/" + javaPluginUsingThisLib.getConfig().getString("Plugin.languageForUpdates") + ".yml") != null) {
                resourceName = "languages/" + javaPluginUsingThisLib.getConfig().getString("Plugin.languageForUpdates") + ".yml";
            } else {
                resourceName = "languages/en.yml";
            }
        }
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(javaPluginUsingThisLib.getResource(resourceName), StandardCharsets.UTF_8));
        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(toUpdate);
        Map<String, String> comments = parseComments(javaPluginUsingThisLib, resourceName, defaultConfig);
    }

    private static Map<String, String> parseComments(Plugin plugin, String resourceName, FileConfiguration defaultConfig) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(plugin.getResource(resourceName)));
        Map<String, String> comments = new LinkedHashMap<>();
        StringBuilder commentBuilder = new StringBuilder();
        KeyBuilder keyBuilder = new KeyBuilder(defaultConfig, SEPARATOR);

        String line;
        while ((line = reader.readLine()) != null) {
            String trimmedLine = line.trim();

            if (trimmedLine.startsWith("-")) {
                continue;
            }

            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                commentBuilder.append(trimmedLine).append("\n");
            } else {
                keyBuilder.parseLine(trimmedLine);
                String key = keyBuilder.toString();

                if (commentBuilder.length() > 0) {
                    comments.put(key, commentBuilder.toString());
                    commentBuilder.setLength(0);
                }

                if (!keyBuilder.isConfigSectionWithKeys()) {
                    keyBuilder.removeLastKey();
                }
            }
        }

        reader.close();

        if (commentBuilder.length() > 0)
            comments.put(null, commentBuilder.toString());

        return comments;
    }
}
