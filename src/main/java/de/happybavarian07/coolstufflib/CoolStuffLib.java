package de.happybavarian07.coolstufflib;

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
import de.happybavarian07.coolstufflib.service.api.ServiceRegistry;
import de.happybavarian07.coolstufflib.service.api.ServiceDescriptor;
import de.happybavarian07.coolstufflib.service.api.ServiceState;
import de.happybavarian07.coolstufflib.service.api.Service;
import de.happybavarian07.coolstufflib.service.impl.ChatInputService;
import de.happybavarian07.coolstufflib.service.impl.DefaultServiceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CoolStuffLib {
    private static final Logger LOGGER = Logger.getLogger("CoolStuffLib");
    private static CoolStuffLib lib;
    private final JavaPlugin javaPluginUsingLib;
    private final ServiceRegistry serviceRegistry;
    private final LanguageManager languageManager;
    private final CommandManagerRegistry commandManagerRegistry;
    private final MenuAddonManager menuAddonManager;
    private final RepositoryManager repositoryManager;
    private final CacheManager cacheManager;
    private final BackupManager backupManager;
    private final PluginFileLogger pluginFileLogger;
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
     * Initializes the CoolStuffLib library core.
     *
     * @param javaPluginUsingLib                   The plugin instance using this library.
     * @param languageManager                      The language manager.
     * @param commandManagerRegistry               The command manager registry.
     * @param menuAddonManager                     The menu addon manager.
     * @param repositoryManager                    The repository manager.
     * @param cacheManager                         The cache manager.
     * @param backupManager                        The backup manager.
     * @param pluginFileLogger                     The plugin logger.
     * @param usePlayerLangHandler                 Whether to use per-player language handling.
     * @param sendSyntaxOnArgsZero                 Whether to send syntax when no args are provided.
     * @param languageManagerStartingMethod        Initialization logic for the language manager.
     * @param commandManagerRegistryStartingMethod Initialization logic for the command manager registry.
     * @param menuAddonManagerStartingMethod       Initialization logic for the menu addon manager.
     * @param repositoryManagerStartingMethod      Initialization logic for the repository manager.
     * @param cacheManagerStartingMethod           Initialization logic for the cache manager.
     * @param backupManagerStartingMethod          Initialization logic for the backup manager.
     * @param dataFile                             The data file used for persistent storage.
     */
    protected CoolStuffLib(JavaPlugin javaPluginUsingLib, LanguageManager languageManager, CommandManagerRegistry commandManagerRegistry, MenuAddonManager menuAddonManager, RepositoryManager repositoryManager, CacheManager cacheManager, BackupManager backupManager, PluginFileLogger pluginFileLogger, boolean usePlayerLangHandler, boolean sendSyntaxOnArgsZero, Consumer<Object[]> languageManagerStartingMethod, Consumer<Object[]> commandManagerRegistryStartingMethod, Consumer<Object[]> menuAddonManagerStartingMethod, Consumer<Object[]> repositoryManagerStartingMethod, Consumer<Object[]> cacheManagerStartingMethod, Consumer<Object[]> backupManagerStartingMethod, File dataFile) {
        if (javaPluginUsingLib == null) {
            throw new RuntimeException("CoolStuffLib did not find a Plugin it got called from.");
        }
        if (lib != null) {
            LOGGER.warning("CoolStuffLib is being re-initialized; the previous singleton instance (from "
                    + lib.javaPluginUsingLib.getName() + ") will be replaced. getLib() will now return the new instance.");
        }
        lib = this;
        this.javaPluginUsingLib = javaPluginUsingLib;
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
        this.serviceRegistry = new DefaultServiceRegistry();
    }

    /**
     * Retrieves the singleton instance of CoolStuffLib.
     * <p>Throws a RuntimeException if the library has not been initialized yet.</p>
     *
     * @return The CoolStuffLib instance.
     */
    public static CoolStuffLib getLib() {
        if(lib == null) throw new RuntimeException("CoolStuffLib has not been initialized yet.");
        return lib;
    }

    /**
     * Initializes all core systems and managers.
     *
     * <p>Enables components like LanguageManager and CommandManagerRegistry, and registers required event listeners.</p>
     * <pre><code>
     * coolStuffLib.setup();
     * </code></pre>
     */
    public void setup() {
        LogPrefix.setup();
        Map<String, Service> coreServices = new LinkedHashMap<>();
        coreServices.put("language-manager", languageManager);
        coreServices.put("command-manager-registry", commandManagerRegistry);
        coreServices.put("menu-addon-manager", menuAddonManager);
        coreServices.put("repository-manager", repositoryManager);
        coreServices.put("cache-manager", cacheManager);
        coreServices.put("backup-manager", backupManager);
        coreServices.put("chat-input-service", new ChatInputService());
        coreServices.forEach((name, service) -> {
            if (service != null) serviceRegistry.register(ServiceDescriptor.of(name), service, null);
        });

        try {
            serviceRegistry.startAll().join();
        } catch (CompletionException e) {
            List<String> failed = serviceRegistry.snapshotStatesByName().entrySet().stream()
                    .filter(entry -> entry.getValue() == ServiceState.FAILED)
                    .map(Map.Entry::getKey)
                    .sorted()
                    .toList();
            throw new IllegalStateException("CoolStuffLib setup failed; services that failed to start: " + failed, e.getCause());
        }

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

    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    /**
     * <p>Retrieves a service from the registry by name and class. If the service is not registered,
     * it will attempt to instantiate it via its no-argument constructor, register it, and start it.
     * If the service is registered but not running, it will attempt to start it. Blocks until started.</p>
     *
     * @param name  The service name.
     * @param clazz The service class.
     * @param <T>   The service type.
     * @return A guaranteed running instance of the service.
     * @throws IllegalStateException if the name is bound to a different type, or the service cannot be created
     */
    public <T extends Service> T requireService(String name, Class<T> clazz) {
        Service existing = serviceRegistry.getByName(name).orElse(null);
        T service;
        if (existing == null) {
            try {
                service = clazz.getDeclaredConstructor().newInstance();
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Service '" + name + "' is not registered and " + clazz.getName()
                        + " has no no-arg constructor; register it with the ServiceRegistry first.", e);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to instantiate service '" + name + "' (" + clazz.getName() + ")", e);
            }
            serviceRegistry.register(ServiceDescriptor.of(name), service, null);
        } else if (clazz.isInstance(existing)) {
            service = clazz.cast(existing);
        } else {
            throw new IllegalStateException("Service '" + name + "' is registered as " + existing.getClass().getName()
                    + ", not " + clazz.getName());
        }
        UUID id = serviceRegistry.getIdByName(name);
        ServiceState state = serviceRegistry.getState(id);
        if (state == ServiceState.REGISTERED || state == ServiceState.STOPPED || state == ServiceState.FAILED) {
            serviceRegistry.start(id).join();
        }
        return service;
    }

    /**
     * <p>Logs an error to the plugin file log (when the library and its file logger are initialized)
     * and to the console. Safe to call before initialization.</p>
     *
     * @param message what failed
     * @param error   the cause
     */
    public static void logError(String message, Throwable error) {
        CoolStuffLib current = lib;
        if (current != null && current.pluginFileLogger != null) {
            current.pluginFileLogger.writeToLog(Level.SEVERE, message + ": " + error, LogPrefix.ERROR, false);
        }
        LOGGER.log(Level.SEVERE, message, error);
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
     * Writes a message to the log file if enabled.
     *
     * @param info          The log level.
     * @param logMessage    The message to log.
     * @param logPrefix     The prefix to apply.
     * @param sendToConsole Whether to output to the console.
     */
    public void writeToLog(Level info, String logMessage, LogPrefix logPrefix, boolean sendToConsole) {
        if (pluginFileLogger != null) {
            pluginFileLogger.writeToLog(info, logMessage, logPrefix, sendToConsole);
            return;
        }
        if (sendToConsole || info.intValue() >= Level.WARNING.intValue()) {
            LOGGER.log(info, logMessage);
        }
    }

    /**
     * Retrieves or creates a PlayerMenuUtility for the specified player.
     *
     * @param player The UUID of the player.
     * @return The PlayerMenuUtility instance.
     */
    public PlayerMenuUtility getPlayerMenuUtility(UUID player) {
        PlayerMenuUtility playerMenuUtility;
        if (!(playerMenuUtilityMap.containsKey(player))) {
            playerMenuUtility = new PlayerMenuUtility(this, player);
            playerMenuUtilityMap.put(player, playerMenuUtility);
            return playerMenuUtility;
        } else {
            return playerMenuUtilityMap.get(player);
        }
    }

    /**
     * Creates a new PlayerMenuUtility for the specified player, overwriting any existing instance.
     *
     * @param player    The UUID of the player.
     * @param addToList Whether to store the utility in the internal map.
     * @return The created PlayerMenuUtility.
     */
    public PlayerMenuUtility createPlayerMenuUtility(UUID player, boolean addToList) {
        PlayerMenuUtility playerMenuUtility = new PlayerMenuUtility(this, player);
        if (addToList) playerMenuUtilityMap.put(player, playerMenuUtility);

        return playerMenuUtility;
    }

    /**
     * Removes the PlayerMenuUtility for the specified player from the cache.
     *
     * @param player The UUID of the player.
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
