package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PerPlayerLanguageHandler {
    private final LanguageManager lgm;
    private final File dataFile;
    private final FileConfiguration dataConfig;

    /**
     * <p>Creates a new per-player language handler.</p>
     *
     * @param lgm        The language manager
     * @param dataFile   The data file for storage
     * @param dataConfig The configuration object for the data file
     */
    public PerPlayerLanguageHandler(LanguageManager lgm, File dataFile, FileConfiguration dataConfig) {
        this.lgm = lgm;
        this.dataFile = dataFile;
        this.dataConfig = dataConfig;
    }

    /**
     * <p>Retrieves the language name for a player.</p>
     *
     * @param uuid The player UUID
     * @return The language name
     */
    public String getPlayerLanguageName(UUID uuid) {
        return dataConfig.getString("playerdata." + uuid.toString() + ".language", lgm.getCurrentLangName());
    }

    /**
     * <p>Retrieves the language file for a player.</p>
     *
     * @param uuid The player UUID
     * @return The {@link LanguageFile}
     */
    public LanguageFile getPlayerLanguage(UUID uuid) {
        return lgm.getLang(dataConfig.getString("playerdata." + uuid.toString() + ".language", lgm.getCurrentLangName()), true);
    }

    /**
     * <p>Retrieves all player language mappings.</p>
     *
     * @return A map of player UUIDs to language files
     */
    public Map<UUID, LanguageFile> getPlayerLanguages() {
        Map<UUID, LanguageFile> playerLangs = new HashMap<>();
        for(String configSec : dataConfig.getConfigurationSection("playerdata").getKeys(false)) {
            playerLangs.put(UUID.fromString(configSec),
                    lgm.getLang(dataConfig.getString("playerdata." + UUID.fromString(configSec).toString() + ".language",
                            lgm.getCurrentLangName()), true));
        }
        return playerLangs;
    }

    /**
     * <p>Sets the preferred language for a player.</p>
     *
     * @param uuid     The player UUID
     * @param language The language name
     */
    public void setPlayerLanguage(UUID uuid, String language) {
        if(lgm.getLang(language, false) == null) language = lgm.getCurrentLangName();
        dataConfig.set("playerdata." + uuid.toString() + ".language", language);

        saveConfig();
    }

    /**
     * <p>Removes the preferred language for a player.</p>
     *
     * @param uuid The player UUID
     */
    public void removePlayerLanguage(UUID uuid) {
        dataConfig.set("playerdata." + uuid.toString(), null);

        saveConfig();
    }

    /**
     * <p>Saves the current configuration to disk.</p>
     */
    public void saveConfig() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
