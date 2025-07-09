package de.happybavarian07.coolstufflib.jpa;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
     * Setzt die zu verwendende Verbindung.
     *
     * @param name Name der Verbindung
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
     * Registriert ein Repository-Interface und gibt eine Implementierung zurück.
     *
     * @param <T>                 Repository-Typ
     * @param <E>                 Entity-Typ
     * @param <ID>                ID-Typ
     * @param repositoryInterface Repository-Interface
     * @param entityClass         Entity-Klasse
     * @return Implementierung des Repository-Interfaces
     */
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

        T repository = RepositoryProxy.create(repositoryInterface, dbProperties.getDatabasePrefix(), sqlExecutor, plugin);
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
     * Gibt ein registriertes Repository zurück.
     *
     * @param <T>                 Repository-Typ
     * @param repositoryInterface Repository-Interface
     * @return Repository-Implementierung oder null, wenn nicht gefunden
     */
    @SuppressWarnings("unchecked")
    public <T extends Repository<?, ?>> T getRepository(Class<T> repositoryInterface) {
        return (T) repositories.get(repositoryInterface);
    }

    /**
     * Prüft, ob ein Repository bereits registriert ist.
     *
     * @param repositoryInterface Repository-Interface
     * @return true, wenn das Repository registriert ist, sonst false
     */
    public boolean isRepositoryRegistered(Class<?> repositoryInterface) {
        return repositories.containsKey(repositoryInterface);
    }

    /**
     * Gibt alle registrierten Repository-Interfaces zurück.
     *
     * @return Set mit allen registrierten Repository-Interfaces
     */
    public Set<Class<?>> getAllRepositoryInterfaces() {
        return new HashSet<>(repositories.keySet());
    }

    /**
     * Gibt alle registrierten Repository-Instanzen zurück.
     *
     * @return Collection mit allen registrierten Repository-Instanzen
     */
    public Collection<Repository<?, ?>> getAllRepositories() {
        return new HashSet<>(repositories.values());
    }

    /**
     * Entfernt ein Repository aus der Registrierung.
     *
     * @param repositoryInterface Repository-Interface, das entfernt werden soll
     * @return true, wenn das Repository entfernt wurde, false wenn es nicht registriert war
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
     * Führt eine SQL-Abfrage direkt über den SQLExecutor aus.
     *
     * @param sql    SQL-Abfrage
     * @param params Parameter für die Abfrage
     * @return ResultSet der Abfrage
     * @throws SQLException Bei Fehlern in der Abfrage
     */
    public java.sql.ResultSet executeQuery(String sql, Object... params) throws SQLException {
        return sqlExecutor.executeQuery(sql, params);
    }

    /**
     * Führt ein SQL-Update direkt über den SQLExecutor aus.
     *
     * @param sql    SQL-Update
     * @param params Parameter für das Update
     * @throws SQLException Bei Fehlern beim Update
     */
    public void executeUpdate(String sql, Object... params) throws SQLException {
        sqlExecutor.executeUpdate(sql, params);
    }

    /**
     * Führt mehrere SQL-Statements als Transaktion aus.
     *
     * @param statements Liste von SQL-Statements
     * @throws SQLException Bei Fehlern in der Transaktion
     */
    public void executeTransaction(List<String> statements) throws SQLException {
        sqlExecutor.executeTransaction(statements);
    }

    /**
     * Lädt Repository-Registrierungen aus einer Datei.
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
     * Fügt ein Repository zur Registrierungsdatei hinzu.
     *
     * @param <T>             Repository-Typ
     * @param <E>             Entity-Typ
     * @param repositoryClass Repository-Klasse
     * @param entityClass     Entity-Klasse
     * @param description     Beschreibung des Repositories
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
     * Entfernt ein Repository aus der Registrierungsdatei.
     *
     * @param repositoryClass Repository-Klasse
     * @param entityClass     Entity-Klasse
     * @return true, wenn das Repository entfernt wurde, sonst false
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
     * Gibt eine Entity-Klasse anhand ihres Namens zurück.
     *
     * @param entityClassName Klassenname der Entity
     * @return Optional mit der Entity-Klasse oder leer, wenn nicht gefunden
     */
    public Optional<Class<?>> getEntityClassByName(String entityClassName) {
        return Optional.ofNullable(entityClasses.get(entityClassName));
    }

    /**
     * Aktualisiert den Status eines Repositories in der Registrierungsdatei.
     *
     * @param repositoryClass Repository-Klasse
     * @param entityClass     Entity-Klasse
     * @param enabled         Neuer Status (aktiviert/deaktiviert)
     * @return true bei erfolgreicher Aktualisierung, sonst false
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
     * Schließt alle Datenbankverbindungen.
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
     * Schließt eine spezifische Datenbankverbindung.
     *
     * @param connectionName Name der zu schließenden Verbindung
     * @return true, wenn die Verbindung geschlossen wurde, sonst false
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
