package de.happybavarian07.coolstufflib;

import de.happybavarian07.coolstufflib.commandmanagement.CommandManagerRegistry;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.languagemanager.PerPlayerLanguageHandler;
import de.happybavarian07.coolstufflib.menusystem.MenuAddonManager;
import de.happybavarian07.coolstufflib.utils.PluginFileLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.function.Consumer;

public class CoolStuffLibBuilder {
    private JavaPlugin javaPluginUsingLib;
    private LanguageManager languageManager = null;
    private CommandManagerRegistry commandManagerRegistry = null;
    private MenuAddonManager menuAddonManager = null;
    private PluginFileLogger pluginFileLogger = null;
    private boolean usePlayerLangHandler = false;
    private Consumer<Object[]> languageManagerStartingMethod = (args) -> {
        if (args.length != 4) return;
        LanguageManager languageManager = (LanguageManager) args[0];
        JavaPlugin javaPluginUsingLib = (JavaPlugin) args[1];
        boolean usePlayerLangHandler = (boolean) args[2];
        File dataFile = (File) args[3];

        if (usePlayerLangHandler) {
            FileConfiguration dataYML = YamlConfiguration.loadConfiguration(dataFile);
            languageManager.setPLHandler(new PerPlayerLanguageHandler(languageManager, dataFile, dataYML));
        }

        // Language Manager Enabling
        //LanguageFile deLang = new LanguageFile(javaPluginUsingLib, "de");
        //LanguageFile enLang = new LanguageFile(javaPluginUsingLib, "en");
        languageManager.addLanguagesToList(true);
        //languageManager.addLang(deLang, deLang.getLangName());
        //languageManager.addLang(enLang, enLang.getLangName());
        //languageManager.setCurrentLang(languageManager.getLang(javaPluginUsingLib.getConfig().getString("Plugin.language"), true), true);
        /*if (languageManager.getCurrentLang() != null) {
            Bukkit.getServer().getConsoleSender().sendMessage(languageManager.getMessage("Plugin.EnablingMessage", null, true));
        } else {
            Bukkit.getServer().getConsoleSender().sendMessage(Utils.chat("&f[&aCool&eStuff&6Lib&f] &cLanguage Manager is not enabled, " +
                    "which means all the Items and Messages won't work! " +
                    "The Plugin will automatically unload! " +
                    "Look for Errors from " + javaPluginUsingLib.getName() + " or the CoolStuffLib in the Console!"));
            Bukkit.getPluginManager().disablePlugin(javaPluginUsingLib);
        }*/
    };
    private Consumer<Object[]> commandManagerRegistryStartingMethod = (args) -> {
        if (args.length != 2) return;
        CommandManagerRegistry commandManagerRegistry = (CommandManagerRegistry) args[0];
        LanguageManager lgm = (LanguageManager) args[1];
        commandManagerRegistry.setLanguageManager(lgm);
        commandManagerRegistry.setCommandManagerRegistryReady(true);

    };
    private Consumer<Object[]> menuAddonManagerStartingMethod = (args) -> {
        if (args.length != 1) return;
        MenuAddonManager menuAddonManager = (MenuAddonManager) args[0];
        menuAddonManager.setMenuAddonManagerReady(true);
    };
    private File dataFile = null;

    /**
     * The CoolStuffLibBuilder function is used to create a new CoolStuffLibBuilder object.
     * The CoolStuffLibBuilder object can be used to build the CoolStuffLib library,
     * which is needed for the Plugin using it.
     *
     *
     * @param javaPluginUsingLib Get the plugin's datafolder
     */
    public CoolStuffLibBuilder(JavaPlugin javaPluginUsingLib) {
        this.javaPluginUsingLib = javaPluginUsingLib;
        if (this.javaPluginUsingLib == null) {
            throw new RuntimeException("CoolStuffLib did not find a Plugin it got called from. Returning. Report the Issue to the Plugin Dev(s), that may have programmed the Plugin.");
        }
    }

    /**
     * The setJavaPluginUsingLib function sets the javaPluginUsingLib variable to the value of its parameter.
     *
     *
     * @param javaPluginUsingLib Set the javapluginusinglib field
     *
     * @return A reference to the builder object itself
     */
    public CoolStuffLibBuilder setJavaPluginUsingLib(JavaPlugin javaPluginUsingLib) {
        this.javaPluginUsingLib = javaPluginUsingLib;
        return this;
    }

    public CoolStuffLibBuilder setLanguageManager(LanguageManager languageManager) {
        this.languageManager = languageManager;
        return this;
    }

    public CoolStuffLibBuilder setCommandManagerRegistry(CommandManagerRegistry commandManagerRegistry) {
        this.commandManagerRegistry = commandManagerRegistry;
        return this;
    }

    public CoolStuffLibBuilder setMenuAddonManager(MenuAddonManager menuAddonManager) {
        this.menuAddonManager = menuAddonManager;
        return this;
    }

    public CoolStuffLibBuilder setPluginFileLogger(PluginFileLogger pluginFileLogger) {
        this.pluginFileLogger = pluginFileLogger;
        return this;
    }

    public CoolStuffLibBuilder setUsePlayerLangHandler(boolean usePlayerLangHandler) {
        this.usePlayerLangHandler = usePlayerLangHandler;
        return this;
    }

    public CoolStuffLibBuilder setLanguageManagerStartingMethod(Consumer<Object[]> languageManagerStartingMethod) {
        this.languageManagerStartingMethod = languageManagerStartingMethod;
        return this;
    }

    public CoolStuffLibBuilder setCommandManagerRegistryStartingMethod(Consumer<Object[]> commandManagerRegistryStartingMethod) {
        this.commandManagerRegistryStartingMethod = commandManagerRegistryStartingMethod;
        return this;
    }

    public CoolStuffLibBuilder setMenuAddonManagerStartingMethod(Consumer<Object[]> menuAddonManagerStartingMethod) {
        this.menuAddonManagerStartingMethod = menuAddonManagerStartingMethod;
        return this;
    }

    public CoolStuffLibBuilder setDataFile(File dataFile) {
        this.dataFile = dataFile;
        return this;
    }

    public CoolStuffLib createCoolStuffLib() {
        if (this.javaPluginUsingLib == null) {
            throw new RuntimeException("CoolStuffLib did not find a Plugin it got called from. Returning. Report the Issue to the Plugin Dev(s), that may have programmed the Plugin.");
        }
        return new CoolStuffLib(javaPluginUsingLib,
                languageManager,
                commandManagerRegistry,
                menuAddonManager,
                pluginFileLogger,
                usePlayerLangHandler,
                languageManagerStartingMethod,
                commandManagerRegistryStartingMethod,
                menuAddonManagerStartingMethod,
                dataFile);
    }
}