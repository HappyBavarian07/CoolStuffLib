package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEventBus;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEventListener;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.BaseConfigModule;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * <p>Abstract base implementation for configuration modules that provides a standardized
 * lifecycle management system with state tracking, event listener registration, and
 * dependency management capabilities.</p>
 *
 * <p>This abstract class implements the common functionality required by all configuration
 * modules including:</p>
 * <ul>
 * <li>Module lifecycle management (initialize, enable, disable, cleanup)</li>
 * <li>State tracking with proper state transitions</li>
 * <li>Event listener registration and automatic cleanup</li>
 * <li>Dependency management between modules</li>
 * <li>Configuration management and validation</li>
 * <li>Thread-safe operations for concurrent environments</li>
 * </ul>
 *
 * <p>Subclasses must implement the abstract lifecycle methods to provide their
 * specific functionality while inheriting the standardized module behavior.</p>
 *
 * <pre><code>
 * public class CustomModule extends AbstractBaseConfigModule {
 *     public CustomModule() {
 *         super("CustomModule", "Handles custom configuration logic", "1.0.0");
 *     }
 *
 *     protected void onInitialize() {
 *         // Module initialization logic
 *     }
 *
 *     protected void onEnable() {
 *         // Module enabling logic
 *     }
 *
 *     protected void onDisable() {
 *         // Module disabling logic
 *     }
 *
 *     protected void onCleanup() {
 *         // Module cleanup logic
 *     }
 * }
 * </code></pre>
 */
public abstract class AbstractBaseConfigModule implements BaseConfigModule {
    protected final String name;
    protected final String description;
    protected final String version;
    protected final Set<String> dependencies;
    protected final Map<ConfigEventBus, Map<Class<? extends ConfigEvent>, Set<ConfigEventListener<?>>>> registeredListeners;

    protected AdvancedConfig config;
    protected Map<String, Object> moduleConfiguration;
    protected ModuleState state = ModuleState.UNINITIALIZED;

