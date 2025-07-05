package de.happybavarian07.coolstufflib.configstuff.advanced.interfaces;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEventBus;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEventListener;

import java.util.Map;
import java.util.Set;

/**
 * <p>Base interface for configuration modules that extend configuration functionality
 * through a managed lifecycle, event handling, and dependency management.</p>
 *
 * <p>Modules provide extensible functionality such as:</p>
 * <ul>
 *   <li>Value validation and transformation</li>
 *   <li>Backup and history management</li>
 *   <li>Encryption and security</li>
 *   <li>Change tracking and auditing</li>
 *   <li>Integration with external systems</li>
 * </ul>
 *
 * <pre><code>
 * public class ValidationModule extends AbstractBaseConfigModule {
 *     public ValidationModule() {
 *         super("ValidationModule", "Validates configuration values", "1.0.0");
 *     }
 *
 *     protected void onEnable() {
 *         registerEventListener(config.getEventBus(),
 *             ConfigValueEvent.class, this::validateValue);
 *     }
 * }
 * </code></pre>
 */
public interface BaseConfigModule {

    /**
     * <p>Gets the unique identifier name for this module.</p>
     *
     * <pre><code>
     * String moduleName = module.getName();
     * </code></pre>
     *
     * @return the module name, never null
     */
    String getName();

    /**
     * <p>Gets a human-readable description of the module's functionality.</p>
     *
     * <pre><code>
     * String description = module.getDescription();
     * </code></pre>
     *
     * @return the module description, never null
     */
    String getDescription();

    /**
     * <p>Gets the version string for this module implementation.</p>
     *
     * <pre><code>
     * String version = module.getVersion();
     * </code></pre>
     *
     * @return the module version, never null
     */
    String getVersion();

    /**
     * <p>Gets the current lifecycle state of this module.</p>
     *
     * <pre><code>
     * if (module.getState() == ModuleState.ENABLED) {
     *     // Module is active
     * }
     * </code></pre>
     *
     * @return the current module state, never null
     */
    ModuleState getState();

    /**
     * <p>Initializes the module with a configuration instance, preparing it for operation.</p>
     *
     * <pre><code>
     * ValidationModule module = new ValidationModule();
     * module.initialize(config);
     * </code></pre>
     *
     * @param config the configuration instance to attach to
     * @throws IllegalStateException if module is already initialized
     */
    void initialize(AdvancedConfig config);

    /**
     * <p>Enables the module, activating its functionality and event listeners.</p>
     *
     * <pre><code>
     * module.enable();
     * assert module.getState() == ModuleState.ENABLED;
     * </code></pre>
     *
     * @throws IllegalStateException if module is not initialized
     */
    void enable();

    /**
     * <p>Disables the module while preserving its configuration and state.</p>
     *
     * <pre><code>
     * module.disable();
     * assert module.getState() == ModuleState.DISABLED;
     * </code></pre>
     *
     * @throws IllegalStateException if module is not enabled
     */
    void disable();

    /**
     * <p>Performs complete cleanup, resetting the module to uninitialized state.</p>
     *
     * <pre><code>
     * module.cleanup();
     * assert module.getState() == ModuleState.UNINITIALIZED;
     * </code></pre>
     */
    void cleanup();

    boolean isEnabled();

    boolean isInitialized();

    Set<String> getDependencies();

    /**
     * <p>Gets comprehensive state information about the module including configuration and metrics.</p>
     *
     * <pre><code>
     * Map&lt;String, Object&gt; state = module.getModuleState();
     * String status = (String) state.get("status");
     * </code></pre>
     *
     * @return map containing module state details
     */
    Map<String, Object> getModuleState();

    boolean isConfigured();

    void configure(Map<String, Object> configuration);

    /**
     * <p>Registers an event listener for specific configuration events.</p>
     *
     * <pre><code>
     * module.registerEventListener(config.getEventBus(),
     *     ConfigValueEvent.class, this::onValueChange);
     * </code></pre>
     *
     * @param eventBus  the event bus to register with
     * @param eventType the type of events to listen for
     * @param listener  the listener callback
     * @param <T>       the event type
     */
    <T extends ConfigEvent> void registerEventListener(ConfigEventBus eventBus, Class<T> eventType, ConfigEventListener<T> listener);

    void copyFrom(BaseConfigModule module);

    <T extends ConfigEvent> void unregisterEventListener(ConfigEventBus eventBus, Class<T> eventType, ConfigEventListener<T> listener);

    void addDependency(BaseConfigModule dependency);

    void removeDependency(BaseConfigModule dependency);

    Map<? extends ConfigEventBus, ? extends Map<Class<? extends ConfigEvent>, Set<ConfigEventListener<?>>>> getRegisteredListeners();

    AdvancedConfig getConfig();

    Map<String, Object> getModuleConfiguration();

    /**
     * <p>Enumeration of possible module lifecycle states.</p>
     */
    enum ModuleState {
        UNINITIALIZED,
        INITIALIZED,
        ERROR,
        ENABLED,
        DISABLED
    }
}
