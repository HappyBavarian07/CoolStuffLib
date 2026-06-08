package de.happybavarian07.coolstufflib.configstuff.advanced.event;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;

/**
 * <p>Event fired when a configuration change occurs (value or section modification).</p>
 *
 * <p>This event provides information about configuration changes including the affected path,
 * old and new values, and the type of change. Unlike {@link ConfigValueEvent}, this event
 * can represent both value changes and section-level operations.</p>
 *
 * <pre><code>
 * // Subscribe to config change events
 * eventBus.subscribe(ConfigChangeEvent.class, event -> {
 *     if (event.getChangeType() == ChangeType.VALUE_CHANGE) {
 *         System.out.println("Changed: " + event.getPath());
 *         System.out.println("From: " + event.getOldValue() + ", To: " + event.getNewValue());
 *     }
 * });
 *
 * // Triggering the event
 * ConfigChangeEvent change = new ConfigChangeEvent(config, ChangeType.VALUE_CHANGE, 
 *     "server.port", 8080, 9090);
 * eventBus.publish(change);
 * </code></pre>
 *
 * <p>Change types:</p>
 * <ul>
 *   <li>{@link ChangeType#VALUE_CHANGE} - A configuration value was modified</li>
 *   <li>{@link ChangeType#SECTION_CHANGE} - A section was modified (metadata, etc.)</li>
 *   <li>{@link ChangeType#SECTION_CREATION} - A new section was created</li>
 *   <li>{@link ChangeType#SECTION_REMOVAL} - An existing section was deleted</li>
 * </ul>
 *
 * @see ConfigEventBus
 * @see ConfigEventListener
 */
public class ConfigChangeEvent extends ConfigEvent {
    private final ChangeType changeType;
    private final String path;
    private final Object oldValue;
    private final Object newValue;

    /**
     * Creates a new configuration change event.
     *
     * <pre><code>
 * // Value change event
 * ConfigChangeEvent event = new ConfigChangeEvent(config, ChangeType.VALUE_CHANGE,
 *     "database.pool-size", 10, 20);
 * </code></pre>
     *
     * @param config the configuration containing the changed value/section
     * @param changeType the type of change that occurred
     * @param path the full path to the affected element (e.g., "database.pool-size")
     * @param oldValue the previous value, or {@code null} for creation events
     * @param newValue the new value, or {@code null} for removal events
     */
    public ConfigChangeEvent(AdvancedConfig config, ChangeType changeType, String path, Object oldValue, Object newValue) {
        super(config);
        this.changeType = changeType;
        this.path = path;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * Gets the type of change that occurred.
     *
     * @return the ChangeType indicating what kind of modification happened
     */
    public ChangeType getChangeType() {
        return changeType;
    }

    /**
     * Gets the full path to the affected configuration element.
     *
     * <pre><code>
 * String path = event.getPath(); // Returns "database.connection.url"
 * </code></pre>
     *
     * @return the hierarchical path using dot notation
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the previous value before the change.
     *
     * <p>Returns {@code null} for SECTION_CREATION events.</p>
     *
     * @return the old value, or {@code null} if not applicable
     */
    public Object getOldValue() {
        return oldValue;
    }

    /**
     * Gets the new value after the change.
     *
     * <p>Returns {@code null} for SECTION_REMOVAL events.</p>
     *
     * @return the new value, or {@code null} if not applicable
     */
    public Object getNewValue() {
        return newValue;
    }

    /**
     * Checks if this event can be cancelled.
     *
     * <p>All change events are cancellable by default.</p>
     *
     * @return {@code true} - change events can always be cancelled
     */
    @Override
    public boolean isCancellable() {
        return true;
    }

    /**
     * Enum representing the types of configuration changes.
     *
     * <ul>
     *   <li>{@link #VALUE_CHANGE} - A configuration value was modified</li>
     *   <li>{@link #SECTION_CHANGE} - A section metadata was modified</li>
     *   <li>{@link #SECTION_CREATION} - A new section was created</li>
     *   <li>{@link #SECTION_REMOVAL} - An existing section was deleted</li>
     * </ul>
     */
    public enum ChangeType {
        /** A configuration value was modified. Both oldValue and newValue are present. */
        VALUE_CHANGE,
        /** A section's metadata or properties were modified. */
        SECTION_CHANGE,
        /** A new section was created. oldValue is null, newValue contains the section. */
        SECTION_CREATION,
        /** An existing section was deleted. oldValue contains the section, newValue is null. */
        SECTION_REMOVAL
    }
}