    /**
     * <p>Constructs a new AbstractBaseConfigModule with the specified metadata.</p>
     *
     * <pre><code>
     * public class ValidationModule extends AbstractBaseConfigModule {
     *     public ValidationModule() {
     *         super("ValidationModule", "Validates configuration values", "2.1.0");
     *     }
     * }
     * </code></pre>
     *
     * @param name the unique name identifier for this module
     * @param description a brief description of the module's functionality
     * @param version the version string for this module
     */
    public AbstractBaseConfigModule(String name, String description, String version) {
        this.name = name;
        this.description = description;
        this.version = version;
        this.dependencies = new CopyOnWriteArraySet<>();
        this.registeredListeners = new ConcurrentHashMap<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public ModuleState getState() {
        return state;
    }

    /**
     * <p>Initializes the module with the provided configuration instance. This method
     * handles state validation, calls the abstract onInitialize method, and manages
     * state transitions with proper error handling.</p>
     *
     * <pre><code>
     * AbstractBaseConfigModule module = new CustomModule();
     * AdvancedConfig config = new AdvancedPersistentConfig("config.yml");
     * module.initialize(config);
     * // Module is now in INITIALIZED state
     * </code></pre>
     *
     * @param config the AdvancedConfig instance to associate with this module
     * @throws IllegalStateException if the module is already initialized
     * @throws RuntimeException if initialization fails
     */
    @Override
    public void initialize(AdvancedConfig config) {
        if (state != ModuleState.UNINITIALIZED) {
            throw new IllegalStateException("Module is already initialized");
        }

        this.config = config;
        try {
            onInitialize();
            state = ModuleState.INITIALIZED;
        } catch (Exception e) {
            state = ModuleState.ERROR;
            throw new RuntimeException("Failed to initialize module: " + name, e);
        }
    }

    /**
     * <p>Abstract method called during module initialization. Subclasses must implement
     * this method to perform their specific initialization logic.</p>
     *
     * <p>This method is called automatically by the initialize method and should not
     * be called directly. Any exceptions thrown will cause the module to enter ERROR state.</p>
     */
    protected abstract void onInitialize();

    /**
     * <p>Enables the module if it is in the appropriate state. This method validates
     * the current state, calls the abstract onEnable method, and manages state
     * transitions with proper error handling.</p>
     *
     * <pre><code>
     * module.initialize(config);
     * module.enable();
     * // Module is now in ENABLED state and actively processing
     * </code></pre>
     *
     * @throws IllegalStateException if the module is not in a valid state for enabling
     * @throws RuntimeException if enabling fails
     */
    @Override
    public void enable() {
        if (state != ModuleState.INITIALIZED && state != ModuleState.DISABLED) {
            if (state == ModuleState.ERROR) {
                throw new IllegalStateException("Module is in error state, cannot enable");
            }
            if (state == ModuleState.UNINITIALIZED) {
                throw new IllegalStateException("Module must be initialized before enabling");
            }
            if( state == ModuleState.ENABLED) {
                throw new IllegalStateException("Module is already enabled");
            }
        }

        try {
            onEnable();
            state = ModuleState.ENABLED;
        } catch (Exception e) {
            state = ModuleState.ERROR;
            throw new RuntimeException("Failed to enable module: " + name, e);
        }
    }

    /**
     * <p>Abstract method called during module enabling. Subclasses must implement
     * this method to perform their specific enabling logic such as starting
     * background tasks or registering event listeners.</p>
     *
     * <p>This method is called automatically by the enable method and should not
     * be called directly. Any exceptions thrown will cause the module to enter ERROR state.</p>
     */
    protected abstract void onEnable();

    /**
     * <p>Disables the module if it is currently enabled. This method validates
     * the current state, calls the abstract onDisable method, and manages state
     * transitions with proper error handling.</p>
     *
     * <pre><code>
     * module.disable();
     * // Module is now in DISABLED state and no longer processing
     * </code></pre>
     *
     * @throws IllegalStateException if the module is not in a valid state for disabling
     * @throws RuntimeException if disabling fails
     */
    @Override
    public void disable() {
        if (state != ModuleState.ENABLED) {
            if (state == ModuleState.ERROR) {
                throw new IllegalStateException("Module is in error state, cannot disable");
            }
            if (state == ModuleState.UNINITIALIZED || state == ModuleState.INITIALIZED) {
                throw new IllegalStateException("Module must be enabled before disabling");
            }
        }

        try {
            onDisable();
            state = ModuleState.DISABLED;
        } catch (Exception e) {
            state = ModuleState.ERROR;
            throw new RuntimeException("Failed to disable module: " + name, e);
        }
    }

    /**
     * <p>Abstract method called during module disabling. Subclasses must implement
     * this method to perform their specific disabling logic such as stopping
     * background tasks or unregistering event listeners.</p>
     *
     * <p>This method is called automatically by the disable method and should not
     * be called directly. Any exceptions thrown will cause the module to enter ERROR state.</p>
     */
    protected abstract void onDisable();

    /**
     * <p>Performs cleanup operations on the module, including automatic unregistration
     * of all event listeners and calling the abstract onCleanup method. This method
     * resets the module to an uninitialized state.</p>
     *
     * <pre><code>
     * module.disable();
     * module.cleanup();
     * // Module is now back to UNINITIALIZED state and ready for reuse
     * </code></pre>
     *
     * @throws IllegalStateException if the module is not in a valid state for cleanup
     * @throws RuntimeException if cleanup fails
     */
    @Override
    public void cleanup() {
        if (state != ModuleState.DISABLED && state != ModuleState.ERROR) {
            throw new IllegalStateException("Module must be disabled or in error state before cleanup");
        }

        try {
            for (Map.Entry<ConfigEventBus, Map<Class<? extends ConfigEvent>, Set<ConfigEventListener<?>>>> busEntry : registeredListeners.entrySet()) {
                ConfigEventBus eventBus = busEntry.getKey();
                for (Map.Entry<Class<? extends ConfigEvent>, Set<ConfigEventListener<?>>> listenersEntry : busEntry.getValue().entrySet()) {
                    for (ConfigEventListener<?> listener : new ArrayList<>(listenersEntry.getValue())) {
                        unregisterTypedListener(eventBus, listenersEntry.getKey(), listener);
                    }
                }
            }

            onCleanup();
            state = ModuleState.UNINITIALIZED;
            config = null;
            moduleConfiguration = null;
        } catch (Exception e) {
            state = ModuleState.ERROR;
            throw new RuntimeException("Failed to cleanup module: " + name, e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends ConfigEvent> void unregisterTypedListener(ConfigEventBus eventBus, Class<? extends ConfigEvent> eventType, ConfigEventListener<?> listener) {
        unregisterEventListener(eventBus, (Class<T>) eventType, (ConfigEventListener<T>) listener);
    }

    /**
     * <p>Abstract method called during module cleanup. Subclasses must implement
     * this method to perform their specific cleanup logic such as releasing
     * resources or clearing internal state.</p>
     *
     * <p>This method is called automatically by the cleanup method after all
     * event listeners have been unregistered. Any exceptions thrown will cause
     * the module to enter ERROR state.</p>
     */
    protected abstract void onCleanup();

    @Override
    public Set<String> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    /**
     * <p>Adds a dependency on another module. This module will require the specified
     * module to be loaded and initialized before this module can be enabled.</p>
     *
     * <pre><code>
     * public class DatabaseModule extends AbstractBaseConfigModule {
     *     protected void onInitialize() {
     *         addDependency("ConnectionPoolModule");
     *         addDependency("ValidationModule");
     *     }
     * }
     * </code></pre>
     *
     * @param moduleName the name of the module this module depends on
     */
    protected void addDependency(String moduleName) {
        dependencies.add(moduleName);
    }

    /**
     * <p>Removes a dependency on another module. This module will no longer
     * require the specified module to be loaded before enabling.</p>
     *
     * <pre><code>
     * protected void onInitialize() {
     *     removeDependency("OptionalModule");
     * }
     * </code></pre>
     *
     * @param moduleName the name of the module to remove as a dependency
     */
    protected void removeDependency(String moduleName) {
        dependencies.remove(moduleName);
    }

    @Override
    public boolean isConfigured() {
        return moduleConfiguration != null;
    }

    @Override
    public void configure(Map<String, Object> configuration) {
        this.moduleConfiguration = new HashMap<>(configuration);
    }

    /**
     * <p>Registers an event listener for the specified event type on the given event bus.
     * The listener registration is tracked for automatic cleanup during module cleanup.</p>
     *
     * <pre><code>
     * protected void onEnable() {
     *     registerEventListener(config.getEventBus(), ConfigValueEvent.class, event -> {
     *         System.out.println("Value changed: " + event.getPath());
     *     });
     * }
     * </code></pre>
     *
     * @param eventBus the event bus to register the listener on
     * @param eventType the class of events to listen for
     * @param listener the listener to invoke when events occur
     * @param <T> the event type parameter
     */
    @Override
    public <T extends ConfigEvent> void registerEventListener(ConfigEventBus eventBus, Class<T> eventType, ConfigEventListener<T> listener) {
        if (eventBus == null || eventType == null || listener == null) {
            return;
        }

        registeredListeners.computeIfAbsent(eventBus, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(eventType, k -> ConcurrentHashMap.newKeySet())
                .add(listener);

        eventBus.subscribe(eventType, listener);
    }

    /**
     * <p>Copies the state and configuration from another module instance. This method
     * performs a deep copy of the module configuration, state, dependencies, and
     * registered listeners.</p>
     *
     * <pre><code>
     * BaseConfigModule sourceModule = moduleManager.getModule("SourceModule");
     * BaseConfigModule targetModule = new CustomModule();
     * targetModule.copyFrom(sourceModule);
     * </code></pre>
     *
     * @param module the module to copy state and configuration from
     * @throws IllegalArgumentException if the source module is null
     */
    @Override
    public void copyFrom(BaseConfigModule module) {
        if (module == null) {
            throw new IllegalArgumentException("Module to copy from cannot be null");
        }

        this.moduleConfiguration = module.getModuleConfiguration();
        this.state = module.getState();
        this.config = module.getConfig();
        this.registeredListeners.putAll(module.getRegisteredListeners());
        this.dependencies.addAll(module.getDependencies());
    }

    @Override
    public Map<ConfigEventBus, Map<Class<? extends ConfigEvent>, Set<ConfigEventListener<?>>>> getRegisteredListeners() {
        return registeredListeners;
    }

    /**
     * <p>Returns the current module configuration as a map of key-value pairs.
     * This configuration is set via the configure method and may be null if
     * no configuration has been provided.</p>
     *
     * <pre><code>
     * Map&lt;String, Object&gt; config = module.getModuleConfiguration();
     * if (config != null) {
     *     String setting = (String) config.get("timeout");
     * }
     * </code></pre>
     *
     * @return the module configuration map, or null if not configured
     */
    public Map<String, Object> getModuleConfiguration() {
        return moduleConfiguration;
    }

    @Override
    public AdvancedConfig getConfig() {
        return config;
    }

    /**
     * <p>Unregisters an event listener from the specified event type on the given event bus.
     * This removes the listener from both the internal tracking and the event bus itself.</p>
     *
     * <pre><code>
     * ConfigEventListener&lt;ConfigValueEvent&gt; listener = // ... existing listener
     * module.unregisterEventListener(config.getEventBus(), ConfigValueEvent.class, listener);
     * </code></pre>
     *
     * @param eventBus the event bus to unregister the listener from
     * @param eventType the class of events to stop listening for
     * @param listener the listener to remove
     * @param <T> the event type parameter
     */
    @Override
    public <T extends ConfigEvent> void unregisterEventListener(ConfigEventBus eventBus, Class<T> eventType, ConfigEventListener<T> listener) {
        if (eventBus == null || eventType == null || listener == null) {
            return;
        }

        Map<Class<? extends ConfigEvent>, Set<ConfigEventListener<?>>> busListeners = registeredListeners.get(eventBus);
        if (busListeners != null) {
            Set<ConfigEventListener<?>> typeListeners = busListeners.get(eventType);
            if (typeListeners != null) {
                typeListeners.remove(listener);
                if (typeListeners.isEmpty()) {
                    busListeners.remove(eventType);
                }
            }
            if (busListeners.isEmpty()) {
                registeredListeners.remove(eventBus);
            }
        }

        eventBus.unsubscribe(eventType, listener);
    }

    /**
     * <p>Returns a comprehensive state snapshot of the module including metadata,
     * current state, configuration status, dependencies, and any additional
     * state information provided by subclasses.</p>
     *
     * <pre><code>
     * Map&lt;String, Object&gt; state = module.getModuleState();
     * System.out.println("Module " + state.get("name") + " is " + state.get("state"));
     * </code></pre>
     *
     * @return a map containing the complete module state information
     */
    @Override
    public final Map<String, Object> getModuleState() {
        Map<String, Object> state = new HashMap<>();

        state.put("name", name);
        state.put("description", description);
        state.put("version", version);
        state.put("state", this.state.name());
        state.put("configured", isConfigured());
        state.put("dependencies", new ArrayList<>(dependencies));

        Map<String, Object> additionalState = getAdditionalModuleState();
        if (additionalState != null) {
            state.putAll(additionalState);
        }

        return state;
    }

    @Override
    public boolean isEnabled() {
        return state == ModuleState.ENABLED;
    }

    @Override
    public boolean isInitialized() {
        return state == ModuleState.INITIALIZED || state == ModuleState.ENABLED;
    }

    public void setState(ModuleState state) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }
        this.state = state;
    }

    public void setModuleConfiguration(Map<String, Object> moduleConfiguration) {
        if (moduleConfiguration == null) {
            throw new IllegalArgumentException("Module configuration cannot be null");
        }
        this.moduleConfiguration = new HashMap<>(moduleConfiguration);
    }

    @Override
    public void addDependency(BaseConfigModule dependency) {
        if (dependency == null) {
            throw new IllegalArgumentException("Dependency cannot be null");
        }
        dependencies.add(dependency.getName());
    }

    @Override
    public void removeDependency(BaseConfigModule dependency) {
        if (dependency == null) {
            throw new IllegalArgumentException("Dependency cannot be null");
        }
        dependencies.remove(dependency.getName());
    }

    /**
     * <p>Abstract method that allows subclasses to provide additional state information
     * to be included in the module state snapshot. This method is called by getModuleState
     * to collect any module-specific state data.</p>
     *
     * <p>Subclasses should override this method to provide relevant state information
     * such as configuration values, statistics, or operational status.</p>
     *
     * <pre><code>
     * protected Map&lt;String, Object&gt; getAdditionalModuleState() {
     *     Map&lt;String, Object&gt; state = new HashMap&lt;&gt;();
     *     state.put("processedCount", processedEventCount);
     *     state.put("lastProcessed", lastProcessedTime);
     *     return state;
     * }
     * </code></pre>
     *
     * @return a map of additional state information, or null if no additional state
     */
    protected abstract Map<String, Object> getAdditionalModuleState();
}
