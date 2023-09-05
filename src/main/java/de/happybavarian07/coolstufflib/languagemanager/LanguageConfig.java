package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

public class LanguageConfig {

    private final JavaPlugin plugin;
    private final String langName;
    private File file;
    private FileConfiguration config;
    private final String resourceDirectory;
    private final File langFolder;

    /**
     * The LanguageConfig function is used to create a new LanguageConfig object.
     * This function will also save the default language file if it does not exist,
     * and load the configuration from that file into memory.

     *
     * @param langFile Set the file variable
     * @param langFolder Create the folder where the language file is stored
     * @param resourceDirectory Specify the folder in which the language file is located
     * @param langName Set the name of the language file
     * @param plugin Get the plugin name, which is used to save the default config
     *
     * @return A new languageconfig object
     *
     * @docauthor Trelent
     */
    public LanguageConfig(File langFile, File langFolder, String resourceDirectory, String langName, JavaPlugin plugin) {
        this.plugin = plugin;
        this.langName = langName;
        this.file = langFile;
        this.resourceDirectory = resourceDirectory;
        this.langFolder = langFolder;
        //System.out.println("Creating Language Config: " + this.langName + "  |  " + this.file);
        saveDefaultConfig();
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * The reloadConfig function reloads the config file from disk.
     * It is called when the plugin is enabled, and whenever a player uses /reload.
     */
    public void reloadConfig() {
        if (this.file == null)
            this.file = new File(langFolder, this.langName + ".yml");

        this.config = YamlConfiguration.loadConfiguration(this.file);

        if (this.plugin.getResource(resourceDirectory + "/" + this.langName + ".yml") != null) {
            InputStream defaultStream = this.plugin.getResource(resourceDirectory + "/" + this.langName + ".yml");
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
                this.config.setDefaults(defaultConfig);
            }
        }
    }

    /**
     * The getConfig function is used to get the configuration file for this plugin.
     * If the config file does not exist, it will be created.
     */
    public FileConfiguration getConfig() {
        if (this.config == null)
            reloadConfig();

        return this.config;
    }

    /**
     * The saveConfig function saves the config file to disk.
     */
    public void saveConfig() {
        if (this.config == null || this.file == null)
            return;

        try {
            this.getConfig().save(this.file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save Config to " + this.file, e);
        }
    }

    /**
     * The saveDefaultConfig function is used to save the default configuration file from the plugin's jar.
     * This function will only run if there is no existing config file in the plugins data folder.
     */
    public void saveDefaultConfig() {
        if (this.file == null)
            this.file = new File(langFolder, this.langName + ".yml");

        if (!this.file.exists()) {
            this.plugin.saveResource(resourceDirectory + "/" + this.langName + ".yml", false);
        }
    }

    /**
     * The getLangName function returns the name of the language.
     *
     *
     *
     * @return The name of the language
     */
    public String getLangName() {
        return langName;
    }

    /**
     * The getFile function returns the file that is being read from.
     *
     *
     *
     * @return A file object
     */
    public File getFile() {
        return file;
    }
}
