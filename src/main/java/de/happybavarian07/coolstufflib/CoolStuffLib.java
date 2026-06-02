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
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

public class CoolStuffLib {
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
        lib = this;
        this.javaPluginUsingLib = javaPluginUsingLib;
        if (this.javaPluginUsingLib == null) {
            throw new RuntimeException("CoolStuffLib did not find a Plugin it got called from.");
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
        this.serviceRegistry = new DefaultServiceRegistry();
    }

    public static @Nullable CoolStuffLib getLib() {
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
        // Register services
        ServiceRegistry registry = this.serviceRegistry;
        // TODO maybe change the service descriptor a bit and make it easier to use and not require so much stuff since uuid can be handled by the registry
        // TODO maybe also add a servicecomponent annotation to all the services below
        if (languageManager != null)
            registry.register(new ServiceDescriptor(UUID.randomUUID(), "language-manager", null, Duration.ofSeconds(5), Duration.ofSeconds(5)), languageManager, null);
        if (commandManagerRegistry != null)
            registry.register(new ServiceDescriptor(UUID.randomUUID(), "command-manager-registry", null, Duration.ofSeconds(5), Duration.ofSeconds(5)), commandManagerRegistry, null);
        if (menuAddonManager != null)
            registry.register(new ServiceDescriptor(UUID.randomUUID(), "menu-addon-manager", null, Duration.ofSeconds(5), Duration.ofSeconds(5)), menuAddonManager, null);
        if (repositoryManager != null)
            registry.register(new ServiceDescriptor(UUID.randomUUID(), "repository-manager", null, Duration.ofSeconds(5), Duration.ofSeconds(5)), repositoryManager, null);
        if (cacheManager != null)
            registry.register(new ServiceDescriptor(UUID.randomUUID(), "cache-manager", null, Duration.ofSeconds(5), Duration.ofSeconds(5)), cacheManager, null);
        if (backupManager != null)
            registry.register(new ServiceDescriptor(UUID.randomUUID(), "backup-manager", null, Duration.ofSeconds(5), Duration.ofSeconds(5)), backupManager, null);

        ChatInputService chatInputService = new ChatInputService();
        registry.register(new ServiceDescriptor(UUID.randomUUID(), "chat-input-service", null, Duration.ofSeconds(5), Duration.ofSeconds(5)), chatInputService, null);

        registry.startAll().join();

        // ... existing setup logic ...
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
     * If the service is registered but stopped or failed, it will attempt to start it.</p>
     *
     * @param name  The service name.
     * @param clazz The service class.
     * @param <T>   The service type.
     * @return A guaranteed running instance of the service.
     */
    public <T extends Service> T requireService(String name, Class<T> clazz) {
        // TODO move this into the registry perhaps (seperation of concerns and also better handling) and this handling of instantiation is very fragile and dumb
        T service = serviceRegistry.getAsByName(name, clazz).orElse(null);
        if (service == null) {
            try {
                service = clazz.getDeclaredConstructor().newInstance();
                UUID randomUUID = UUID.randomUUID();
                serviceRegistry.register(new ServiceDescriptor(randomUUID, name, null, Duration.ofSeconds(5), Duration.ofSeconds(5)), service, randomUUID);
                serviceRegistry.start(service.id()).join();
            } catch (Exception e) {
                throw new RuntimeException("Failed to auto-instantiate service: " + name, e);
            }
        } else {
            ServiceState state = serviceRegistry.getState(service.id());
            if (state == ServiceState.REGISTERED || state == ServiceState.STOPPED || state == ServiceState.FAILED) {
                serviceRegistry.start(service.id()).join();
            }
        }
        return service;
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
        System.out.println("PluginFileLogger is not enabled.");
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
