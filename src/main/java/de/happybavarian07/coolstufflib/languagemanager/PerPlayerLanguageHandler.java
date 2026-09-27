package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PerPlayerLanguageHandler {
    private final LanguageManager lgm;
    private final PlayerLanguageStore store;

    /**
     * <p>Creates a new per-player language handler.</p>
     *
     * @param lgm        The language manager
     * @param dataFile   The data file for storage
     * @param dataConfig The configuration object for the data file
     */
    public PerPlayerLanguageHandler(LanguageManager lgm, File dataFile, FileConfiguration dataConfig) {
        this(lgm, new YamlPlayerLanguageStore(dataFile, dataConfig));
    }

    public PerPlayerLanguageHandler(LanguageManager lgm, PlayerLanguageStore store) {
        this.lgm = lgm;
        this.store = store;
    }

    public PlayerLanguageStore getStore() {
        return store;
    }

    /**
     * <p>Retrieves the language name for a player.</p>
     *
     * @param uuid The player UUID
     * @return The language name
     */
    public String getPlayerLanguageName(UUID uuid) {
        String language = store.get(uuid);
        return language == null ? lgm.getCurrentLangName() : language;
    }

    /**
     * <p>Retrieves the language file for a player.</p>
     *
     * @param uuid The player UUID
     * @return The {@link LanguageFile}
     */
    public LanguageFile getPlayerLanguage(UUID uuid) {
        String language = store.get(uuid);
        return language == null ? null : lgm.getLang(language, false);
    }

    /**
     * <p>Retrieves all player language mappings.</p>
     *
     * @return A map of player UUIDs to language files
     */
    public Map<UUID, LanguageFile> getPlayerLanguages() {
        Map<UUID, LanguageFile> languages = new HashMap<>();
        store.all().forEach((uuid, name) -> {
            LanguageFile language = lgm.getLang(name, false);
            if (language != null) languages.put(uuid, language);
        });
        return languages;
    }

    /**
     * <p>Sets the preferred language for a player.</p>
     *
     * @param uuid     The player UUID
     * @param language The language name
     */
    public void setPlayerLanguage(UUID uuid, String language) {
        if (lgm.getLang(language, false) == null) language = lgm.getCurrentLangName();
        store.set(uuid, language);
    }

    /**
     * <p>Removes the preferred language for a player.</p>
     *
     * @param uuid The player UUID
     */
    public void removePlayerLanguage(UUID uuid) {
        store.remove(uuid);
    }

    /**
     * <p>Saves the current configuration to disk.</p>
     */
    public void saveConfig() {
        if (store instanceof YamlPlayerLanguageStore yaml) yaml.save();
    }
}
