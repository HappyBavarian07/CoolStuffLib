package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class LanguageFile {

    private final JavaPlugin plugin;
    private final File langFile;
    private final String langName;
    private final LanguageConfig langConfig;

    public LanguageFile(JavaPlugin plugin, File langFolder, String resourceDirectory, String langName) {
        this.plugin = plugin;
        this.langFile = new File(langFolder,langName + ".yml");
        this.langName = langName;
        this.langConfig = new LanguageConfig(this.langFile, langFolder, resourceDirectory, this.langName, this.plugin);
    }

    public File getLangFile() {
        return langFile;
    }

    public String getLangName() {
        return langName;
    }

    public LanguageConfig getLangConfig() {
        return langConfig;
    }

    public String getFullName() {
        return langConfig.getConfig().getString("LanguageFullName");
    }

    public String getFileVersion() {
        return langConfig.getConfig().getString("LanguageVersion");
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}
