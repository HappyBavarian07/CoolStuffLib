package de.happybavarian07.coolstufflib;/*
 * @Author HappyBavarian07
 * @Date 24.04.2023 | 17:14
 */

import de.happybavarian07.coolstufflib.commandmanagement.CommandManagerRegistry;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.menusystem.MenuAddonManager;
import de.happybavarian07.coolstufflib.menusystem.MenuListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.function.Consumer;

public class CoolStuffLib {
    private static CoolStuffLib lib;
    private final JavaPlugin javaPluginUsingLib;
    private final LanguageManager languageManager;
    private final CommandManagerRegistry commandManagerRegistry;
    private final MenuAddonManager menuAddonManager;
    // Directory of the Plugin using this Lib
    private final File workingDirectory;
    private final boolean usePlayerLangHandler;
    private final Consumer<Object[]> languageManagerStartingMethod;
    private final Consumer<Object[]> commandManagerRegistryStartingMethod;
    private final Consumer<Object[]> menuAddonManagerStartingMethod;
    private final File dataFile;
    private boolean languageManagerEnabled = false;
    private boolean commandManagerRegistryEnabled = false;
    private boolean menuAddonManagerEnabled = false;
    private boolean placeholderAPIEnabled = false;


    /**
     * You can find a full Tutorial on this Lib under this Link: .
     * Here is a Video from me explaining it a bit more and showing one Example.
     *
     * @param javaPluginUsingLib                   You have to set your Java Plugin Here to make it work.
     * @param languageManager                      The Language Manager Class
     * @param commandManagerRegistry               The Command Manager Registry Class
     * @param menuAddonManager                     The Menu Addon Manager Class
     * @param usePlayerLangHandler                 Boolean if it should use a PlayerLangHandler
     * @param languageManagerStartingMethod        The Method that gets executed when Language Manager System is initiated
     * @param commandManagerRegistryStartingMethod The Method that gets executed when Command Manager System is initiated
     * @param menuAddonManagerStartingMethod       The Method that gets executed when Menu Manager System is initiated
     * @param dataFile                             Data File for important Data
     */
    protected CoolStuffLib(JavaPlugin javaPluginUsingLib,
                           LanguageManager languageManager,
                           CommandManagerRegistry commandManagerRegistry,
                           MenuAddonManager menuAddonManager,
                           boolean usePlayerLangHandler,
                           Consumer<Object[]> languageManagerStartingMethod,
                           Consumer<Object[]> commandManagerRegistryStartingMethod,
                           Consumer<Object[]> menuAddonManagerStartingMethod,
                           File dataFile) {
        lib = this;
        this.javaPluginUsingLib = javaPluginUsingLib;
        if (this.javaPluginUsingLib == null) {
            throw new RuntimeException("CoolStuffLib did not find a Plugin it got called from. Returning. Report the Issue to the Plugin Dev(s), that may have programmed the Plugin.");
        }
        // TODO Add StartUpLogger API, MySQL API (with class that can be inherited and then added into this constructor, Maybe other Things
        this.workingDirectory = javaPluginUsingLib.getDataFolder();
        this.languageManager = languageManager;
        this.commandManagerRegistry = commandManagerRegistry;
        this.menuAddonManager = menuAddonManager;
        this.usePlayerLangHandler = usePlayerLangHandler;
        this.languageManagerStartingMethod = languageManagerStartingMethod;
        this.commandManagerRegistryStartingMethod = commandManagerRegistryStartingMethod;
        this.menuAddonManagerStartingMethod = menuAddonManagerStartingMethod;
        this.dataFile = dataFile;
    }

    public static CoolStuffLib getLib() {
        return lib;
    }

    public void setup() {
        if (languageManager != null) {
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                placeholderAPIEnabled = true;
            }
            // Language Manager Enable Code
            // TODO Add more customizablity for languages folder. Make it editable and make it use the instance of the language manager or smth
            File langDir = new File(javaPluginUsingLib.getDataFolder(), "languages");
            if (!langDir.isDirectory()) langDir.mkdirs();
            executeMethod(languageManagerStartingMethod, languageManager, javaPluginUsingLib, usePlayerLangHandler, dataFile, langDir);
            languageManagerEnabled = true;
        }
        if (commandManagerRegistry != null) {
            // Command Registry Enable Code
            executeMethod(commandManagerRegistryStartingMethod, commandManagerRegistry, languageManager);
            commandManagerRegistryEnabled = true;
        }
        if (menuAddonManager != null) {
            // Menu Addon Manager Init Code
            executeMethod(menuAddonManagerStartingMethod, menuAddonManager);
            menuAddonManagerEnabled = true;
        }
        Bukkit.getPluginManager().registerEvents(new MenuListener(), javaPluginUsingLib);
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public CommandManagerRegistry getCommandManagerRegistry() {
        return commandManagerRegistry;
    }

    public MenuAddonManager getMenuAddonManager() {
        return menuAddonManager;
    }

    public JavaPlugin getJavaPluginUsingLib() {
        return javaPluginUsingLib;
    }

    public File getDataFile() {
        return dataFile;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public Consumer<Object[]> getLanguageManagerStartingMethod() {
        return languageManagerStartingMethod;
    }

    public Consumer<Object[]> getCommandManagerRegistryStartingMethod() {
        return commandManagerRegistryStartingMethod;
    }

    public Consumer<Object[]> getMenuAddonManagerStartingMethod() {
        return menuAddonManagerStartingMethod;
    }

    public boolean isLanguageManagerEnabled() {
        return languageManagerEnabled;
    }

    public boolean isCommandManagerRegistryEnabled() {
        return commandManagerRegistryEnabled;
    }

    public boolean isMenuAddonManagerEnabled() {
        return menuAddonManagerEnabled;
    }

    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }

    public void executeMethod(Consumer<Object[]> method, Object... arguments) {
        method.accept(arguments);
    }
}
