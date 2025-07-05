package de.happybavarian07.coolstufflib.configstuff.advanced.event;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;

/**
 * <p>Event class representing configuration lifecycle operations such as loading,
 * saving, reloading, creation, deletion, and copying. Provides factory methods
 * for creating specific lifecycle events with appropriate metadata.</p>
 *
 * <p>This event class provides:</p>
 * <ul>
 * <li>Factory methods for all major configuration lifecycle operations</li>
 * <li>Type-safe event creation with predefined lifecycle types</li>
 * <li>Integration with the configuration event bus system</li>
 * <li>Standardized lifecycle event handling across the framework</li>
 * </ul>
 *
 * <pre><code>
 * eventBus.subscribe(ConfigLifecycleEvent.class, event -> {
 *     if (event.getType() == ConfigLifecycleEvent.Type.SAVE) {
 *         System.out.println("Config saved: " + event.getConfig().getName());
 *     }
 * });
 * eventBus.publish(ConfigLifecycleEvent.configSave(myConfig));
 * </code></pre>
 */
public class ConfigLifecycleEvent extends ConfigEvent {
    private final Type type;

    private ConfigLifecycleEvent(AdvancedConfig config, Type type) {
        super(config);
        this.type = type;
    }

    /**
     * <p>Creates a lifecycle event indicating that configuration data has been
     * copied from one configuration instance to another.</p>
     *
     * <pre><code>
     * ConfigEvent event = ConfigLifecycleEvent.configCopied(sourceConfig, targetConfig);
     * eventBus.publish(event);
     * </code></pre>
     *
     * @param baseAdvancedConfig the source configuration that was copied from
     * @param config2 the target configuration that received the copied data
     * @return a lifecycle event representing the copy operation
     */
    public static ConfigEvent configCopied(AdvancedConfig baseAdvancedConfig, AdvancedConfig config2) {
        return new ConfigLifecycleEvent(config2, Type.COPY);
    }

    /**
     * <p>Creates a lifecycle event indicating that a configuration has been
     * loaded from its persistent storage.</p>
     *
     * <pre><code>
     * ConfigLifecycleEvent event = ConfigLifecycleEvent.configLoad(myConfig);
     * eventBus.publish(event);
     * </code></pre>
     *
     * @param config the configuration that was loaded
     * @return a lifecycle event representing the load operation
     */
    public static ConfigLifecycleEvent configLoad(AdvancedConfig config) {
        return new ConfigLifecycleEvent(config, Type.LOAD);
    }

    /**
     * <p>Creates a lifecycle event indicating that a configuration has been
     * saved to its persistent storage.</p>
     *
     * <pre><code>
     * ConfigLifecycleEvent event = ConfigLifecycleEvent.configSave(myConfig);
     * eventBus.publish(event);
     * </code></pre>
     *
     * @param config the configuration that was saved
     * @return a lifecycle event representing the save operation
     */
    public static ConfigLifecycleEvent configSave(AdvancedConfig config) {
        return new ConfigLifecycleEvent(config, Type.SAVE);
    }

    /**
     * <p>Creates a lifecycle event indicating that a configuration has been
     * reloaded from its persistent storage, refreshing its current state.</p>
     *
     * <pre><code>
     * ConfigLifecycleEvent event = ConfigLifecycleEvent.configReload(myConfig);
     * eventBus.publish(event);
     * </code></pre>
     *
     * @param config the configuration that was reloaded
     * @return a lifecycle event representing the reload operation
     */
    public static ConfigLifecycleEvent configReload(AdvancedConfig config) {
        return new ConfigLifecycleEvent(config, Type.RELOAD);
    }

    /**
     * <p>Creates a lifecycle event indicating that a new configuration instance
     * has been created and initialized.</p>
     *
     * <pre><code>
     * ConfigLifecycleEvent event = ConfigLifecycleEvent.configCreate(newConfig);
     * eventBus.publish(event);
     * </code></pre>
     *
     * @param config the configuration that was created
     * @return a lifecycle event representing the creation operation
     */
    public static ConfigLifecycleEvent configCreate(AdvancedConfig config) {
        return new ConfigLifecycleEvent(config, Type.CREATE);
    }

    /**
     * <p>Creates a lifecycle event indicating that a configuration instance
     * has been deleted and its resources cleaned up.</p>
     *
     * <pre><code>
     * ConfigLifecycleEvent event = ConfigLifecycleEvent.configDelete(oldConfig);
     * eventBus.publish(event);
     * </code></pre>
     *
     * @param config the configuration that was deleted
     * @return a lifecycle event representing the deletion operation
     */
    public static ConfigLifecycleEvent configDelete(AdvancedConfig config) {
        return new ConfigLifecycleEvent(config, Type.DELETE);
    }

    /**
     * <p>Creates a lifecycle event indicating that a configuration's data
     * has been cleared, removing all stored values.</p>
     *
     * <pre><code>
     * ConfigLifecycleEvent event = ConfigLifecycleEvent.configClear(myConfig);
     * eventBus.publish(event);
     * </code></pre>
     *
     * @param config the configuration that was cleared
     * @return a lifecycle event representing the clear operation
     */
    public static ConfigLifecycleEvent configClear(AdvancedConfig config) {
        return new ConfigLifecycleEvent(config, Type.CLEAR);
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        LOAD, SAVE, RELOAD, CREATE, DELETE, COPY, CLEAR
    }
}
