package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class LanguageFile {
    private final String langName;
    private final LanguageConfig langConfig;

    /**
     * <p>Creates a new LanguageFile instance.</p>
     *
     * @param langFolder        The language folder
     * @param resourceDirectory The resource directory
     * @param langName          The language name
     */
    public LanguageFile(File langFolder, String resourceDirectory, String langName) {
        this.langName = langName;
        this.langConfig = new LanguageConfig(new File(langFolder, langName + ".yml"), langFolder, resourceDirectory, langName);
    }

    /** The language's folder in the split layout, or its single file before migration. */
    public File getLangFile() {
        return langConfig.getFile();
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
}
