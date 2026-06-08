package de.happybavarian07.coolstufflib.configstuff.advanced.event;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;

/**
 * <p>Event fired when a configuration value is accessed or modified.</p>
 *
 * <p>This event provides detailed information about configuration value operations,
 * including the affected key, old and new values, and the type of operation performed.
 * It extends {@link ConfigEvent} to integrate with the event bus system.</p>
 *
 * <pre><code>
 * // Subscribe to config value change events
 * eventBus.subscribe(ConfigValueEvent.class, event -> {
 *     if ("database.url".equals(event.getFullPath())) {
 *         System.out.println("Database URL changed from " + 
 *             event.getOldValue() + " to " + event.getNewValue());
 *         reconnectDatabase((String) event.getNewValue());
 *     }
 * });
 *
 * // Triggering the event
 * config.set("database.url", newUrl);
 * </code></pre>
 *
 * <p>Event types:</p>
 * <ul>
 *   <li>{@link Type#SET} - A value was set or updated</li>
 *   <li>{@link Type#GET} - A value was read/accessed</li>
 *   <li>{@link Type#REMOVE} - A value was deleted</li>
 * </ul>
 *
 * @see ConfigEventBus
 * @see ConfigEventListener
 */
public class ConfigValueEvent extends ConfigEvent {
    private final String key;
    private final Object oldValue;
    private Object newValue;
    private final ConfigSection section;
    private final Type eventType;

    /**
     * Creates a value set event indicating a configuration value was modified.
     *
     * <pre><code>
 * ConfigValueEvent event = ConfigValueEvent.valueSet(config, section, "key", oldVal, newVal);
 * eventBus.publish(event);
 * </code></pre>
     *
     * @param config the config containing the value
     * @param section the section containing the key (may be null for root-level keys)
     * @param key the configuration key that was set
     * @param oldValue the previous value before modification
     * @param newValue the new value being set
     * @return a ConfigValueEvent with Type.SET
     */
    public static ConfigValueEvent valueSet(AdvancedConfig config, ConfigSection section, String key, Object oldValue, Object newValue) {
        return new ConfigValueEvent(Type.SET, config, section, key, oldValue, newValue);
    }

    /**
     * Creates a value get event indicating a configuration value was read.
     *
     * <pre><code>
 * ConfigValueEvent event = ConfigValueEvent.valueGet(config, null, "setting", currentValue);
 * eventBus.publish(event);
 * </code></pre>
     *
     * @param config the config containing the value
     * @param section the section containing the key (may be null for root-level keys)
     * @param key the configuration key that was accessed
     * @param value the current value being read
     * @return a ConfigValueEvent with Type.GET
     */
    public static ConfigValueEvent valueGet(AdvancedConfig config, ConfigSection section, String key, Object value) {
        return new ConfigValueEvent(Type.GET, config, section, key, null, value);
    }

    /**
     * Creates a value remove event indicating a configuration value was deleted.
     *
     * <pre><code>
 * ConfigValueEvent event = ConfigValueEvent.valueRemove(config, section, "tempKey", oldValue);
 * eventBus.publish(event);
 * </code></pre>
     *
     * @param config the config containing the value
     * @param section the section containing the key (may be null for root-level keys)
     * @param key the configuration key that was removed
     * @param oldValue the value that was deleted
     * @return a ConfigValueEvent with Type.REMOVE
     */
    public static ConfigValueEvent valueRemove(AdvancedConfig config, ConfigSection section, String key, Object oldValue) {
        return new ConfigValueEvent(Type.REMOVE, config, section, key, oldValue, null);
    }

    private ConfigValueEvent(Type eventType, AdvancedConfig config, ConfigSection section, String key, Object oldValue, Object newValue) {
        super(config);
        this.eventType = eventType;
        this.section = section;
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * Gets the type of operation that triggered this event.
     *
     * @return the operation type (SET, GET, or REMOVE)
     */
    public Type getType() {
        return eventType;
    }

    /**
     * Gets the source of this event.
     *
     * <p>Returns the containing section if present, otherwise returns the config.</p>
     *
     * @return the ConfigSection or AdvancedConfig that is the source
     */
    @Override
    public Object getSource() {
        return section != null ? section : getConfig();
    }

    /**
     * Gets the configuration key affected by this event.
     *
     * <pre><code>
 * String key = event.getKey(); // e.g., "max-players"
 * </code></pre>
     *
     * @return the key name within its section
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the full hierarchical path to the configuration value.
     *
     * <pre><code>
 * event.getFullPath(); // Returns "database.connection.url"
 * </code></pre>
     *
     * <p>The path includes section hierarchy when applicable, using dot notation.</p>
     *
     * @return the full path including section prefix if present
     */
    public String getFullPath() {
        return section != null ?
            (section.getFullPath().isEmpty() ? key : section.getFullPath() + "." + key) :
            key;
    }

    /**
     * Gets the previous value before the operation.
     *
     * <pre><code>
 * Object oldVal = event.getOldValue();
 * if (oldVal != null) {
 *     System.out.println("Previous value: " + oldVal);
 * }
 * </code></pre>
     *
     * @return the previous value, or {@code null} for GET events
     */
    public Object getOldValue() {
        return oldValue;
    }

    /**
     * Gets the new value after the operation.
     *
     * <pre><code>
 * String newUrl = (String) event.getNewValue();
 * </code></pre>
     *
     * @return the new value, or {@code null} for REMOVE events
     */
    public Object getNewValue() {
        return newValue;
    }

    /**
     * Sets the new value for this event. Primarily used internally.
     *
     * @param newValue the new value to set
     */
    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }

    /**
     * Gets the configuration section containing the affected key.
     *
     * @return the ConfigSection, or {@code null} if at root level
     */
    public ConfigSection getSection() {
        return section;
    }

    /**
     * Checks if this event can be cancelled.
     *
     * <p>SET and REMOVE operations can be cancelled to prevent the change.
     * GET operations cannot be cancelled as they are read-only.</p>
     *
     * @return {@code true} if the operation type allows cancellation
     */
    @Override
    public boolean isCancellable() {
        // Only SET and REMOVE operations can be cancelled
        return eventType == Type.SET || eventType == Type.REMOVE;
    }

    /**
     * Enum representing the types of configuration value operations.
     *
     * <ul>
     *   <li>{@link #SET} - A value was set or updated</li>
     *   <li>{@link #GET} - A value was read/accessed</li>
     *   <li>{@link #REMOVE} - A value was deleted</li>
     * </ul>
     */
    public enum Type {
        /** Configuration value was set or updated. */
        SET,
        /** Configuration value was read/accessed. */
        GET,
        /** Configuration value was deleted. */
        REMOVE
    }
}
