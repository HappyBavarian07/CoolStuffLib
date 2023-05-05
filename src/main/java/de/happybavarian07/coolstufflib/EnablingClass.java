package de.happybavarian07.coolstufflib;/*
 * @Author HappyBavarian07
 * @Date 24.04.2023 | 17:38
 */

import de.happybavarian07.coolstufflib.languagemanager.LanguageFile;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.languagemanager.PerPlayerLanguageHandler;
import de.happybavarian07.coolstufflib.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public abstract class EnablingClass {
    public void startLanguageManager(LanguageManager languageManager, JavaPlugin javaPlugin, boolean usePLHandler, File dataFile) {
        if(usePLHandler) {
            FileConfiguration dataYML = YamlConfiguration.loadConfiguration(dataFile);
            languageManager.setPlhandler(new PerPlayerLanguageHandler(languageManager, dataFile, dataYML));
        }

        // Language Manager Enabling
        LanguageFile deLang = new LanguageFile(javaPlugin, "de");
        LanguageFile enLang = new LanguageFile(javaPlugin, "en");
        languageManager.addLanguagesToList(true);
        languageManager.addLang(deLang, deLang.getLangName());
        languageManager.addLang(enLang, enLang.getLangName());
        languageManager.setCurrentLang(languageManager.getLang(javaPlugin.getConfig().getString("Plugin.language"), true), true);
        if (languageManager.getCurrentLang() != null) {
            Bukkit.getServer().getConsoleSender().sendMessage(languageManager.getMessage("Plugin.EnablingMessage", null, true));
        } else {
            Bukkit.getServer().getConsoleSender().sendMessage(Utils.chat("&f[&aCool&eStuff&6Lib&f] &cLanguage Manager is not enabled, " +
                    "which means all the Items and Messages won't work! " +
                    "The Plugin will automatically unload! " +
                    "Look for Errors from " + javaPlugin.getName() + " or the CoolStuffLib in the Console!"));
            Bukkit.getPluginManager().disablePlugin(javaPlugin);
        }
    }
    public abstract void startCommandManagerRegistry();
    public abstract void startMenuAddonManager();
}
