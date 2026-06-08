package de.happybavarian07.coolstufflib.configstuff.advanced.event;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.BaseConfigModule;

/**
 * <p>Event fired when a configuration module lifecycle operation occurs.</p>
 *
 * <p>This event tracks changes to config modules, which are logical groupings of
 * related configuration values. Modules allow organizing configuration into named
 * namespaces and can be enabled/disabled independently.</p>
 *
 * <pre><code>
 * // Subscribe to module events
 * eventBus.subscribe(ConfigModuleEvent.class, event -> {
 *     if (event.getType() == ConfigModuleEvent.Type.REGISTERED) {
 *         System.out.println("Module registered: " + event.getModule().getName());
 *         initializeModuleResources(event.getModule());
 *     }
 * });
 *
 * // Triggering the event
 * config.registerModule(myModule);
 * </code></pre>
 *
 * <p>Event types:</p>
 * <ul>
 *   <li>{@link Type#REGISTERED} - A new module was registered with the config</li>
 *   <li>{@link Type#UNREGISTERED} - An existing module was unregistered/removed</li>
 *   <li>{@link Type#ENABLED} - A disabled module was enabled</li>
 *   <li>{@link Type#DISABLED} - An enabled module was disabled</li>
 * </ul>
 *
 * @see ConfigEventBus
 * @see ConfigEventListener
 * @see BaseConfigModule
 */
public class ConfigModuleEvent extends ConfigEvent {
    private final BaseConfigModule module;
    private final Type eventType;

    /**
     * Creates a module event indicating that a new module was registered.
     *
     * <pre><code>
 * ConfigModuleEvent event = ConfigModuleEvent.moduleRegistered(config, databaseModule);
 * eventBus.publish(event);
 * </code></pre>
     *
     * @param config the configuration that received the new module
     * @param module the module that was registered
     * @return a ConfigModuleEvent with Type.REGISTERED
     */
    public static ConfigModuleEvent moduleRegistered(AdvancedConfig config, BaseConfigModule module) {
        return new ConfigModuleEvent(Type.REGISTERED, config, module);
    }

    /**
     * Creates a module event indicating that a module was unregistered.
     *
     * <pre><code>
 * ConfigModuleEvent event = ConfigModuleEvent.moduleUnregistered(config, tempModule);
 * eventBus.publish(event);
 * </code></pre>
     *
     * @param config the configuration from which the module was removed
     * @param module the module that was unregistered
     * @return a ConfigModuleEvent with Type.UNREGISTERED
     */
    public static ConfigModuleEvent moduleUnregistered(AdvancedConfig config, BaseConfigModule module) {
        return new ConfigModuleEvent(Type.UNREGISTERED, config, module);
    }

    /**
     * Creates a module event indicating that a disabled module was enabled.
     *
     * <pre><code>
 * ConfigModuleEvent event = ConfigModuleEvent.moduleEnabled(config, optionalModule);
 * eventBus.publish(event);
 * </code></pre>
     *
     * @param config the configuration containing the module
     * @param module the module that was enabled
     * @return a ConfigModuleEvent with Type.ENABLED
     */
    public static ConfigModuleEvent moduleEnabled(AdvancedConfig config, BaseConfigModule module) {
        return new ConfigModuleEvent(Type.ENABLED, config, module);
    }

    /**
     * Creates a module event indicating that an enabled module was disabled.
     *
     * <pre><code>
 * ConfigModuleEvent event = ConfigModuleEvent.moduleDisabled(config, debugModule);
 * eventBus.publish(event);
 * </code></pre>
     *
     * @param config the configuration containing the module
     * @param module the module that was disabled
     * @return a ConfigModuleEvent with Type.DISABLED
     */
    public static ConfigModuleEvent moduleDisabled(AdvancedConfig config, BaseConfigModule module) {
        return new ConfigModuleEvent(Type.DISABLED, config, module);
    }

    private ConfigModuleEvent(Type eventType, AdvancedConfig config, BaseConfigModule module) {
        super(config);
        this.eventType = eventType;
        this.module = module;
    }

    /**
     * Gets the type of operation that triggered this event.
     *
     * @return the Type indicating what action was performed on the module
     */
    public Type getType() {
        return eventType;
    }

    /**
     * Gets the config module affected by this event.
     *
     * <pre><code>
 * BaseConfigModule module = event.getModule();
 * System.out.println("Module: " + module.getName());
 * </code></pre>
     *
     * @return the BaseConfigModule instance that was modified
     */
    public BaseConfigModule getModule() {
        return module;
    }

    /**
     * Enum representing the types of config module operations.
     *
     * <ul>
     *   <li>{@link #REGISTERED} - A new module was registered with the configuration</li>
     *   <li>{@link #UNREGISTERED} - An existing module was removed from the configuration</li>
     *   <li>{@link #ENABLED} - A disabled module was enabled and activated</li>
     *   <li>{@link #DISABLED} - An enabled module was disabled and deactivated</li>
     * </ul>
     */
    public enum Type {
        /** A new module was registered with the configuration. */
        REGISTERED,
        /** An existing module was unregistered/removed from the configuration. */
        UNREGISTERED,
        /** A disabled module was enabled and is now active. */
        ENABLED,
        /** An enabled module was disabled and is now inactive. */
        DISABLED
    }
}
