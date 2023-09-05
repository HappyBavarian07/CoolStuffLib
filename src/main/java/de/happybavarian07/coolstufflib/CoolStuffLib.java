package de.happybavarian07.coolstufflib;/*
 * @Author HappyBavarian07
 * @Date 24.04.2023 | 17:14
 */

import de.happybavarian07.coolstufflib.commandmanagement.CommandManagerRegistry;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.menusystem.MenuAddonManager;
import de.happybavarian07.coolstufflib.menusystem.MenuListener;
import de.happybavarian07.coolstufflib.utils.LogPrefix;
import de.happybavarian07.coolstufflib.utils.PluginFileLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.function.Consumer;
import java.util.logging.Level;

public class CoolStuffLib {
    private static CoolStuffLib lib;
    private final JavaPlugin javaPluginUsingLib;
    private final LanguageManager languageManager;
    private final CommandManagerRegistry commandManagerRegistry;
    private final MenuAddonManager menuAddonManager;
    private final PluginFileLogger pluginFileLogger;
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
                           PluginFileLogger pluginFileLogger,
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
        this.pluginFileLogger = pluginFileLogger;
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
     * The setup function is used to initialize the various managers and addons that are being used by the plugin.
     * It will also check if PlaceholderAPI is enabled, and if it is, it will set a boolean value to true so that
     * other parts of the library can use this information. The setup function should be called in onEnable() or after all
     * of your code has been executed in onEnable(). If you do not call this method, then none of your managers or addons will work!
     */
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
        if(pluginFileLogger != null) {
            // Plugin File Logger Init Code
            pluginFileLogger.createLogFile();
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

    /**
     * The getPluginFileLogger function returns the pluginFileLogger object.
     *
     *
     *
     * @return The pluginfilelogger variable
     *
     * @docauthor Trelent
     */
    public PluginFileLogger getPluginFileLogger() {
        return pluginFileLogger;
    }

    /**
     * The writeToLog function is used to write a message to the log file.
     *
     *
     * @param info Define the log level, which is used to filter out some messages
     * @param logMessage Write the message to the log
     * @param logPrefix Add a prefix to the log message
     * @param sendToConsole Determine if the message should be sent to the console
     *
     * @return A boolean, which is true if the message was written to the log file
     *
     * @docauthor Trelent
     */
    public void writeToLog(Level info, String logMessage, LogPrefix logPrefix, boolean sendToConsole) {
        if(pluginFileLogger != null) {
            pluginFileLogger.writeToLog(info, logMessage, logPrefix, sendToConsole);
            return;
        }
        System.out.println("PluginFileLogger is not enabled. Please enable it in the CoolStuffLibBuilder, if you want to see Error Messages and such Things.");
    }
}
