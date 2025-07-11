package de.happybavarian07.coolstufflib;

import de.happybavarian07.coolstufflib.backupmanager.BackupManager;
import de.happybavarian07.coolstufflib.backupmanager.FileBackup;
import de.happybavarian07.coolstufflib.backupmanager.RegexFileFilter;
import de.happybavarian07.coolstufflib.cache.Cache;
import de.happybavarian07.coolstufflib.cache.CacheManager;
import de.happybavarian07.coolstufflib.cache.FilePersistentCache;
import de.happybavarian07.coolstufflib.cache.InMemoryCache;
import de.happybavarian07.coolstufflib.commandmanagement.CommandManagerRegistry;
import de.happybavarian07.coolstufflib.jpa.utils.DatabaseProperties;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.languagemanager.PerPlayerLanguageHandler;
import de.happybavarian07.coolstufflib.menusystem.MenuAddonManager;
import de.happybavarian07.coolstufflib.repository.RepositoryManager;
import de.happybavarian07.coolstufflib.utils.PluginFileLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class CoolStuffLibBuilder {
    private JavaPlugin javaPluginUsingLib;
    private LanguageManager languageManager = null;
    private CommandManagerRegistry commandManagerRegistry = null;
    private MenuAddonManager menuAddonManager = null;
    private RepositoryManager repositoryManager = null;
    private CacheManager cacheManager = null;
    private BackupManager backupManager = null;
    private PluginFileLogger pluginFileLogger = null;
    private boolean usePlayerLangHandler = false;
    private boolean sendSyntaxOnZeroArgs = false;
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

        languageManager.addLanguagesToList(true);
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
    private Consumer<Object[]> repositoryManagerStartingMethod = (args) -> {
        if (args.length != 1) return;
        RepositoryManager repositoryManager = (RepositoryManager) args[0];
        repositoryManager.loadRepositoriesFromFile();
        repositoryManager.setRepositoryManagerEnabled(true);
    };
    private Consumer<Object[]> cacheManagerStartingMethod = (args) -> {
        if (args.length != 1) return;
        CacheManager cacheManager = (CacheManager) args[0];
        // No default startup action needed for CacheManager
    };
    private Consumer<Object[]> backupManagerStartingMethod = (args) -> {
        if (args.length != 1) return;
        BackupManager backupManager = (BackupManager) args[0];
    };
    private File dataFile = null;

    /**
     * Constructs a new CoolStuffLibBuilder.
     *
     * @param javaPluginUsingLib The JavaPlugin instance using this library.
     */
    public CoolStuffLibBuilder(JavaPlugin javaPluginUsingLib) {
        this.javaPluginUsingLib = javaPluginUsingLib;
        if (this.javaPluginUsingLib == null) {
            throw new RuntimeException("CoolStuffLib did not find a Plugin it got called from. Returning. Report the Issue to the Plugin Dev(s), that may have programmed the Plugin.");
        }
    }

    public LanguageManagerBuilder withLanguageManager() {
        return new LanguageManagerBuilder(this);
    }

    public CommandManagerBuilder withCommandManager() {
        return new CommandManagerBuilder(this);
    }

    public MenuSystemBuilder withMenuSystem() {
        return new MenuSystemBuilder(this);
    }

    public RepositoryManagerBuilder withRepositoryManager() {
        return new RepositoryManagerBuilder(this);
    }

    public CacheManagerBuilder withCacheManager() {
        return new CacheManagerBuilder(this);
    }

    public BackupManagerBuilder withBackupManager() {
        return new BackupManagerBuilder(this);
    }

    public LoggingBuilder withLogging() {
        return new LoggingBuilder(this);
    }

    @Deprecated
    public CoolStuffLibBuilder setJavaPluginUsingLib(JavaPlugin javaPluginUsingLib) {
        this.javaPluginUsingLib = javaPluginUsingLib;
        return this;
    }

    @Deprecated
    public CoolStuffLibBuilder setLanguageManager(LanguageManager languageManager) {
        this.languageManager = languageManager;
        return this;
    }

    @Deprecated
    public CoolStuffLibBuilder setCommandManagerRegistry(CommandManagerRegistry commandManagerRegistry) {
        this.commandManagerRegistry = commandManagerRegistry;
        return this;
    }

    @Deprecated
    public CoolStuffLibBuilder setMenuAddonManager(MenuAddonManager menuAddonManager) {
        this.menuAddonManager = menuAddonManager;
        return this;
    }

    @Deprecated
    public CoolStuffLibBuilder setRepositoryManager(RepositoryManager repositoryManager) {
        this.repositoryManager = repositoryManager;
        return this;
    }

    @Deprecated
    public CoolStuffLibBuilder setPluginFileLogger(PluginFileLogger pluginFileLogger) {
        this.pluginFileLogger = pluginFileLogger;
        return this;
    }

    @Deprecated
    public CoolStuffLibBuilder setUsePlayerLangHandler(boolean usePlayerLangHandler) {
        this.usePlayerLangHandler = usePlayerLangHandler;
        return this;
    }

    @Deprecated
    public CoolStuffLibBuilder setSendSyntaxOnZeroArgs(boolean sendSyntaxOnZeroArgs) {
        this.sendSyntaxOnZeroArgs = sendSyntaxOnZeroArgs;
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

    public CoolStuffLibBuilder setRepositoryManagerStartingMethod(Consumer<Object[]> repositoryManagerStartingMethod) {
        this.repositoryManagerStartingMethod = repositoryManagerStartingMethod;
        return this;
    }

    public CoolStuffLibBuilder setCacheManagerStartingMethod(Consumer<Object[]> cacheManagerStartingMethod) {
        this.cacheManagerStartingMethod = cacheManagerStartingMethod;
        return this;
    }

    public CoolStuffLibBuilder setBackupManagerStartingMethod(Consumer<Object[]> backupManagerStartingMethod) {
        this.backupManagerStartingMethod = backupManagerStartingMethod;
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
                repositoryManager,
                cacheManager,
                backupManager,
                pluginFileLogger,
                usePlayerLangHandler,
                sendSyntaxOnZeroArgs,
                languageManagerStartingMethod,
                commandManagerRegistryStartingMethod,
                menuAddonManagerStartingMethod,
                repositoryManagerStartingMethod,
                cacheManagerStartingMethod,
                backupManagerStartingMethod,
                dataFile);
    }

    public static class LanguageManagerBuilder {
        private final CoolStuffLibBuilder parent;
        private String prefix;
        private File languageFolder = null;
        private String resourceDirectory;
        private boolean usePlayerLangHandler = false;

        private LanguageManagerBuilder(CoolStuffLibBuilder parent) {
            this.parent = parent;
        }

        public LanguageManagerBuilder enablePlayerLanguageHandler() {
            this.usePlayerLangHandler = true;
            return this;
        }

        public LanguageManagerBuilder setResourceDirectory(String resourceDirectory) {
            this.resourceDirectory = resourceDirectory;
            return this;
        }

        public LanguageManagerBuilder setLanguageFolder(File languageFolder) {
            this.languageFolder = languageFolder;
            return this;
        }

        public LanguageManagerBuilder setPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public CoolStuffLibBuilder build() {
            parent.languageManager = new LanguageManager(parent.javaPluginUsingLib, languageFolder, resourceDirectory, prefix);
            parent.usePlayerLangHandler = this.usePlayerLangHandler;
            return parent;
        }
    }

    public static class CommandManagerBuilder {
        private final CoolStuffLibBuilder parent;
        private boolean sendSyntaxOnZeroArgs = false;

        private CommandManagerBuilder(CoolStuffLibBuilder parent) {
            this.parent = parent;
        }

        public CommandManagerBuilder enableSyntaxOnZeroArgs() {
            this.sendSyntaxOnZeroArgs = true;
            return this;
        }

        public CoolStuffLibBuilder build() {
            parent.commandManagerRegistry = new CommandManagerRegistry(parent.javaPluginUsingLib);
            parent.sendSyntaxOnZeroArgs = this.sendSyntaxOnZeroArgs;
            return parent;
        }
    }

    public static class MenuSystemBuilder {
        private final CoolStuffLibBuilder parent;

        private MenuSystemBuilder(CoolStuffLibBuilder parent) {
            this.parent = parent;
        }

        public CoolStuffLibBuilder build() {
            parent.menuAddonManager = new MenuAddonManager();
            return parent;
        }
    }

    public static class RepositoryManagerBuilder {
        private final CoolStuffLibBuilder parent;
        private final RepositoryManager repositoryManager;

        private RepositoryManagerBuilder(CoolStuffLibBuilder parent) {
            this.parent = parent;
            this.repositoryManager = new RepositoryManager(parent.javaPluginUsingLib);
        }

        public RepositoryManagerBuilder addMySQLConnection(String name, String host, String database, String username, String password) {
            DatabaseProperties props = new DatabaseProperties();
            props.setHost(host);
            props.setDatabase(database);
            props.setUsername(username);
            props.setPassword(password);
            props.setDriver("mysql");
            repositoryManager.addConnection(name, props);
            return this;
        }

        public RepositoryManagerBuilder addSQLiteConnection(String name, String filePath) {
            DatabaseProperties props = new DatabaseProperties();
            props.setDatabase(filePath);
            props.setDriver("sqlite");
            repositoryManager.addConnection(name, props);
            return this;
        }

        public RepositoryManagerBuilder setDefaultConnection(String name) {
            repositoryManager.setDefaultController(name);
            return this;
        }

        public RepositoryManagerBuilder setDatabasePrefix(String prefix) {
            return this;
        }

        public RepositoryManagerBuilder enableAutoTableCreation() {
            return this;
        }

        public CoolStuffLibBuilder build() {
            parent.repositoryManager = this.repositoryManager;
            return parent;
        }
    }

    public static class CacheManagerBuilder {
        private final CoolStuffLibBuilder parent;
        private final CacheManager cacheManager;

        private CacheManagerBuilder(CoolStuffLibBuilder parent) {
            this.parent = parent;
            this.cacheManager = new CacheManager();
        }

        public CacheBuilder newCache(String name) {
            return new CacheBuilder(this, name);
        }

        public CoolStuffLibBuilder build() {
            parent.cacheManager = this.cacheManager;
            return parent;
        }

        public static class CacheBuilder {
            private final CacheManagerBuilder parent;
            private final String name;
            private Cache<?, ?> cache;

            private CacheBuilder(CacheManagerBuilder parent, String name) {
                this.parent = parent;
                this.name = name;
            }

            public <K, V> CacheBuilder inMemory(int maxSize) {
                this.cache = new InMemoryCache<K, V>(maxSize);
                return this;
            }

            public <K, V> CacheBuilder persistent(File file) {
                this.cache = new FilePersistentCache<K, V>(file);
                return this;
            }

            public CacheManagerBuilder build() {
                parent.cacheManager.registerCache(name, cache);
                return parent;
            }
        }
    }

    public static class BackupManagerBuilder {
        private final CoolStuffLibBuilder parent;
        private final Map<String, FileBackup> backups = new HashMap<>();
        private int numberOfBackupsBeforeDeleting = 5;
        private long backupIntervalInSeconds = 3600;

        private BackupManagerBuilder(CoolStuffLibBuilder parent) {
            this.parent = parent;
        }

        public BackupManagerBuilder retention(int count) {
            this.numberOfBackupsBeforeDeleting = count;
            return this;
        }

        public BackupManagerBuilder interval(long seconds) {
            this.backupIntervalInSeconds = seconds;
            return this;
        }

        public FileBackupBuilder addBackup(String name) {
            return new FileBackupBuilder(this, name);
        }

        public CoolStuffLibBuilder build() {
            BackupManager manager = new BackupManager(numberOfBackupsBeforeDeleting, backupIntervalInSeconds);
            backups.forEach((it, val) -> manager.addFileBackup(val));
            parent.backupManager = manager;
            return parent;
        }

        public static class FileBackupBuilder {
            private final BackupManagerBuilder parent;
            private final String name;
            private final List<File> sources = new ArrayList<>();
            private final List<RegexFileFilter> includeFilters = new ArrayList<>();
            private final List<RegexFileFilter> excludeFilters = new ArrayList<>();
            private File destination;
            private File rootDirectory;
            private boolean useRegexFilters = false;

            private FileBackupBuilder(BackupManagerBuilder parent, String name) {
                this.parent = parent;
                this.name = name;
                this.rootDirectory = parent.parent.javaPluginUsingLib.getDataFolder();
            }

            public FileBackupBuilder source(File file) {
                sources.add(file);
                return this;
            }

            public FileBackupBuilder sources(File... files) {
                sources.addAll(Arrays.asList(files));
                return this;
            }

            public FileBackupBuilder useRegexFilters() {
                this.useRegexFilters = true;
                return this;
            }

            public FileBackupBuilder includeFilter(RegexFileFilter filter) {
                if (!sources.isEmpty()) {
                    throw new IllegalStateException("Cannot mix regex filters with file sources");
                }
                includeFilters.add(filter);
                return this;
            }

            public FileBackupBuilder excludeFilter(RegexFileFilter filter) {
                if (!sources.isEmpty()) {
                    throw new IllegalStateException("Cannot mix regex filters with file sources");
                }
                excludeFilters.add(filter);
                return this;
            }

            public FileBackupBuilder destination(File destination) {
                this.destination = destination;
                return this;
            }

            public FileBackupBuilder rootDirectory(File rootDirectory) {
                this.rootDirectory = rootDirectory;
                return this;
            }

            public BackupManagerBuilder build() {
                if (destination == null) {
                    throw new IllegalStateException("Destination must be specified");
                }

                FileBackup fileBackup;
                if (useRegexFilters) {
                    if (includeFilters.isEmpty()) {
                        throw new IllegalStateException("At least one include filter must be specified when using regex mode");
                    }
                    fileBackup = new FileBackup(name, includeFilters, excludeFilters, destination, rootDirectory);
                } else {
                    if (sources.isEmpty()) {
                        throw new IllegalStateException("At least one source file must be specified");
                    }
                    File[] sourceArray = sources.toArray(new File[0]);
                    fileBackup = new FileBackup(name, sourceArray, destination, rootDirectory);
                }

                parent.backups.put(name, fileBackup);
                return parent;
            }
        }
    }

    public static class LoggingBuilder {
        private final CoolStuffLibBuilder parent;
        private PluginFileLogger pluginFileLogger;

        private LoggingBuilder(CoolStuffLibBuilder parent) {
            this.parent = parent;
        }

        public LoggingBuilder create(PluginFileLogger pluginFileLogger) {
            this.pluginFileLogger = pluginFileLogger;
            return this;
        }

        public LoggingBuilder enableFileLogging() {
            return this;
        }

        public CoolStuffLibBuilder build() {
            parent.pluginFileLogger = this.pluginFileLogger;
            return parent;
        }
    }
}
