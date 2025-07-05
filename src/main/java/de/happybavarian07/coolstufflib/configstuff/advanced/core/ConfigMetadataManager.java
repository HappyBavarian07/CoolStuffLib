package de.happybavarian07.coolstufflib.configstuff.advanced.core;

import de.happybavarian07.coolstufflib.configstuff.advanced.BaseAdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEventBus;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigMetadataEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Thread-safe manager for configuration metadata that provides storage, retrieval,
 * and event-driven operations for arbitrary key-value metadata associated with
 * configuration instances.</p>
 *
 * <p>This manager provides:</p>
 * <ul>
 * <li>Thread-safe metadata storage with read-write locking</li>
 * <li>Event-driven metadata change notifications</li>
 * <li>Key-based metadata association and retrieval</li>
 * <li>Integration with configuration event systems</li>
 * </ul>
 *
 * <pre><code>
 * ConfigMetadataManager manager = new ConfigMetadataManager(lockManager, eventBus);
 * manager.addMetadata("author", "John Doe");
 * String author = (String) manager.getMetadata("author");
 * </code></pre>
 */
public class ConfigMetadataManager {
    private final Map<String, Object> metadata = new HashMap<>();
    private final ConfigLockManager lockManager;
    private final ConfigEventBus eventBus;

    public ConfigMetadataManager(ConfigLockManager lockManager, ConfigEventBus eventBus) {
        this.lockManager = lockManager;
        this.eventBus = eventBus;
    }

    /**
     * <p>Checks whether metadata exists for the specified key.</p>
     *
     * <pre><code>
     * if (manager.hasMetadata("version")) {
     *     Object version = manager.getMetadata("version");
     *     // Process the version metadata
     * }
     * </code></pre>
     *
     * @param key the metadata key to check for existence
     * @return true if metadata exists for this key, false otherwise
     */
    public boolean hasMetadata(String key) {
        lockManager.lockValuesRead();
        try {
            return metadata.containsKey(key);
        } finally {
            lockManager.unlockValuesRead();
        }
    }

    /**
     * <p>Removes metadata associated with the specified key.
     * No effect if no metadata exists for the given key.</p>
     *
     * <pre><code>
     * manager.removeMetadata("deprecated-setting");
     * // Metadata is now removed for this key
     * </code></pre>
     *
     * @param key the metadata key to remove
     */
    public void removeMetadata(String key) {
        lockManager.lockValuesWrite();
        try {
            metadata.remove(key);
        } finally {
            lockManager.unlockValuesWrite();
        }
    }

    /**
     * <p>Retrieves the metadata value associated with the specified key.</p>
     *
     * <pre><code>
     * String description = (String) manager.getMetadata("description");
     * if (description != null) {
     *     // Process the description metadata
     * }
     * </code></pre>
     *
     * @param key the metadata key to retrieve the value for
     * @param <T> the expected type of the metadata value
     * @return the metadata value, or null if no metadata exists for this key
     */
    public <T> T getMetadata(String key) {
        lockManager.lockValuesRead();
        try {
            return (T) metadata.get(key);
        } finally {
            lockManager.unlockValuesRead();
        }
    }

    /**
     * <p>Adds or updates metadata for the specified key and publishes a metadata event.
     * If the value is null, the metadata entry will be removed.</p>
     *
     * <pre><code>
     * manager.addMetadata("lastModified", System.currentTimeMillis());
     * manager.addMetadata("author", "Jane Smith");
     * </code></pre>
     *
     * @param key   the metadata key to associate the value with
     * @param value the metadata value to store, or null to remove the entry
     * @param config the configuration instance associated with this metadata
     */
    public void addMetadata(String key, Object value, AdvancedConfig config) {
        lockManager.lockValuesWrite();
        try {
            if (value == null) {
                metadata.remove(key);
            } else {
                metadata.put(key, value);
            }
            eventBus.publish(ConfigMetadataEvent.metadataAdded((BaseAdvancedConfig) config, key, value));
        } finally {
            lockManager.unlockValuesWrite();
        }
    }

    public Map<String, Object> getMetadata() {
        lockManager.lockValuesRead();
        try {
            return new HashMap<>(metadata);
        } finally {
            lockManager.unlockValuesRead();
        }
    }
}
