package de.happybavarian07.coolstufflib;/*
 * @Author HappyBavarian07
 * @Date 24.04.2023 | 17:14
 */

import de.happybavarian07.coolstufflib.commandmanagement.CommandManagerRegistry;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.menusystem.MenuAddonManager;
import de.happybavarian07.coolstufflib.menusystem.MenuListener;
import de.happybavarian07.coolstufflib.menusystem.PlayerMenuUtility;
import de.happybavarian07.coolstufflib.repository.RepositoryManager;
import de.happybavarian07.coolstufflib.cache.CacheManager;
import de.happybavarian07.coolstufflib.backupmanager.BackupManager;
import de.happybavarian07.coolstufflib.testing.LibraryTestInitializer;
import de.happybavarian07.coolstufflib.utils.LogPrefix;
import de.happybavarian07.coolstufflib.utils.PluginFileLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

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
    private final RepositoryManager repositoryManager;
    private final CacheManager cacheManager;
    private final BackupManager backupManager;
    private final PluginFileLogger pluginFileLogger;
    // Directory of the Plugin using this Lib
    private final File workingDirectory;
    private final boolean usePlayerLangHandler;
    private final boolean sendSyntaxOnArgsZero;
    private final Consumer<Object[]> languageManagerStartingMethod;
    private final Consumer<Object[]> commandManagerRegistryStartingMethod;
    private final Consumer<Object[]> menuAddonManagerStartingMethod;
    private final Consumer<Object[]> repositoryManagerStartingMethod;
    private final Consumer<Object[]> cacheManagerStartingMethod;
    private final Consumer<Object[]> backupManagerStartingMethod;
    private final File dataFile;
    private final Map<UUID, PlayerMenuUtility> playerMenuUtilityMap = new HashMap<>();
    private boolean languageManagerEnabled = false;
    private boolean commandManagerRegistryEnabled = false;
    private boolean menuAddonManagerEnabled = false;
    private boolean repositoryManagerEnabled = false;
    private boolean cacheManagerEnabled = false;
    private boolean backupManagerEnabled = false;
    private boolean placeholderAPIEnabled = false;
    private LibraryTestInitializer testInitializer;


    /**
     * Initializes the CoolStuffLib with the provided parameters.
     * For a full tutorial on using this library, refer to [insert link here].
     * Additionally, a video explaining the library and demonstrating an example is available.
     *
     * @param javaPluginUsingLib                   Your Java Plugin instance for integration.
     * @param languageManager                      The Language Manager Class.
     * @param commandManagerRegistry               The Command Manager Registry Class.
     * @param menuAddonManager                     The Menu Addon Manager Class.
     * @param repositoryManager                    The Repository Manager Class.
     * @param cacheManager                         The Cache Manager Class.
     * @param backupManager                        The Backup Manager Class.
     * @param pluginFileLogger                     The Plugin File Logger for logging purposes.
     * @param usePlayerLangHandler                 A boolean indicating if a PlayerLangHandler should be used.
     * @param sendSyntaxOnArgsZero                 A boolean indicating if the syntax should be sent when the command is executed with no arguments.
     * @param languageManagerStartingMethod        The method to execute when the Language Manager System is initiated.
     * @param commandManagerRegistryStartingMethod The method to execute when the Command Manager System is initiated.
     * @param menuAddonManagerStartingMethod       The method to execute when the Menu Manager System is initiated.
     * @param repositoryManagerStartingMethod      The method to execute when the Repository Manager System is initiated.
     * @param cacheManagerStartingMethod           The method to execute when the Cache Manager System is initiated.
     * @param backupManagerStartingMethod          The method to execute when the Backup Manager System is initiated.
     * @param dataFile                             The data file for storing important data.
     */
    protected CoolStuffLib(JavaPlugin javaPluginUsingLib,
                           LanguageManager languageManager,
                           CommandManagerRegistry commandManagerRegistry,
                           MenuAddonManager menuAddonManager,
                           RepositoryManager repositoryManager,
                           CacheManager cacheManager,
                           BackupManager backupManager,
                           PluginFileLogger pluginFileLogger,
                           boolean usePlayerLangHandler,
                           boolean sendSyntaxOnArgsZero,
                           Consumer<Object[]> languageManagerStartingMethod,
                           Consumer<Object[]> commandManagerRegistryStartingMethod,
                           Consumer<Object[]> menuAddonManagerStartingMethod,
                           Consumer<Object[]> repositoryManagerStartingMethod,
                           Consumer<Object[]> cacheManagerStartingMethod,
                           Consumer<Object[]> backupManagerStartingMethod,
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
        this.repositoryManager = repositoryManager;
        this.cacheManager = cacheManager;
        this.backupManager = backupManager;
        this.pluginFileLogger = pluginFileLogger;
        this.usePlayerLangHandler = usePlayerLangHandler;
        this.sendSyntaxOnArgsZero = sendSyntaxOnArgsZero;
        this.languageManagerStartingMethod = languageManagerStartingMethod;
        this.commandManagerRegistryStartingMethod = commandManagerRegistryStartingMethod;
        this.menuAddonManagerStartingMethod = menuAddonManagerStartingMethod;
        this.repositoryManagerStartingMethod = repositoryManagerStartingMethod;
        this.cacheManagerStartingMethod = cacheManagerStartingMethod;
        this.backupManagerStartingMethod = backupManagerStartingMethod;
        this.dataFile = dataFile;
    }

    public static @Nullable CoolStuffLib getLib() {
        return lib;
    }

    /**
     * <p>Initializes all core managers and addons for the plugin.</p>
     * <ul>
     *   <li>Enables LanguageManager, CommandManagerRegistry, and MenuAddonManager if present</li>
     *   <li>Checks for PlaceholderAPI and sets internal state</li>
     *   <li>Registers MenuListener events</li>
     * </ul>
     * <pre><code>
     * coolStuffLib.setup();
     * </code></pre>
     */
    public void setup() {
        if (languageManager != null) {
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                placeholderAPIEnabled = true;
            }
            File langDir = languageManager.getLangFolder();
            if (!langDir.isDirectory()) langDir.mkdirs();
            executeMethod(languageManagerStartingMethod, languageManager, javaPluginUsingLib, usePlayerLangHandler, dataFile);
            languageManagerEnabled = true;
        }
        if (commandManagerRegistry != null) {
            executeMethod(commandManagerRegistryStartingMethod, commandManagerRegistry, languageManager);
            commandManagerRegistryEnabled = true;
        }
        if (menuAddonManager != null) {
            executeMethod(menuAddonManagerStartingMethod, menuAddonManager);
            menuAddonManagerEnabled = true;
        }
        if (repositoryManager != null) {
            executeMethod(repositoryManagerStartingMethod, repositoryManager);
            repositoryManagerEnabled = true;
        }
        if (cacheManager != null) {
            executeMethod(cacheManagerStartingMethod, cacheManager);
            cacheManagerEnabled = true;
        }
        if (backupManager != null) {
            executeMethod(backupManagerStartingMethod, backupManager);
            backupManagerEnabled = true;
        }
        if (pluginFileLogger != null) {
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

    public RepositoryManager getRepositoryManager() {
        return repositoryManager;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public BackupManager getBackupManager() {
        return backupManager;
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

    public Consumer<Object[]> getRepositoryManagerStartingMethod() {
        return repositoryManagerStartingMethod;
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

    public boolean isRepositoryManagerEnabled() {
        return repositoryManagerEnabled;
    }

    public boolean isCacheManagerEnabled() {
        return cacheManagerEnabled;
    }

    public boolean isBackupManagerEnabled() {
        return backupManagerEnabled;
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
     * @return The pluginfilelogger variable
     */
    public PluginFileLogger getPluginFileLogger() {
        return pluginFileLogger;
    }

    /**
     * The writeToLog function is used to write a message to the log file.
     *
     * @param info          Define the log level, which is used to filter out some messages
     * @param logMessage    Write the message to the log
     * @param logPrefix     Add a prefix to the log message
     * @param sendToConsole Determine if the message should be sent to the console
     */
    public void writeToLog(Level info, String logMessage, LogPrefix logPrefix, boolean sendToConsole) {
        if (pluginFileLogger != null) {
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
     *
     * @param player Get the player, whose PlayerMenuUtility object should be returned
     * @return The PlayerMenuUtility object of the player
     * @see PlayerMenuUtility
     * @see MenuListener
     * @see MenuAddonManager
     * @see de.happybavarian07.coolstufflib.menusystem.Menu
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
     *
     * @param player    Get the player, whose PlayerMenuUtility object should be created
     * @param addToList Determine if the PlayerMenuUtility object should be added to the hashmap
     * @return The PlayerMenuUtility object of the player
     * @see PlayerMenuUtility
     * @see MenuListener
     * @see MenuAddonManager
     * @see de.happybavarian07.coolstufflib.menusystem.Menu
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
     *
     * @param player Get the player, whose PlayerMenuUtility object should be removed
     * @see PlayerMenuUtility
     */
    public void removePlayerMenuUtility(UUID player) {
        playerMenuUtilityMap.remove(player);
    }

    public Map<UUID, PlayerMenuUtility> getPlayerMenuUtilityMap() {
        return playerMenuUtilityMap;
    }

    public void initializeTests() {
        if (javaPluginUsingLib != null) {
            testInitializer = new LibraryTestInitializer(javaPluginUsingLib);
            testInitializer.executeTests();
        }
    }

    public LibraryTestInitializer getTestInitializer() {
        return testInitializer;
    }
}
