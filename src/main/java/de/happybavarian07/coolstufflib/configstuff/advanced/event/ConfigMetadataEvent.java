package de.happybavarian07.coolstufflib.configstuff.advanced.event;

import de.happybavarian07.coolstufflib.configstuff.advanced.BaseAdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;

/**
 * <p>Event fired when configuration metadata is modified.</p>
 *
 * <p>This event tracks changes to configuration-level metadata properties, such as
 * comments, headers, custom attributes, or any key-value pairs stored at the config level
 * rather than within sections. Unlike {@link ConfigValueEvent} which tracks individual
 * config values, this event handles metadata about the config itself.</p>
 *
 * <pre><code>
 * // Subscribe to metadata change events
 * eventBus.subscribe(ConfigMetadataEvent.class, event -> {
 *     if (event.getOperation() == MetadataOperation.SET) {
 *         System.out.println("Metadata '" + event.getKey() + "' set to: " + event.getValue());
 *     }
 * });
 *
 * // Triggering the event
 * config.setMetadata("author", "John Doe");
 * </code></pre>
 *
 * <p>Operations:</p>
 * <ul>
 *   <li>{@link MetadataOperation#SET} - A metadata value was set or updated</li>
 *   <li>{@link MetadataOperation#REMOVE} - A metadata entry was deleted</li>
 *   <li>{@link MetadataOperation#CLEAR} - All metadata was cleared</li>
 * </ul>
 *
 * @see ConfigEventBus
 * @see ConfigEventListener
 */
public class ConfigMetadataEvent extends ConfigEvent {
    private final MetadataOperation operation;
    private final String key;
    private final Object value;
    private final Object oldValue;

    /**
     * Creates a metadata event for SET operations.
     *
     * <pre><code>
 * ConfigMetadataEvent event = new ConfigMetadataEvent(config, MetadataOperation.SET, "author", "John");
 * </code></pre>
     *
     * @param config the configuration containing the metadata
     * @param operation the type of metadata operation (SET)
     * @param key the metadata key being modified
     * @param value the new metadata value
     */
    public ConfigMetadataEvent(AdvancedConfig config, MetadataOperation operation, String key, Object value) {
        super(config);
        this.operation = operation;
        this.key = key;
        this.value = value;
        this.oldValue = null;
    }

    /**
     * Creates a metadata event with both old and new values (for update operations).
     *
     * <pre><code>
 * ConfigMetadataEvent event = new ConfigMetadataEvent(config, MetadataOperation.SET,
 *     "version", "2.0", "1.0");
 * </code></pre>
     *
     * @param config the configuration containing the metadata
     * @param operation the type of metadata operation
     * @param key the metadata key being modified
     * @param value the new metadata value
     * @param oldValue the previous metadata value (for comparison)
     */
    public ConfigMetadataEvent(AdvancedConfig config, MetadataOperation operation, String key, Object value, Object oldValue) {
        super(config);
        this.operation = operation;
        this.key = key;
        this.value = value;
        this.oldValue = oldValue;
    }

    /**
     * Creates a metadata event indicating that a new metadata entry was added.
     *
     * <pre><code>
 * ConfigEvent event = ConfigMetadataEvent.metadataAdded(config, "author", "John Doe");
 * eventBus.publish(event);
 * </code></pre>
     *
     * @param config the configuration containing the metadata
     * @param name the metadata key being added
     * @param value the metadata value being set
     * @param <T> the type of the metadata value
     * @return a ConfigMetadataEvent with operation SET
     */
    public static <T> ConfigEvent metadataAdded(BaseAdvancedConfig config, String name, T value) {
        return new ConfigMetadataEvent(config, MetadataOperation.SET, name, value);
    }

    /**
     * Gets the type of operation that triggered this event.
     *
     * @return the MetadataOperation indicating what action was performed
     */
    public MetadataOperation getOperation() {
        return operation;
    }

    /**
     * Gets the metadata key affected by this event.
     *
     * <pre><code>
 * String key = event.getKey(); // Returns "author"
 * </code></pre>
     *
     * @return the metadata key name; returns empty string for CLEAR operations
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the new metadata value after the operation.
     *
     * <p>Returns {@code null} for REMOVE and CLEAR operations.</p>
     *
     * @return the metadata value, or {@code null} if not applicable
     */
    public Object getValue() {
        return value;
    }

    /**
     * Gets the previous metadata value before the operation.
     *
     * <p>Returns {@code null} for SET operations on new keys.</p>
     *
     * @return the old metadata value, or {@code null} if not applicable
     */
    public Object getOldValue() {
        return oldValue;
    }

    /**
     * Checks if this event can be cancelled.
     *
     * <p>All metadata operations are cancellable by default.</p>
     *
     * @return {@code true} - metadata events can always be cancelled
     */
    @Override
    public boolean isCancellable() {
        return true;
    }

    /**
     * Enum representing the types of metadata operations.
     *
     * <ul>
     *   <li>{@link #SET} - A metadata value was set or updated</li>
     *   <li>{@link #REMOVE} - A specific metadata entry was deleted</li>
     *   <li>{@link #CLEAR} - All metadata entries were cleared</li>
     * </ul>
     */
    public enum MetadataOperation {
        /** A metadata value was set or updated. Value is present, oldValue may be null. */
        SET,
        /** A specific metadata entry was deleted. Value is null, oldValue contains deleted value. */
        REMOVE,
        /** All metadata entries were cleared. Both value and oldValue are null; key is empty. */
        CLEAR
    }
}
