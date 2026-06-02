package de.happybavarian07.coolstufflib.languagemanager;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.utils.Utils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

public class LanguageConfig {
    private final String langName;
    private File file;
    private FileConfiguration config;
    private final String resourceDirectory;
    private final File langFolder;

    /**
     * <p>Creates a new LanguageConfig instance and initializes it.</p>
     *
     * @param langFile          The language file
     * @param langFolder        The folder containing language files
     * @param resourceDirectory The resource directory
     * @param langName          The language name
     */
    public LanguageConfig(File langFile, File langFolder, String resourceDirectory, String langName) {
        this.langName = langName;
        this.file = langFile;
        this.resourceDirectory = resourceDirectory;
        this.langFolder = langFolder;
        saveDefaultConfig();
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * <p>Reloads the configuration file from disk.</p>
     */
    public void reloadConfig() {
        if (this.file == null)
            this.file = new File(langFolder, this.langName + ".yml");

        this.config = YamlConfiguration.loadConfiguration(this.file);

        if (Utils.getResource(resourceDirectory + "/" + this.langName + ".yml") != null) {
            InputStream defaultStream = Utils.getResource(resourceDirectory + "/" + this.langName + ".yml");
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
                this.config.setDefaults(defaultConfig);
            }
        }
    }

    /**
     * <p>Retrieves the configuration.</p>
     *
     * @return The {@link FileConfiguration}
     */
    public FileConfiguration getConfig() {
        if (this.config == null)
            reloadConfig();

        return this.config;
    }

    /**
     * <p>Saves the configuration to disk.</p>
     */
    public void saveConfig() {
        if (this.config == null || this.file == null)
            return;

        try {
            this.getConfig().save(this.file);
        } catch (IOException e) {
            LanguageManager.getLogger().log(Level.SEVERE, "Could not save Config to " + this.file, e);
        }
    }

    /**
     * <p>Saves the default configuration from resources if it doesn't exist.</p>
     */
    public void saveDefaultConfig() {
        if (this.file == null)
            this.file = new File(langFolder, this.langName + ".yml");

        if (!this.file.exists()) {
            File configDir = CoolStuffLib.getLib() == null ? new File("") : CoolStuffLib.getLib().getWorkingDirectory();
            Utils.saveResource(configDir, resourceDirectory + "/" + this.langName + ".yml", false);
        }
    }

    public String getLangName() {
        return langName;
    }

    public File getFile() {
        return file;
    }
}
