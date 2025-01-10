package de.happybavarian07.coolstufflib;/*
 * @Author HappyBavarian07
 * @Date 24.04.2023 | 17:14
 */

import de.happybavarian07.coolstufflib.commandmanagement.CommandManagerRegistry;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.menusystem.MenuAddonManager;
import de.happybavarian07.coolstufflib.menusystem.MenuListener;
import de.happybavarian07.coolstufflib.menusystem.PlayerMenuUtility;
import de.happybavarian07.coolstufflib.utils.LogPrefix;
import de.happybavarian07.coolstufflib.utils.PluginFileLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
    private final boolean sendSyntaxOnArgsZero;
    private final Consumer<Object[]> languageManagerStartingMethod;
    private final Consumer<Object[]> commandManagerRegistryStartingMethod;
    private final Consumer<Object[]> menuAddonManagerStartingMethod;
    private final File dataFile;
    private boolean languageManagerEnabled = false;
    private boolean commandManagerRegistryEnabled = false;
    private boolean menuAddonManagerEnabled = false;
    private boolean placeholderAPIEnabled = false;
    private final Map<UUID, PlayerMenuUtility> playerMenuUtilityMap = new HashMap<>();


    /**
     * Initializes the CoolStuffLib with the provided parameters.
     * For a full tutorial on using this library, refer to [insert link here].
     * Additionally, a video explaining the library and demonstrating an example is available.
     *
     * @param javaPluginUsingLib                   Your Java Plugin instance for integration.
     * @param languageManager                      The Language Manager Class.
     * @param commandManagerRegistry               The Command Manager Registry Class.
     * @param menuAddonManager                     The Menu Addon Manager Class.
     * @param pluginFileLogger                    The Plugin File Logger for logging purposes.
     * @param usePlayerLangHandler                 A boolean indicating if a PlayerLangHandler should be used.
     * @param sendSyntaxOnArgsZero                 A boolean indicating if the syntax should be sent when the command is executed with no arguments.
     * @param languageManagerStartingMethod        The method to execute when the Language Manager System is initiated.
     * @param commandManagerRegistryStartingMethod The method to execute when the Command Manager System is initiated.
     * @param menuAddonManagerStartingMethod       The method to execute when the Menu Manager System is initiated.
     * @param dataFile                             The data file for storing important data.
     */
    protected CoolStuffLib(JavaPlugin javaPluginUsingLib,
                           LanguageManager languageManager,
                           CommandManagerRegistry commandManagerRegistry,
                           MenuAddonManager menuAddonManager,
                           PluginFileLogger pluginFileLogger,
                           boolean usePlayerLangHandler,
                           boolean sendSyntaxOnArgsZero,
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
        this.sendSyntaxOnArgsZero = sendSyntaxOnArgsZero;
        this.languageManagerStartingMethod = languageManagerStartingMethod;
        this.commandManagerRegistryStartingMethod = commandManagerRegistryStartingMethod;
        this.menuAddonManagerStartingMethod = menuAddonManagerStartingMethod;
        this.dataFile = dataFile;
    }

    public static CoolStuffLib getLib() {
        return lib;
    }

    /**
     * Initializes various managers and addons used by the plugin.
     * Checks if PlaceholderAPI is enabled and sets a boolean value accordingly.
     * This function should be called in onEnable() or after all other initialization
     * code in onEnable().
     * <p>
     * Failure to call this method will result in non-functioning managers and addons.
     */
    public void setup() {
        if (languageManager != null) {
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                placeholderAPIEnabled = true;
            }
            // Language Manager Enable Code
            File langDir = languageManager.getLangFolder();
            if (!langDir.isDirectory()) langDir.mkdirs();
            executeMethod(languageManagerStartingMethod, languageManager, javaPluginUsingLib, usePlayerLangHandler, dataFile);
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

    public boolean isSendSyntaxOnArgsZero() {
        return sendSyntaxOnArgsZero;
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
     */
    public void writeToLog(Level info, String logMessage, LogPrefix logPrefix, boolean sendToConsole) {
        if(pluginFileLogger != null) {
            pluginFileLogger.writeToLog(info, logMessage, logPrefix, sendToConsole);
            return;
        }
        System.out.println("PluginFileLogger is not enabled. Please enable it in the CoolStuffLibBuilder, if you want to see Error Messages and such Things.");
    }

    /**
     * The getPlayerMenuUtility function is used to get the PlayerMenuUtility object of a player.
     * <p>
     * If the player doesn't have a PlayerMenuUtility object, it will be created.
     * <p>
     * The Menu API uses this function.
     * @see PlayerMenuUtility
     * @see MenuListener
     * @see MenuAddonManager
     * @see de.happybavarian07.coolstufflib.menusystem.Menu
     *
     * @param player Get the player, whose PlayerMenuUtility object should be returned
     * @return The PlayerMenuUtility object of the player
     */
    public PlayerMenuUtility getPlayerMenuUtility(UUID player) {
        PlayerMenuUtility playerMenuUtility;
        if (!(playerMenuUtilityMap.containsKey(player))) { //See if the player has a playermenuutility "saved" for them

            //This player doesn't. Make one of them and add it to the hashmap
            playerMenuUtility = new PlayerMenuUtility(player);
            playerMenuUtilityMap.put(player, playerMenuUtility);

            return playerMenuUtility;
        } else {
            return playerMenuUtilityMap.get(player); //Return the object by using the provided player
        }
    }

    /**
     * The createPlayerMenuUtility function is used to create a PlayerMenuUtility object for a player.
     * <p>
     * If the player already has a PlayerMenuUtility object, it will be overwritten.
     * <p>
     * The Menu API uses this function.
     * @see PlayerMenuUtility
     * @see MenuListener
     * @see MenuAddonManager
     * @see de.happybavarian07.coolstufflib.menusystem.Menu
     *
     * @param player Get the player, whose PlayerMenuUtility object should be created
     * @param addToList Determine if the PlayerMenuUtility object should be added to the hashmap
     * @return The PlayerMenuUtility object of the player
     */
    public PlayerMenuUtility createPlayerMenuUtility(UUID player, boolean addToList) {
        //This player doesn't. Make one of them and add it to the hashmap
        PlayerMenuUtility playerMenuUtility = new PlayerMenuUtility(player);
        if (addToList)
            playerMenuUtilityMap.put(player, playerMenuUtility);

        return playerMenuUtility;
    }

    /**
     * The removePlayerMenuUtility function is used to remove a PlayerMenuUtility object from the hashmap.
     * <p>
     * The Menu API uses this function.
     * <p>
     * @see PlayerMenuUtility
     *
     * @param player Get the player, whose PlayerMenuUtility object should be removed
     */
    public void removePlayerMenuUtility(UUID player) {
        playerMenuUtilityMap.remove(player);
    }

    public Map<UUID, PlayerMenuUtility> getPlayerMenuUtilityMap() {
        return playerMenuUtilityMap;
    }
}
