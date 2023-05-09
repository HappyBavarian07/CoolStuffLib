package de.happybavarian07.coolstufflib;/*
 * @Author HappyBavarian07
 * @Date 24.04.2023 | 17:14
 */

import de.happybavarian07.coolstufflib.commandmanagement.CommandManagerRegistry;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.menusystem.MenuAddonManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.function.Consumer;

public class CoolStuffLib {
    private static CoolStuffLib lib;
    private final JavaPlugin javaPluginUsingLib;
    private final LanguageManager languageManager;
    private boolean languageManagerEnabled = false;
    private final CommandManagerRegistry commandManagerRegistry;
    private boolean commandManagerRegistryEnabled = false;
    private final MenuAddonManager menuAddonManager;
    private boolean menuAddonManagerEnabled = false;
    private boolean placeholderAPIEnabled = false;
    // Directory of the Plugin using this Lib
    private final File workingDirectory;
    private final boolean usePlayerLangHandler;
    private final Consumer<Object[]> languageManagerStartingMethod;
    private final Consumer<Object[]> commandManagerRegistryStartingMethod;
    private final Consumer<Object[]> menuAddonManagerStartingMethod;
    private final File dataFile;


    /**
     * You can find a full Tutorial on this Lib under this Link: <a></a>.
     * Here is a Video from me explaining it a bit more and showing one Example.</b>
     *
     * @param javaPluginUsingLib     You have to set your Java Plugin Here to make it work.
     * @param languageManager        The Language Manager Class
     * @param commandManagerRegistry The Command Manager Registry Class
     * @param menuAddonManager       The Menu Addon Manager Class
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

    /**
     * @param usePlHandler
     */
    public void setup() {
        if (languageManager != null) {
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                placeholderAPIEnabled = true;
            }
            // Language Manager Enable Code
            executeMethod(languageManagerStartingMethod, languageManager, javaPluginUsingLib, usePlayerLangHandler, dataFile);
            languageManagerEnabled = true;
        }
        if (commandManagerRegistry != null) {
            // Command Registry Enable Code
            enablingClass.startCommandManagerRegistry();
            executeMethod(commandManagerRegistryStartingMethod,);
            commandManagerRegistryEnabled = true;
        }
        if (menuAddonManager != null) {
            // Menu Addon Manager Init Code
            enablingClass.startMenuAddonManager();
            menuAddonManagerEnabled = true;
        }
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
