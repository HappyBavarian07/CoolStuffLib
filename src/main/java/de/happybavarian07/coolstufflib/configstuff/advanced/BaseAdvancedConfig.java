package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.core.ConfigLockManager;
import de.happybavarian07.coolstufflib.configstuff.advanced.core.SectionManager;
import de.happybavarian07.coolstufflib.configstuff.advanced.core.ValueAccessor;
import de.happybavarian07.coolstufflib.configstuff.advanced.core.ConfigMetadataManager;
import de.happybavarian07.coolstufflib.configstuff.advanced.core.ConfigCommentManager;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEventBus;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigFileType;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.ConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.BaseConfigModule;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.ModuleManager;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.BaseConfigSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.migration.MigrationContext;
import java.io.File;
import java.util.*;
import java.util.function.Supplier;

/**
 * <p>Abstract base implementation for advanced configuration systems that provides
 * comprehensive configuration management capabilities including thread-safe operations,
 * modular architecture, event handling, and advanced data access patterns.</p>
 *
 * <p>This class serves as the foundation for both persistent and in-memory configuration
 * implementations, offering features such as:</p>
 * <ul>
 * <li>Thread-safe read/write operations with granular locking</li>
 * <li>Modular configuration system with plugin architecture</li>
 * <li>Event-driven configuration changes</li>
 * <li>Section-based hierarchical data organization</li>
 * <li>Type-safe value access with optional defaults</li>
 * <li>Configuration migration support</li>
 * </ul>
 *
 * <pre><code>
 * // Example usage with custom file handler
 * BaseAdvancedConfig config = new MyAdvancedConfig("app-config",
 *     new File("config.yml"), ConfigFileType.YAML);
 * config.set("database.host", "localhost");
 * String host = config.getString("database.host", "default-host");
 * </code></pre>
 */
public abstract class BaseAdvancedConfig implements AdvancedConfig {
    private final String name;
    private final File file;
    private final ConfigFileHandler configFileHandler;
    private final ConfigLockManager lockManager;
    private final ConfigEventBus eventBus;
    private final ModuleManager moduleManager;
    private final SectionManager sectionManager;
    private final ValueAccessor valueAccessor;
    private final ConfigMetadataManager metadataManager;
    private final ConfigCommentManager commentManager;
    private MigrationContext migrationContext;

    protected BaseAdvancedConfig(String name, File file, ConfigFileHandler configFileHandler) {
        this.name = name;
        this.file = file;
        this.configFileHandler = configFileHandler;
        this.lockManager = new ConfigLockManager();
        this.eventBus = new ConfigEventBus();
        this.sectionManager = new SectionManager(new BaseConfigSection(""), eventBus);
        this.valueAccessor = new ValueAccessor(sectionManager, eventBus);
        this.moduleManager = new ModuleManager(this, eventBus, lockManager);
        this.metadataManager = new ConfigMetadataManager(lockManager, eventBus);
        this.commentManager = new ConfigCommentManager(lockManager);
        this.migrationContext = null;
    }

    protected BaseAdvancedConfig(String name, File file, ConfigFileType configFileType) {
        this(name, file, configFileType.createHandler());
    }

