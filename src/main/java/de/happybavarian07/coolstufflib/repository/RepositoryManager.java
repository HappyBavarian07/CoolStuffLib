package de.happybavarian07.coolstufflib.repository;

import de.happybavarian07.coolstufflib.jpa.RepositoryController;
import de.happybavarian07.coolstufflib.jpa.repository.Repository;
import de.happybavarian07.coolstufflib.jpa.utils.DatabaseProperties;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public class RepositoryManager {
    private final JavaPlugin plugin;
    private final Map<String, RepositoryController> controllers = new HashMap<>();
    private final Map<String, DatabaseProperties> connectionProperties = new HashMap<>();
    private boolean repositoryManagerEnabled = false;
    private String defaultController = "default";
    private final File repositoryRegistrationFile;

    public RepositoryManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.repositoryRegistrationFile = new File(plugin.getDataFolder(), "repositories.json");
    }

    public <T extends Repository<E, ID>, E, ID> T registerRepository(Class<T> repositoryInterface, Class<E> entityClass) {
        return getController(defaultController).registerRepository(repositoryInterface, entityClass);
    }

    public <T extends Repository<E, ID>, E, ID> T registerRepository(String controllerName, Class<T> repositoryInterface, Class<E> entityClass) {
        return getController(controllerName).registerRepository(repositoryInterface, entityClass);
    }

    public <T extends Repository<?, ?>> T getRepository(Class<T> repositoryInterface) {
        return getController(defaultController).getRepository(repositoryInterface);
    }

    public <T extends Repository<?, ?>> T getRepository(String controllerName, Class<T> repositoryInterface) {
        return getController(controllerName).getRepository(repositoryInterface);
    }

    public void addConnection(String name, DatabaseProperties properties) {
        connectionProperties.put(name, properties);
        RepositoryController controller = new RepositoryController(plugin, repositoryRegistrationFile, properties);
        controllers.put(name, controller);

        if (controllers.size() == 1) {
            defaultController = name;
        }

        plugin.getLogger().log(Level.INFO, "Added repository connection: " + name);
    }

    public void setDefaultController(String name) {
        if (controllers.containsKey(name)) {
            this.defaultController = name;
            plugin.getLogger().log(Level.INFO, "Set default repository controller to: " + name);
        } else {
            throw new IllegalArgumentException("Repository controller not found: " + name);
        }
    }

    public RepositoryController getController(String name) {
        RepositoryController controller = controllers.get(name);
        if (controller == null) {
            throw new IllegalArgumentException("Repository controller not found: " + name);
        }
        return controller;
    }

    public void loadRepositoriesFromFile() {
        for (RepositoryController controller : controllers.values()) {
            controller.loadRepositoriesFromFile();
        }
    }

    public <T extends Repository<E, ?>, E> void addRepositoryToRegistrationFile(
            Class<T> repositoryClass,
            Class<E> entityClass,
            String description,
            boolean register) {
        getController(defaultController).addRepositoryToRegistrationFile(repositoryClass, entityClass, description, register);
    }

    public void closeAllConnections() {
        for (RepositoryController controller : controllers.values()) {
            controller.closeConnections();
        }
        plugin.getLogger().log(Level.INFO, "Closed all repository connections");
    }

    public boolean isRepositoryManagerEnabled() {
        return repositoryManagerEnabled;
    }

    public void setRepositoryManagerEnabled(boolean enabled) {
        this.repositoryManagerEnabled = enabled;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public Optional<DatabaseProperties> getConnectionProperties(String name) {
        return Optional.ofNullable(connectionProperties.get(name));
    }

    public Map<String, DatabaseProperties> getAllConnectionProperties() {
        return new HashMap<>(connectionProperties);
    }

    public boolean hasConnection(String name) {
        return controllers.containsKey(name);
    }

    public void removeConnection(String name) {
        RepositoryController controller = controllers.remove(name);
        if (controller != null) {
            controller.closeConnections();
            connectionProperties.remove(name);
            plugin.getLogger().log(Level.INFO, "Removed repository connection: " + name);
        }
    }
}
