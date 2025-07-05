package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class LanguageFile {
    private final File langFile;
    private final String langName;
    private final LanguageConfig langConfig;

    /**
     * The LanguageFile function is used to create a new LanguageFile object.
     *
     *
     * @param langFolder Get the path to the language folder
     * @param resourceDirectory Specify the directory in which the language file is located
     * @param langName Set the name of the language file
     */
    public LanguageFile(File langFolder, String resourceDirectory, String langName) {
        this.langFile = new File(langFolder,langName + ".yml");
        this.langName = langName;
        this.langConfig = new LanguageConfig(this.langFile, langFolder, resourceDirectory, this.langName);
    }

    /**
     * The getLangFile function returns the langFile variable.
     *
     *
     *
     * @return The langfile variable
     */
    public File getLangFile() {
        return langFile;
    }

    /**
     * The getLangName function returns the name of the language.
     *
     *
     *
     * @return The value of the langname variable
     */
    public String getLangName() {
        return langName;
    }

    /**
     * The getLangConfig function returns the language configuration object.
     *
     *
     *
     * @return The language configuration object
     */
    public LanguageConfig getLangConfig() {
        return langConfig;
    }

    /**
     * The getFullName function returns the full name of the language.
     *
     *
     *
     * @return The full name of the language
     */
    public String getFullName() {
        return langConfig.getConfig().getString("LanguageFullName");
    }

    /**
     * The getFileVersion function returns the version of the language file.
     *
     *
     *
     * @return The version of the language file
     */
    public String getFileVersion() {
        return langConfig.getConfig().getString("LanguageVersion");
    }
}