    protected BaseAdvancedConfig(String name, File file, ConfigFileHandler configFileHandler, MigrationContext migrationContext) {
        this.name = name;
        this.file = file;
        this.configFileHandler = configFileHandler;
        this.lockManager = new ConfigLockManager();
        this.eventBus = new ConfigEventBus();
        this.sectionManager = new SectionManager(new BaseConfigSection(""), eventBus);
        this.valueAccessor = new ValueAccessor(sectionManager, eventBus);
        this.moduleManager = new ModuleManager(this, eventBus, lockManager);
        this.metadataManager = new ConfigMetadataManager(lockManager, eventBus);
        this.commentManager = new ConfigCommentManager(lockManager);
        this.migrationContext = migrationContext;
        if (migrationContext != null) {
            syncMetadataFromMigrationContext();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public ConfigFileHandler getConfigFileHandler() {
        return configFileHandler;
    }

    @Override
    public ConfigEventBus getEventBus() {
        return eventBus;
    }

    /**
     * <p>Acquires a read lock for module operations, preventing concurrent module modifications
     * while allowing multiple readers. This ensures thread-safe access to the module registry
     * during read operations.</p>
     *
     * <pre><code>
     * config.lockModuleRead();
     * try {
     *     BaseConfigModule module = config.getModuleByName("validation");
     *     // Perform read operations safely
     * } finally {
     *     config.unlockModuleRead();
     * }
     * </code></pre>
     */
    @Override
    public void lockModuleRead() {
        lockManager.lockModuleRead();
    }

    /**
     * <p>Releases a previously acquired module read lock, allowing pending write operations
     * to proceed. Must be called after every {@link #lockModuleRead()} call to prevent
     * deadlocks.</p>
     *
     * <pre><code>
     * config.lockModuleRead();
     * try {
     *     // Read operations
     * } finally {
     *     config.unlockModuleRead(); // Always release the lock
     * }
     * </code></pre>
     */
    @Override
    public void unlockModuleRead() {
        lockManager.unlockModuleRead();
    }

    /**
     * <p>Acquires an exclusive write lock for module operations, blocking all other module
     * access until released. Use this when registering, unregistering, or modifying modules
     * to ensure thread safety.</p>
     *
     * <pre><code>
     * config.lockModuleWrite();
     * try {
     *     config.registerModule(new ValidationModule());
     *     config.enableModule("validation");
     * } finally {
     *     config.unlockModuleWrite();
     * }
     * </code></pre>
     */
    @Override
    public void lockModuleWrite() {
        lockManager.lockModuleWrite();
    }

    /**
     * <p>Releases a previously acquired module write lock, allowing other threads to access
     * the module system. Must be called after every {@link #lockModuleWrite()} call.</p>
     *
     * <pre><code>
     * config.lockModuleWrite();
     * try {
     *     // Module modification operations
     * } finally {
     *     config.unlockModuleWrite(); // Critical to prevent deadlocks
     * }
     * </code></pre>
     */
    @Override
    public void unlockModuleWrite() {
        lockManager.unlockModuleWrite();
    }

    /**
     * <p>Acquires a read lock for configuration values, allowing concurrent read access
     * while preventing write operations. Use this to ensure consistent data when reading
     * multiple related values.</p>
     *
     * <pre><code>
     * config.lockValuesRead();
     * try {
     *     String host = config.getString("database.host");
     *     int port = config.getInt("database.port");
     *     // Both values are consistent
     * } finally {
     *     config.unlockValuesRead();
     * }
     * </code></pre>
     */
    @Override
    public void lockValuesRead() {
        lockManager.lockValuesRead();
    }

    /**
     * <p>Releases a previously acquired values read lock, allowing write operations to proceed.
     * Must be called after every {@link #lockValuesRead()} call.</p>
     *
     * <pre><code>
     * config.lockValuesRead();
     * try {
     *     // Read operations
     * } finally {
     *     config.unlockValuesRead(); // Always release
     * }
     * </code></pre>
     */
    @Override
    public void unlockValuesRead() {
        lockManager.unlockValuesRead();
    }

    /**
     * <p>Acquires an exclusive write lock for configuration values, blocking all other value
     * access until released. Use this when performing atomic updates to multiple related
     * configuration values.</p>
     *
     * <pre><code>
     * config.lockValuesWrite();
     * try {
     *     config.set("database.host", "newhost");
     *     config.set("database.port", 5432);
     *     // Atomic update of related values
     * } finally {
     *     config.unlockValuesWrite();
     * }
     * </code></pre>
     */
    @Override
    public void lockValuesWrite() {
        lockManager.lockValuesWrite();
    }

    /**
     * <p>Releases a previously acquired values write lock, allowing other threads to access
     * configuration values. Must be called after every {@link #lockValuesWrite()} call.</p>
     *
     * <pre><code>
     * config.lockValuesWrite();
     * try {
     *     // Write operations
     * } finally {
     *     config.unlockValuesWrite(); // Critical for thread safety
     * }
     * </code></pre>
     */
    @Override
    public void unlockValuesWrite() {
        lockManager.unlockValuesWrite();
    }

    /**
     * <p>Executes an operation while holding a module read lock, automatically acquiring
     * and releasing the lock. This provides a safe way to perform read operations on
     * modules without manual lock management.</p>
     *
     * <pre><code>
     * BaseConfigModule module = config.withModuleReadLock(() -> {
     *     return config.getModuleByName("validation");
     * });
     * </code></pre>
     *
     * @param operation the operation to execute under lock
     * @param <T> the return type of the operation
     * @return the result of the operation
     */
    @Override
    public <T> T withModuleReadLock(Supplier<T> operation) {
        return lockManager.withModuleReadLock(operation);
    }

    /**
     * <p>Executes an operation while holding a values read lock, automatically acquiring
     * and releasing the lock. This ensures consistent access to configuration values
     * during the operation.</p>
     *
     * <pre><code>
     * Map&lt;String, Object&gt; dbConfig = config.withValuesReadLock(() -> {
     *     Map&lt;String, Object&gt; result = new HashMap&lt;&gt;();
     *     result.put("host", config.getString("database.host"));
     *     result.put("port", config.getInt("database.port"));
     *     return result;
     * });
     * </code></pre>
     *
     * @param operation the operation to execute under lock
     * @param <T> the return type of the operation
     * @return the result of the operation
     */
    @Override
    public <T> T withValuesReadLock(Supplier<T> operation) {
        return lockManager.withValuesReadLock(operation);
    }

    /**
     * <p>Executes an operation while holding a module write lock, automatically acquiring
     * and releasing the lock. Use this for atomic module registration or modification
     * operations.</p>
     *
     * <pre><code>
     * Boolean success = config.withModulesWriteLock(() -> {
     *     config.registerModule(new ValidationModule());
     *     config.enableModule("validation");
     *     return true;
     * });
     * </code></pre>
     *
     * @param operation the operation to execute under lock
     * @param <T> the return type of the operation
     * @return the result of the operation
     */
    @Override
    public <T> T withModulesWriteLock(Supplier<T> operation) {
        return lockManager.withModulesWriteLock(operation);
    }

    /**
     * <p>Executes an operation while holding a values write lock, automatically acquiring
     * and releasing the lock. Use this for atomic updates to multiple configuration
     * values.</p>
     *
     * <pre><code>
     * config.withValuesWriteLock(() -> {
     *     config.set("database.host", "newhost");
     *     config.set("database.port", 5432);
     *     config.set("database.name", "newdb");
     *     return null;
     * });
     * </code></pre>
     *
     * @param operation the operation to execute under lock
     * @param <T> the return type of the operation
     * @return the result of the operation
     */
    @Override
    public <T> T withValuesWriteLock(Supplier<T> operation) {
        return lockManager.withValuesWriteLock(operation);
    }

    @Override
    public ConfigSection getRootSection() {
        return sectionManager.getRootSection();
    }

    /**
     * <p>Retrieves a configuration section at the specified path. Returns null if the
     * section does not exist. Sections provide hierarchical organization of configuration
     * data and can contain both values and sub-sections.</p>
     *
     * <pre><code>
     * ConfigSection dbSection = config.getSection("database");
     * if (dbSection != null) {
     *     String host = dbSection.getString("host");
     * }
     * </code></pre>
     *
     * @param path the dot-separated path to the section
     * @return the configuration section at the path, or null if not found
     */
    @Override
    public ConfigSection getSection(String path) {
        return sectionManager.getSection(path);
    }

    /**
     * <p>Creates a new configuration section at the specified path. If intermediate
     * sections do not exist, they will be created automatically. If a section already
     * exists at the path, it will be returned.</p>
     *
     * <pre><code>
     * ConfigSection dbSection = config.createSection("database");
     * dbSection.set("host", "localhost");
     * dbSection.set("port", 3306);
     * </code></pre>
     *
     * @param path the dot-separated path where the section should be created
     * @return the newly created or existing configuration section
     */
    @Override
    public ConfigSection createSection(String path) {
        return sectionManager.createSection(path, this);
    }

    /**
     * <p>Checks whether a configuration section exists at the specified path.
     * This is useful for conditional logic based on section presence.</p>
     *
     * <pre><code>
     * if (config.hasSection("database")) {
     *     // Configure database settings
     * }
     * </code></pre>
     *
     * @param path the dot-separated path to check
     * @return true if a section exists at the path, false otherwise
     */
    @Override
    public boolean hasSection(String path) {
        return sectionManager.hasSection(path);
    }

    /**
     * <p>Removes a configuration section and all its contents at the specified path.
     * This operation cascades to remove all sub-sections and values within the
     * removed section.</p>
     *
     * <pre><code>
     * config.removeSection("database.cache");
     * // All cache-related configuration is now removed
     * </code></pre>
     *
     * @param path the dot-separated path of the section to remove
     */
    @Override
    public void removeSection(String path) {
        sectionManager.removeSection(path, this);
    }

    @Override
    public Object get(String path) {
        return valueAccessor.get(path);
    }

    @Override
    public Object get(String path, Object defaultValue) {
        return valueAccessor.get(path, defaultValue);
    }

    @Override
    public String getString(String path) {
        return valueAccessor.getString(path);
    }

    @Override
    public String getString(String path, String defaultValue) {
        return valueAccessor.getString(path, defaultValue);
    }

    @Override
    public boolean getBoolean(String path) {
        return valueAccessor.getBoolean(path);
    }

    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
        return valueAccessor.getBoolean(path, defaultValue);
    }

    @Override
    public int getInt(String path) {
        return valueAccessor.getInt(path);
    }

    @Override
    public int getInt(String path, int defaultValue) {
        return valueAccessor.getInt(path, defaultValue);
    }

    @Override
    public long getLong(String path) {
        return valueAccessor.getLong(path);
    }

    @Override
    public long getLong(String path, long defaultValue) {
        return valueAccessor.getLong(path, defaultValue);
    }

    @Override
    public double getDouble(String path) {
        return valueAccessor.getDouble(path);
    }

    @Override
    public double getDouble(String path, double defaultValue) {
        return valueAccessor.getDouble(path, defaultValue);
    }

    @Override
    public float getFloat(String path) {
        return valueAccessor.getFloat(path);
    }

    @Override
    public float getFloat(String path, float defaultValue) {
        return valueAccessor.getFloat(path, defaultValue);
    }

    @Override
    public List<?> getList(String path) {
        return valueAccessor.getList(path);
    }

    @Override
    public List<?> getList(String path, List<?> defaultValue) {
        return valueAccessor.getList(path, defaultValue);
    }

    @Override
    public List<String> getStringList(String path) {
        return valueAccessor.getStringList(path);
    }

    @Override
    public List<String> getStringList(String path, List<String> defaultValue) {
        return valueAccessor.getStringList(path, defaultValue);
    }

    @Override
    public <T> T get(String path, T defaultValue, Class<T> type) {
        return valueAccessor.get(path, defaultValue, type);
    }

    @Override
    public <T> T getValue(String path, Class<T> type) {
        return valueAccessor.getValue(path, type);
    }

    @Override
    public <T> Optional<T> getOptionalValue(String path, Class<T> type) {
        return valueAccessor.getOptionalValue(path, type);
    }

    @Override
    public void set(String path, Object value) {
        valueAccessor.set(path, value, this);
    }

    @Override
    public void setBulk(Map<String, Object> values) {
        valueAccessor.setBulk(values, this);
    }

    @Override
    public void remove(String path) {
        valueAccessor.remove(path, this);
    }

    @Override
    public boolean containsKey(String path) {
        return valueAccessor.containsKey(path);
    }

    @Override
    public void clear() {
        valueAccessor.clear(this);
    }

    @Override
    public void registerModule(BaseConfigModule module) {
        moduleManager.registerModule(module);
    }

    @Override
    public void unregisterModule(String name) {
        moduleManager.unregisterModule(name);
    }

    @Override
    public boolean hasModule(BaseConfigModule module) {
        return moduleManager.hasModule(module);
    }

    @Override
    public boolean hasModule(String moduleName) {
        return moduleManager.hasModule(moduleName);
    }

    @Override
    public BaseConfigModule getModuleByName(String name) {
        return moduleManager.getModuleByName(name);
    }

    @Override
    public void enableModule(String moduleName) {
        moduleManager.enableModule(moduleName);
    }

    @Override
    public void disableModule(String moduleName) {
        moduleManager.disableModule(moduleName);
    }

    @Override
    public Map<String, BaseConfigModule> getModules() {
        return moduleManager.getModules();
    }

    @Override
    public List<String> getKeys(boolean deep) {
        return sectionManager.getKeys(deep);
    }

    @Override
    public <T extends ConfigSection> T createCustomSection(String path, Class<T> sectionType) {
        return sectionManager.createCustomSection(path, sectionType, this);
    }

    @Override
    public void copyFrom(AdvancedConfig config2) {
        valueAccessor.copyFrom(config2, this, moduleManager);
    }

    @Override
    public boolean hasMetadata(String version) {
        return metadataManager.hasMetadata(version);
    }

    @Override
    public void removeMetadata(String version) {
        metadataManager.removeMetadata(version);
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadataManager.getMetadata();
    }

    @Override
    public <T> T getMetadata(String name) {
        return metadataManager.getMetadata(name);
    }

    @Override
    public <T> void addMetadata(String name, T value) {
        metadataManager.addMetadata(name, value, this);
    }

    @Override
    public void setComment(String path, String comment) {
        commentManager.setComment(path, comment);
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public SectionManager getSectionManager() {
        return sectionManager;
    }

    public ConfigCommentManager getCommentManager() {
        return commentManager;
    }

    @Override
    public String getComment(String path) {
        return commentManager.getComment(path);
    }

    @Override
    public void removeComment(String path) {
        commentManager.removeComment(path);
    }

    @Override
    public Map<String, String> getAllComments() {
        return commentManager.getAllComments();
    }

    @Override
    public boolean hasComment(String path) {
        return commentManager.hasComment(path);
    }

    public MigrationContext getMigrationContext() {
        return migrationContext;
    }

    public void setMigrationContext(MigrationContext migrationContext) {
        this.migrationContext = migrationContext;
        if (migrationContext != null) {
            syncMetadataFromMigrationContext();
        }
    }

    private void syncMetadataFromMigrationContext() {
        if (migrationContext == null) return;
        Map<String, Object> migrationMeta = migrationContext.getMetadata();
        for (Map.Entry<String, Object> entry : migrationMeta.entrySet()) {
            if (!metadataManager.hasMetadata(entry.getKey())) {
                metadataManager.addMetadata(entry.getKey(), entry.getValue(), this);
            }
        }
        Map<String, Object> configMeta = metadataManager.getMetadata();
        for (Map.Entry<String, Object> entry : configMeta.entrySet()) {
            if (!migrationMeta.containsKey(entry.getKey())) {
                migrationContext.setMetadata(entry.getKey(), entry.getValue());
            }
        }
    }

    protected void detectAndConvertCollectionsRecursive(ConfigSection section) {
        Set<String> allKeys = new HashSet<>(section.getKeys(true));
        Map<String, Map<Integer, String>> listCandidates = new HashMap<>();
        for (String key : allKeys) {
            int lastDot = key.lastIndexOf('.');
            if (lastDot > 0) {
                String prefix = key.substring(0, lastDot);
                String idxStr = key.substring(lastDot + 1);
                try {
                    int idx = Integer.parseInt(idxStr);
                    listCandidates.computeIfAbsent(prefix, k -> new HashMap<>()).put(idx, key);
                } catch (NumberFormatException ignored) {
                }
            } else {
                // If the key does not contain a dot, it cannot be part of a list
                continue;
            }
        }
        for (Map.Entry<String, Map<Integer, String>> entry : listCandidates.entrySet()) {
            String prefix = entry.getKey();
            Map<Integer, String> idxToKey = entry.getValue();
            if (idxToKey.isEmpty()) continue;
            int minIdx = idxToKey.keySet().stream().min(Integer::compareTo).orElse(0);
            int maxIdx = idxToKey.keySet().stream().max(Integer::compareTo).orElse(-1);
            boolean contiguous = true;
            for (int i = minIdx; i <= maxIdx; i++) {
                if (!idxToKey.containsKey(i)) {
                    contiguous = false;
                    break;
                }
            }
            if (!contiguous) continue;
            List<Object> list = new ArrayList<>();
            for (int i = minIdx; i <= maxIdx; i++) {
                String k = idxToKey.get(i);
                list.add(k != null ? section.get(k) : null);
            }
            for (String k : idxToKey.values()) {
                section.remove(k);
            }
            section.set(prefix, list);
        }
        for (ConfigSection sub : section.getSubSections().values()) {
            detectAndConvertCollectionsRecursive(sub);
        }
    }

    public void detectAndConvertCollections() {
        detectAndConvertCollectionsRecursive(getRootSection());
    }

    @Override
    public abstract void save();

    @Override
    public abstract void reload();

    @Override
    public boolean isLoaded() {
        if (file == null || !file.exists() || !file.canRead()) return false;
        if (configFileHandler == null) return false;
        try {
            java.lang.reflect.Method wasCalledMethod = configFileHandler.getClass().getMethod("wasCalled");
            Object result = wasCalledMethod.invoke(configFileHandler);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    @Override
    public void migrate(MigrationContext context, ConfigSection rootSection, boolean replace) {
        setMigrationContext(context);
        if (replace) {
            getRootSection().clear();
            getRootSection().copyFrom(rootSection);
        } else {
            getRootSection().merge(rootSection);
        }
    }

    @Override
    public void migrate(MigrationContext context, Map<String, Object> values, boolean replace) {
        setMigrationContext(context);
        if (replace) {
            getRootSection().clear();
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                getRootSection().set(entry.getKey(), entry.getValue());
            }
        } else {
            BaseConfigSection temp = new BaseConfigSection("temp");
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                temp.set(entry.getKey(), entry.getValue());
            }
            getRootSection().merge(temp);
        }
    }
}
