package de.happybavarian07.coolstufflib.configstuff.advanced.event;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;

/**
 * <p>Base class for all configuration-related events in the configuration system.</p>
 *
 * <p>ConfigEvent provides common functionality for events such as access to the related
 * configuration, timestamps, and cancellation support.</p>
 *
 * <pre><code>
 * ConfigEvent event = new ConfigChangeEvent(config, ChangeType.VALUE_CHANGE, "server.port", 8080, 9090);
 * eventBus.publish(event);
 * </code></pre>
 */
public class ConfigEvent {
    private final AdvancedConfig config;
    private final long timestamp;
    private boolean cancelled;

    /**
     * <p>Creates a new configuration event.</p>
     *
     * @param config The configuration associated with this event
     */
    public ConfigEvent(AdvancedConfig config) {
        this.config = config;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * <p>Gets the source object of this event.</p>
     *
     * @return The configuration object that is the source of this event
     */
    public Object getSource() {
        return config;
    }

    /**
     * <p>Gets the configuration associated with this event.</p>
     *
     * @return The configuration object
     */
    public AdvancedConfig getConfig() {
        return config;
    }

    /**
     * <p>Gets the timestamp when this event was created.</p>
     *
     * @return The event timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * <p>Checks if this event can be cancelled.</p>
     *
     * <p>By default, events are not cancellable. Subclasses can override
     * this method to make events cancellable.</p>
     *
     * @return True if the event can be cancelled, false otherwise
     */
    public boolean isCancellable() {
        return false;
    }

    /**
     * <p>Checks if this event has been cancelled.</p>
     *
     * @return True if the event has been cancelled, false otherwise
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * <p>Sets the cancelled state of this event.</p>
     *
     * <p>If the event is not cancellable, this method has no effect.</p>
     *
     * @param cancelled True to cancel the event, false otherwise
     */
    public void setCancelled(boolean cancelled) {
        if (isCancellable()) {
            this.cancelled = cancelled;
        }
    }

    /**
     * <p>Gets the name of the configuration associated with this event.</p>
     *
     * @return The configuration name, or "unknown" if no configuration is available
     */
    public String getConfigName() {
        return config != null ? config.getName() : "unknown";
    }
}
