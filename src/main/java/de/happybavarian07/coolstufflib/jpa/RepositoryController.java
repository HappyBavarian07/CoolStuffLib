package de.happybavarian07.coolstufflib.jpa;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.cache.CacheManager;
import de.happybavarian07.coolstufflib.jpa.annotations.*;
import de.happybavarian07.coolstufflib.jpa.connection.ConnectionPool;
import de.happybavarian07.coolstufflib.jpa.exceptions.MySQLSystemExceptions;
import de.happybavarian07.coolstufflib.jpa.repository.Repository;
import de.happybavarian07.coolstufflib.jpa.utils.DatabaseProperties;
import de.happybavarian07.coolstufflib.jpa.utils.MySQLUtils;
import de.happybavarian07.coolstufflib.jpa.utils.RepositoryProxy;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class RepositoryController {
    private final JavaPlugin plugin;
    private final SQLExecutor sqlExecutor;
    private final Map<Class<?>, Repository<?, ?>> repositories = new HashMap<>();
    private final Map<String, Class<?>> entityClasses = new HashMap<>();
    private final DatabaseProperties dbProperties;
    private final File defaultRegistrationFile;
    private final Map<String, ConnectionPool> connectionPools = new HashMap<>();
    private CacheManager cacheManager;

    public RepositoryController(JavaPlugin plugin, File defaultRegistrationFile, DatabaseProperties dbProperties) {
        this.plugin = plugin;
        this.defaultRegistrationFile = defaultRegistrationFile;
        this.dbProperties = dbProperties;
        this.sqlExecutor = new SQLExecutor(this, dbProperties);

        setupDefaultConnection();
    }

    private void setupDefaultConnection() {
        try {
            String url = dbProperties.getConnectionString();
            String username = dbProperties.getUsername();
            String password = dbProperties.getPassword();

            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("Database connection URL is empty or null.");
            }

            ConnectionPool pool = new ConnectionPool(url, username, password, 5, 10);
            connectionPools.put("default", pool);
            sqlExecutor.setDefaultConnection("default");
            logInfo("Default connection pool established with driver: " + dbProperties.getDriver());
        } catch (Exception e) {
            logSevere("Failed to establish default database connection pool: " + e.getMessage(), e);
        }
    }

    public void addConnection(String name, DatabaseProperties connectionProperties) {
        try {
            String url = connectionProperties.getConnectionString();
            String username = connectionProperties.getUsername();
            String password = connectionProperties.getPassword();

            ConnectionPool pool = new ConnectionPool(url, username, password, 5, 10);
            connectionPools.put(name, pool);
            logInfo("Added connection pool '" + name + "' with driver: " + connectionProperties.getDriver());
        } catch (Exception e) {
            logSevere("Failed to add connection pool '" + name + "': " + e.getMessage(), e);
        }
    }

    /**
     * <p>Sets the connection to be used.</p>
     * 
     * <pre><code>controller.setDefaultConnection("default");</code></pre>
     *
     * @param name Name of the connection
     */
    public void setDefaultConnection(String name) {
        try {
            sqlExecutor.setDefaultConnection(name);
            logInfo("Set default connection to: " + name);
        } catch (IllegalArgumentException e) {
            logWarning("Failed to set default connection: " + e.getMessage());
        }
    }

    public void setDatabasePrefix(String prefix) {
        dbProperties.setDatabasePrefix(prefix);
        sqlExecutor.setDatabasePrefix(prefix);

        for (Repository<?, ?> repository : repositories.values()) {
            if (repository instanceof RepositoryProxy) {
                ((RepositoryProxy) repository).setDatabasePrefix(prefix);
            }
        }
    }

    /**
     * <p>Registers a repository interface and returns its implementation.</p>
     *
     * <pre><code>MyRepo repo = controller.registerRepository(MyRepo.class, MyEntity.class);</code></pre>
     *
     * @param repositoryInterface The repository interface class
     * @param entityClass         The entity class
     * @param <T>                 Repository type
     * @param <E>                 Entity type
     * @param <ID>                ID type
     * @return The repository implementation
     */
    @SuppressWarnings("unchecked")
    public <T extends Repository<E, ID>, E, ID> T registerRepository(Class<T> repositoryInterface, Class<E> entityClass) {
        if (repositories.containsKey(repositoryInterface) && repositories.get(repositoryInterface) != null &&
                repositories.get(repositoryInterface).getClass().isAssignableFrom(repositoryInterface)) {
            logWarning("Repository " + repositoryInterface.getName() + " is already registered");
            return (T) repositories.get(repositoryInterface);
        }
        if (!Repository.class.isAssignableFrom(repositoryInterface)) {
            logWarning("Class " + repositoryInterface.getName() + " does not implement Repository interface");
            throw new IllegalArgumentException("Class must implement Repository interface");
        }
        if (!entityClass.isAnnotationPresent(Entity.class) || !entityClass.isAnnotationPresent(Table.class)) {
            logWarning("Entity class " + entityClass.getName() + " must be annotated with @Entity and @Table");
            throw new IllegalArgumentException("Entity class must be annotated with @Entity and @Table");
        }

        T repository = RepositoryProxy.create(repositoryInterface, dbProperties.getDatabasePrefix(), sqlExecutor, plugin, cacheManager());
        repositories.put(repositoryInterface, repository);
        entityClasses.put(entityClass.getName(), entityClass);

        logInfo("Registered repository for entity: " + entityClass.getSimpleName());

        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                logInfo("Primary key field discovered: " + field.getName());
                if (field.isAnnotationPresent(GeneratedValue.class)) {
                    GeneratedValue generatedValue = field.getAnnotation(GeneratedValue.class);
                    GenerationType strategy = generatedValue.strategy();
                    logInfo("Generation strategy for " + field.getName() + ": " + strategy);
                }
            }
        }

        try {
            sqlExecutor.generateSchema(entityClass);
            logInfo("Generated schema for entity: " + entityClass.getSimpleName());
        } catch (SQLException e) {
            logSevere("Failed to generate schema for entity " + entityClass.getSimpleName() + ": " + e.getMessage(), e);
        }

        return repository;
    }

    /**
     * <p>Returns a registered repository.</p>
     *
     * @param repositoryInterface The repository interface class
     * @param <T>                 Repository type
     * @return Repository implementation or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T extends Repository<?, ?>> T getRepository(Class<T> repositoryInterface) {
        return (T) repositories.get(repositoryInterface);
    }

    /**
     * <p>Checks if a repository is registered.</p>
     *
     * @param repositoryInterface The repository interface class
     * @return true if registered, false otherwise
     */
    public boolean isRepositoryRegistered(Class<?> repositoryInterface) {
        return repositories.containsKey(repositoryInterface);
    }

    /**
     * <p>Returns all registered repository interfaces.</p>
     *
     * @return Set of registered repository interfaces
     */
    public Set<Class<?>> getAllRepositoryInterfaces() {
        return new HashSet<>(repositories.keySet());
    }

    /**
     * <p>Returns all registered repository instances.</p>
     *
     * @return Collection of registered repositories
     */
    public Collection<Repository<?, ?>> getAllRepositories() {
        return new HashSet<>(repositories.values());
    }

    /**
     * <p>Removes a repository from the registry.</p>
     *
     * <pre><code>boolean removed = controller.unregisterRepository(MyRepo.class);</code></pre>
     *
     * @param repositoryInterface The repository interface to remove
     * @return {@code true} if removed, {@code false} if not registered
     */
    public boolean unregisterRepository(Class<?> repositoryInterface) {
        if (repositories.containsKey(repositoryInterface)) {
            repositories.remove(repositoryInterface);
            logInfo("Unregistered repository: " + repositoryInterface.getName());
            return true;
        }
        return false;
    }

    /**
     * <p>Sets the cache manager the entity caches of this controller's entities are registered with, so
     * {@code clearAll}, {@code removeCache} and {@code shutdown} reach them.</p>
     *
     * <pre><code>controller.setCacheManager(coolStuffLib.getCacheManager());</code></pre>
     *
     * @param cacheManager The cache manager, or null to use the library singleton
     */
    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * <p>The library singleton only exists once the plugin built it, which is after this controller was created,
     * so the lookup happens per registration and not in the constructor.</p>
     */
    private CacheManager cacheManager() {
        if (cacheManager == null) {
            try {
                cacheManager = CoolStuffLib.getLib().getCacheManager();
            } catch (RuntimeException libraryNotInitialized) {
                return null;
            }
        }
        return cacheManager;
    }

    /**
     * <p>Executes a SQL query directly via the SQLExecutor. The connection is only returned to the pool
     * when the ResultSet is closed; prefer {@link #query(String, SQLExecutor.ResultSetMapper, Object...)}.</p>
     *
     * @param sql    SQL query
     * @param params Query parameters
     * @return ResultSet of the query
     * @throws SQLException If an error occurs
     */
    public java.sql.ResultSet executeQuery(String sql, Object... params) throws SQLException {
        return sqlExecutor.executeQuery(sql, params);
    }

    /**
     * <p>Executes a SQL query and maps the result; all resources are released afterwards.</p>
     *
     * <pre><code>List&lt;String&gt; names = controller.query("SELECT name FROM t", rs -&gt; { ... });</code></pre>
     */
    public <T> T query(String sql, SQLExecutor.ResultSetMapper<T> mapper, Object... params) throws SQLException {
        return sqlExecutor.query(sql, mapper, params);
    }

    /**
     * <p>The executor behind this controller, e.g. for stores that manage their own table.</p>
     */
    public SQLExecutor getSqlExecutor() {
        return sqlExecutor;
    }

    /**
     * <p>Executes a SQL update directly via the SQLExecutor.</p>
     *
     * @param sql    SQL update statement
     * @param params Update parameters
     * @throws SQLException If an error occurs
     */
    public void executeUpdate(String sql, Object... params) throws SQLException {
        sqlExecutor.executeUpdate(sql, params);
    }

    /**
     * <p>Executes multiple SQL statements as a transaction.</p>
     *
     * @param statements List of SQL statements
     * @throws SQLException If an error occurs
     */
    public void executeTransaction(List<String> statements) throws SQLException {
        sqlExecutor.executeTransaction(statements);
    }

    /**
     * <p>Loads repository registrations from a file.</p>
     */
    @SuppressWarnings("unchecked")
    public void loadRepositoriesFromFile() {
        if (!defaultRegistrationFile.exists()) {
            try {
                defaultRegistrationFile.createNewFile();
                JsonObject json = new JsonObject();
                json.add("repositories", new JsonArray());
                Files.write(defaultRegistrationFile.toPath(), json.toString().getBytes());
            } catch (Exception e) {
                logSevere("Failed to create repository registration file: " + e.getMessage(), e);
                return;
            }
        }

        try {
            String content = new String(Files.readAllBytes(defaultRegistrationFile.toPath()));
            if (content.isEmpty()) {
                content = "{}";
            }

            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            JsonArray repoArray = json.has("repositories") ? json.getAsJsonArray("repositories") : null;

            if (repoArray == null) {
                repoArray = new JsonArray();
                json.add("repositories", repoArray);
                Files.write(defaultRegistrationFile.toPath(), json.toString().getBytes());
                return;
            }

            for (int i = 0; i < repoArray.size(); i++) {
                JsonObject repoObj = repoArray.get(i).getAsJsonObject();
                String repositoryClassName = repoObj.get("repositoryClass").getAsString();
                String entityClassName = repoObj.get("entityClass").getAsString();
                boolean enabled = repoObj.get("enabled").getAsBoolean();

                if (!enabled) {
                    logInfo("Repository " + repositoryClassName + " is disabled, skipping.");
                    continue;
                }

                try {
                    Class<?> repositoryClass = Class.forName(repositoryClassName);
                    Class<?> entityClass = Class.forName(entityClassName);

                    if (Repository.class.isAssignableFrom(repositoryClass)) {
                        Object repository = registerRepository((Class) repositoryClass, entityClass);
                        logInfo("Repository successfully registered: " + repositoryClassName);
                    } else {
                        logWarning("Class " + repositoryClassName + " does not implement Repository interface");
                    }

                } catch (ClassNotFoundException e) {
                    logWarning("Class not found: " + e.getMessage());
                } catch (Exception e) {
                    logSevere("Failed to register repository: " + e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            logSevere("Failed to load repositories from file: " + e.getMessage(), e);
        }
    }

    /**
     * <p>Adds a repository to the registration file.</p>
     *
     * <pre><code>controller.addRepositoryToRegistrationFile(MyRepo.class, MyEntity.class, "desc", true);</code></pre>
     *
     * @param <T>             The repository type
     * @param <E>             The entity type
     * @param repositoryClass The repository class
     * @param entityClass     The entity class
     * @param description     Description of the repository
     * @param register        Whether to register immediately
     */
    public <T extends Repository<E, ?>, E> void addRepositoryToRegistrationFile(
            Class<T> repositoryClass,
            Class<E> entityClass,
            String description,
            boolean register) {

        if (!defaultRegistrationFile.exists()) {
            try {
                defaultRegistrationFile.createNewFile();
            } catch (Exception e) {
                logSevere("Failed to create repository registration file: " + e.getMessage(), e);
                return;
            }
        }

        try {
            String content = new String(Files.readAllBytes(defaultRegistrationFile.toPath()));
            if (content.isEmpty()) {
                content = "{}";
            }

            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            JsonArray repoArray = json.has("repositories") ? json.getAsJsonArray("repositories") : null;

            if (repoArray == null) {
                repoArray = new JsonArray();
                json.add("repositories", repoArray);
            }

            for (int i = 0; i < repoArray.size(); i++) {
                JsonObject repoObj = repoArray.get(i).getAsJsonObject();
                if (repoObj.get("repositoryClass").getAsString().equals(repositoryClass.getName()) &&
                        repoObj.get("entityClass").getAsString().equals(entityClass.getName())) {
                    return;
                }
            }

            JsonObject repoObj = new JsonObject();
            repoObj.addProperty("repositoryClass", repositoryClass.getName());
            repoObj.addProperty("entityClass", entityClass.getName());
            repoObj.addProperty("enabled", true);
            repoObj.addProperty("description", Optional.ofNullable(description)
                    .orElse("Repository for " + entityClass.getSimpleName()));

            repoArray.add(repoObj);

            Files.write(defaultRegistrationFile.toPath(), json.toString().getBytes());
            logInfo("Added repository to registration file: " + repositoryClass.getName());
            if (register) {
                registerRepository((Class) repositoryClass, (Class) entityClass);
            }
        } catch (Exception e) {
            logSevere("Failed to add repository to registration file: " + e.getMessage(), e);
        }
    }

    /**
     * <p>Removes a repository from the registration file.</p>
     *
     * <pre><code>boolean removed = controller.removeRepositoryFromRegistrationFile(MyRepo.class, MyEntity.class);</code></pre>
     *
     * @param repositoryClass The repository class
     * @param entityClass     The entity class
     * @return {@code true} if removed, {@code false} otherwise
     */
    public boolean removeRepositoryFromRegistrationFile(Class<?> repositoryClass, Class<?> entityClass) {
        if (!defaultRegistrationFile.exists()) {
            return false;
        }

        try {
            String content = new String(Files.readAllBytes(defaultRegistrationFile.toPath()));
            if (content.isEmpty()) {
                return false;
            }

            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            JsonArray repoArray = json.has("repositories") ? json.getAsJsonArray("repositories") : null;

            if (repoArray == null) {
                return false;
            }

            boolean removed = false;
            for (int i = repoArray.size() - 1; i >= 0; i--) {
                JsonObject repoObj = repoArray.get(i).getAsJsonObject();
                if (repoObj.get("repositoryClass").getAsString().equals(repositoryClass.getName()) &&
                        repoObj.get("entityClass").getAsString().equals(entityClass.getName())) {
                    repoArray.remove(i);
                    removed = true;
                }
            }

            if (removed) {
                Files.write(defaultRegistrationFile.toPath(), json.toString().getBytes());
                logInfo("Removed repository from registration file: " + repositoryClass.getName());
            }

            return removed;

        } catch (Exception e) {
            logSevere("Failed to remove repository from registration file: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * <p>Retrieves an entity class by its name.</p>
     *
     * <pre><code>Optional<Class<?>> entity = controller.getEntityClassByName("PlayerData");</code></pre>
     *
     * @param entityClassName The entity class name
     * @return An Optional containing the entity class, or empty if not found
     */
    public Optional<Class<?>> getEntityClassByName(String entityClassName) {
        return Optional.ofNullable(entityClasses.get(entityClassName));
    }

    /**
     * <p>Updates the status of a repository in the registration file.</p>
     *
     * <pre><code>controller.updateRepositoryStatus(MyRepo.class, MyEntity.class, true);</code></pre>
     *
     * @param repositoryClass The repository class
     * @param entityClass     The entity class
     * @param enabled         The new status (enabled/disabled)
     * @return {@code true} if updated, {@code false} otherwise
     */
    public boolean updateRepositoryStatus(Class<?> repositoryClass, Class<?> entityClass, boolean enabled) {
        if (!defaultRegistrationFile.exists()) {
            return false;
        }

        try {
            String content = new String(Files.readAllBytes(defaultRegistrationFile.toPath()));
            if (content.isEmpty()) {
                return false;
            }

            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            JsonArray repoArray = json.has("repositories") ? json.getAsJsonArray("repositories") : null;

            if (repoArray == null) {
                return false;
            }

            boolean updated = false;
            for (int i = 0; i < repoArray.size(); i++) {
                JsonObject repoObj = repoArray.get(i).getAsJsonObject();
                if (repoObj.get("repositoryClass").getAsString().equals(repositoryClass.getName()) &&
                        repoObj.get("entityClass").getAsString().equals(entityClass.getName())) {
                    repoObj.addProperty("enabled", enabled);
                    updated = true;
                    break;
                }
            }

            if (updated) {
                Files.write(defaultRegistrationFile.toPath(), json.toString().getBytes());
                logInfo("Updated repository status in registration file: " + repositoryClass.getName() + " -> " + (enabled ? "enabled" : "disabled"));
            }

            return updated;

        } catch (Exception e) {
            logSevere("Failed to update repository status in registration file: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * <p>Closes all database connections.</p>
     *
     * <pre><code>controller.closeConnections();</code></pre>
     */
    public void closeConnections() {
        try {
            for (ConnectionPool pool : connectionPools.values()) {
                pool.closeAllConnections();
            }
            connectionPools.clear();
            logInfo("All database connections closed.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>Closes a specific database connection pool.</p>
     *
     * <pre><code>boolean closed = controller.closeConnection("pool-name");</code></pre>
     *
     * @param connectionName Name of the pool to close
     * @return {@code true} if closed, {@code false} otherwise
     */
    public boolean closeConnection(String connectionName) {
        ConnectionPool pool = connectionPools.get(connectionName);
        if (pool != null) {
            try {
                pool.closeAllConnections();
                connectionPools.remove(connectionName);
                logInfo("Closed database connection pool: " + connectionName);
                if (sqlExecutor.getDefaultConnection().equals(connectionName)) {
                    sqlExecutor.setDefaultConnection(null);
                }
                return true;
            } catch (SQLException e) {
                logSevere("Failed to close connection pool '" + connectionName + "': " + e.getMessage(), e);
                return false;
            }
        }
        return false;
    }

    public Connection getConnection(String name) throws SQLException {
        ConnectionPool pool = connectionPools.get(name);
        if (pool != null) {
            return pool.getConnection();
        }
        throw new SQLException("No connection pool found for name: " + name);
    }

    public void releaseConnection(String poolName, Connection connection) {
        ConnectionPool pool = connectionPools.get(poolName);
        if (pool != null) {
            pool.releaseConnection(connection);
        }
    }

    private void logInfo(String message) {
        if (plugin != null && plugin.getLogger() != null) {
            plugin.getLogger().log(Level.INFO, message);
        }
    }

    private void logWarning(String message) {
        if (plugin != null && plugin.getLogger() != null) {
            plugin.getLogger().log(Level.WARNING, message);
        }
    }

    private void logSevere(String message, Exception e) {
        if (plugin != null && plugin.getLogger() != null) {
            plugin.getLogger().log(Level.SEVERE, message, e);
        }
    }
}
