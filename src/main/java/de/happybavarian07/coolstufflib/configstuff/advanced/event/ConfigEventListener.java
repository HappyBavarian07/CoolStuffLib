package de.happybavarian07.coolstufflib.configstuff.advanced.event;

/**
 * <p>Functional interface for handling configuration events.</p>
 *
 * <p>This interface defines the contract for event listeners that respond to
 * configuration-related events published through the {@link ConfigEventBus}. Listeners
 * are invoked when matching events are published, allowing for reactive configuration
 * management and cross-module communication.</p>
 *
 * <pre><code>
 * // Basic usage: Subscribe a listener to value change events
 * eventBus.subscribe(ConfigValueEvent.class, new ConfigEventListener&lt;&gt;() {
 *     public void onEvent(ConfigValueEvent event) {
 *         System.out.println("Config changed: " + event.getPath());
 *         handleConfigChange(event.getNewValue());
 *     }
 * });
 * </code></pre>
 *
 * <p>Listeners can be subscribed with different priorities and execution modes:
 * <ul>
 *   <li><b>Synchronous:</b> Listener executes immediately during event publish</li>
 *   <li><b>Asynchronous:</b> Listener executes in background thread</li>
 * </ul>
 * </p>
 *
 * @param <T> the type of event this listener handles (must extend ConfigEvent)
 * @see ConfigEventBus
 * @see ConfigEvent
 */
@FunctionalInterface
public interface ConfigEventListener<T extends ConfigEvent> {
    /**
     * <p>Called when an event is published to the bus.</p>
     *
     * <pre><code>
 * // Example: Handle configuration value changes
 * public void onEvent(ConfigValueEvent event) {
 *     if ("database.url".equals(event.getPath())) {
 *         reconnectDatabase(event.getNewValue());
 *     }
 * }
 * </code></pre>
     *
     * <p>The event contains all relevant information about the configuration change,
     * including the affected path, old and new values, and metadata.</p>
     *
     * @param event the event that occurred. Never null.
     */
    void onEvent(T event);
}
