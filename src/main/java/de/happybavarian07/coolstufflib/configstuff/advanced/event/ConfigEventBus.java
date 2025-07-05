package de.happybavarian07.coolstufflib.configstuff.advanced.event;

import de.happybavarian07.coolstufflib.logging.ConfigLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

/**
 * <p>Thread-safe event bus system for configuration events that provides synchronous
 * and asynchronous event publishing with priority-based listener management and
 * advanced filtering capabilities.</p>
 *
 * <p>This event bus provides:</p>
 * <ul>
 * <li>Priority-based event listener management</li>
 * <li>Synchronous and asynchronous event publishing</li>
 * <li>Thread-safe concurrent listener registration</li>
 * <li>Advanced event filtering and conditional subscriptions</li>
 * <li>Automatic cleanup and shutdown capabilities</li>
 * </ul>
 *
 * <pre><code>
 * ConfigEventBus eventBus = new ConfigEventBus();
 * eventBus.subscribe(ConfigValueEvent.class, event ->
 *     System.out.println("Value changed: " + event.getPath()));
 * eventBus.publish(new ConfigValueEvent("test.key", "value"));
 * </code></pre>
 */
public class ConfigEventBus {
    private final Map<Class<? extends ConfigEvent>, Map<Integer, List<ListenerEntry<?>>>> listeners;
    private final ExecutorService asyncExecutor;
    private boolean asyncEnabled = false;

    public ConfigEventBus() {
        this.listeners = new ConcurrentHashMap<>();
        this.asyncExecutor = Executors.newCachedThreadPool();
    }

    /**
     * <p>Subscribes a listener to the specified event type with normal priority
     * and synchronous execution.</p>
     *
     * <pre><code>
     * eventBus.subscribe(ConfigValueEvent.class, event -> {
     *     // Handle value change event
     * });
     * </code></pre>
     *
     * @param eventType the class of events to listen for
     * @param listener the listener to invoke when events occur
     * @param <T> the event type parameter
     */
    public <T extends ConfigEvent> void subscribe(Class<T> eventType, ConfigEventListener<T> listener) {
        subscribe(eventType, listener, EventPriority.NORMAL, false);
    }

    /**
     * <p>Subscribes a listener to the specified event type with custom priority
     * and synchronous execution. Higher priority listeners execute first.</p>
     *
     * <pre><code>
     * eventBus.subscribe(ConfigLifecycleEvent.class, listener, EventPriority.HIGH);
     * </code></pre>
     *
     * @param eventType the class of events to listen for
     * @param listener the listener to invoke when events occur
     * @param priority the execution priority (higher values execute first)
     * @param <T> the event type parameter
     */
    public <T extends ConfigEvent> void subscribe(Class<T> eventType, ConfigEventListener<T> listener, int priority) {
        subscribe(eventType, listener, priority, false);
    }

    /**
     * <p>Subscribes a listener to the specified event type with custom priority
     * and execution mode (synchronous or asynchronous).</p>
     *
     * <pre><code>
     * eventBus.subscribe(ConfigModuleEvent.class, listener, EventPriority.LOW, true);
     * // Listener will execute asynchronously
     * </code></pre>
     *
     * @param eventType the class of events to listen for
     * @param listener the listener to invoke when events occur
     * @param priority the execution priority (higher values execute first)
     * @param async whether to execute the listener asynchronously
     * @param <T> the event type parameter
     */
    public <T extends ConfigEvent> void subscribe(Class<T> eventType, ConfigEventListener<T> listener, int priority, boolean async) {
        listeners.computeIfAbsent(eventType, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(priority, k -> new CopyOnWriteArrayList<>())
                .add(new ListenerEntry<>(listener, async));
    }

    /**
     * <p>Unsubscribes a specific listener from the specified event type.
     * The listener will no longer receive events of this type.</p>
     *
     * <pre><code>
     * eventBus.unsubscribe(ConfigValueEvent.class, myListener);
     * </code></pre>
     *
     * @param eventType the class of events to stop listening for
     * @param listener the listener to remove
     * @param <T> the event type parameter
     */
    public <T extends ConfigEvent> void unsubscribe(Class<T> eventType, ConfigEventListener<T> listener) {
        if (listeners.containsKey(eventType)) {
            listeners.get(eventType).values().forEach(listenerList ->
                    listenerList.removeIf(entry -> entry.listener().equals(listener)));
        }
    }

    /**
     * <p>Unsubscribes all listeners from the specified event type.
     * No listeners will receive events of this type after this call.</p>
     *
     * <pre><code>
     * eventBus.unsubscribeAll(ConfigMetadataEvent.class);
     * </code></pre>
     *
     * @param eventType the class of events to remove all listeners for
     * @param <T> the event type parameter
     */
    public <T extends ConfigEvent> void unsubscribeAll(Class<T> eventType) {
        listeners.remove(eventType);
    }

    /**
     * <p>Unsubscribes all listeners from all event types, effectively
     * clearing the entire event bus.</p>
     *
     * <pre><code>
     * eventBus.unsubscribeAll();
     * // Event bus is now empty
     * </code></pre>
     */
    public void unsubscribeAll() {
        listeners.clear();
    }

    /**
     * <p>Publishes an event to all registered listeners synchronously or
     * asynchronously based on individual listener configuration.</p>
     *
     * <pre><code>
     * ConfigValueEvent event = new ConfigValueEvent("setting", "value");
     * eventBus.publish(event);
     * </code></pre>
     *
     * @param event the event to publish to listeners
     */
    @SuppressWarnings("unchecked")
    public void publish(ConfigEvent event) {
        if (event == null) {
            return;
        }

        Class<? extends ConfigEvent> eventType = event.getClass();
        Map<Integer, List<ListenerEntry<?>>> eventListeners = new HashMap<>();

        Set<Class<?>> allTypes = getAllTypes(eventType);
        for (Class<?> type : allTypes) {
            if (ConfigEvent.class.isAssignableFrom(type) && listeners.containsKey(type)) {
                mergeListeners(eventListeners, listeners.get((Class<? extends ConfigEvent>) type));
            }
        }

        List<Integer> priorities = new ArrayList<>(eventListeners.keySet());
        priorities.sort(Collections.reverseOrder());

        for (Integer priority : priorities) {
            List<ListenerEntry<?>> priorityListeners = eventListeners.get(priority);
            if (priorityListeners == null) {
                continue;
            }

            for (ListenerEntry<?> entry : priorityListeners) {
                if (event.isCancelled() && !isAcceptingCancelled(entry.listener())) {
                    continue;
                }

                try {
                    ConfigEventListener<ConfigEvent> listener = (ConfigEventListener<ConfigEvent>) entry.listener();
                    if (entry.async()) {
                        executeAsync(listener, event);
                    } else {
                        listener.onEvent(event);
                    }
                } catch (Exception e) {
                    ConfigLogger.error("Error executing listener for event " + eventType.getSimpleName(), e, "ConfigEventBus", true);
                }

                if (event.isCancelled() && !isAcceptingCancelled(entry.listener())) {
                    break;
                }
            }
        }
    }

    /**
     * <p>Publishes an event asynchronously to all registered listeners,
     * regardless of individual listener async settings.</p>
     *
     * <pre><code>
     * eventBus.publishAsync(new ConfigLifecycleEvent("config.saved"));
     * // Returns immediately, listeners execute in background
     * </code></pre>
     *
     * @param event the event to publish asynchronously
     */
    public void publishAsync(ConfigEvent event) {
        asyncExecutor.submit(() -> publish(event));
    }

    private <T extends ConfigEvent> void executeAsync(ConfigEventListener<T> listener, T event) {
        asyncExecutor.submit(() -> {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                ConfigLogger.error("Error executing async listener for event " + event.getClass().getSimpleName(), e, "ConfigEventBus", true);
            }
        });
    }

    /**
     * <p>Sets the global asynchronous execution flag for this event bus instance.
     * Controls whether listeners can be executed asynchronously.</p>
     *
     * <pre><code>
     * eventBus.setAsyncEnabled(true);
     * </code></pre>
     *
     * @param asyncEnabled true to enable async execution, false to disable
     */
    public void setAsyncEnabled(boolean asyncEnabled) {
        this.asyncEnabled = asyncEnabled;
    }

    /**
     * <p>Checks whether asynchronous event publishing is globally enabled
     * for this event bus instance.</p>
     *
     * <pre><code>
     * if (eventBus.isAsyncEnabled()) {
     *     // Async publishing is available
     * }
     * </code></pre>
     *
     * @return true if async publishing is enabled, false otherwise
     */
    public boolean isAsyncEnabled() {
        return asyncEnabled;
    }

    /**
     * <p>Shuts down the event bus and releases all resources including
     * the async executor service.</p>
     *
     * <pre><code>
     * eventBus.shutdown();
     * // Event bus can no longer be used
     * </code></pre>
     */
    public void shutdown() {
        asyncExecutor.shutdown();
    }

    private boolean isAcceptingCancelled(ConfigEventListener<?> listener) {
        return listener instanceof CancellableEventListener &&
               ((CancellableEventListener<?>) listener).acceptsCancelled();
    }

    private void mergeListeners(Map<Integer, List<ListenerEntry<?>>> target,
                               Map<Integer, List<ListenerEntry<?>>> source) {
        if (source == null) {
            return;
        }

        for (Map.Entry<Integer, List<ListenerEntry<?>>> entry : source.entrySet()) {
            target.computeIfAbsent(entry.getKey(), k -> new CopyOnWriteArrayList<>())
                  .addAll(entry.getValue());
        }
    }

    private Set<Class<?>> getAllInterfaces(Class<?> cls) {
        Set<Class<?>> interfaces = new HashSet<>();
        getAllInterfaces(cls, interfaces);
        return interfaces;
    }

    private void getAllInterfaces(Class<?> cls, Set<Class<?>> interfaces) {
        if (cls == null || cls == Object.class) {
            return;
        }

        for (Class<?> iface : cls.getInterfaces()) {
            interfaces.add(iface);
            getAllInterfaces(iface, interfaces);
        }

        getAllInterfaces(cls.getSuperclass(), interfaces);
    }

    private Set<Class<?>> getAllTypes(Class<?> cls) {
        Set<Class<?>> types = new HashSet<>();
        while (cls != null && cls != Object.class) {
            types.add(cls);
            types.addAll(getAllInterfaces(cls));
            cls = cls.getSuperclass();
        }
        return types;
    }

    public interface CancellableEventListener<T extends ConfigEvent> extends ConfigEventListener<T> {
        boolean acceptsCancelled();
    }

    public static class EventPriority {
        public static final int LOWEST = 100;
        public static final int LOW = 200;
        public static final int NORMAL = 300;
        public static final int HIGH = 400;
        public static final int HIGHEST = 500;
        public static final int MONITOR = 600;
    }

    /**
     * <p>Creates a filtered subscription builder that allows events to be
     * filtered before reaching the listener.</p>
     *
     * <pre><code>
     * eventBus.filter(ConfigValueEvent.class)
     *     .withCondition(event -> event.getPath().startsWith("database"))
     *     .subscribe(listener);
     * </code></pre>
     *
     * @param eventType the class of events to filter
     * @param <T> the event type parameter
     * @return a subscription builder for filtered events
     */
    public <T extends ConfigEvent> EventFilter<T> filter(Class<T> eventType) {
        return new EventFilter<>(this, eventType);
    }

    /**
     * <p>Checks if a specific listener registration is configured for
     * asynchronous execution.</p>
     *
     * <pre><code>
     * boolean async = eventBus.isAsync(ConfigValueEvent.class, myListener);
     * </code></pre>
     *
     * @param eventType the event type to check
     * @param listener the listener to check
     * @param <T> the event type parameter
     * @return true if the listener is configured for async execution
     */
    public <T extends ConfigEvent> boolean isAsync(Class<T> eventType, ConfigEventListener<T> listener) {
        if (!listeners.containsKey(eventType)) {
            return false;
        }

        return listeners.get(eventType).values().stream()
                .flatMap(List::stream)
                .anyMatch(entry -> entry.listener().equals(listener) && entry.async());
    }

    private record ListenerEntry<T extends ConfigEvent>(ConfigEventListener<T> listener, boolean async) {
    }

    public static class EventFilter<T extends ConfigEvent> {
        private final ConfigEventBus eventBus;
        private final Class<T> eventType;
        private Predicate<T> condition;
        private int priority = EventPriority.NORMAL;
        private boolean async = false;

        private EventFilter(ConfigEventBus eventBus, Class<T> eventType) {
            this.eventBus = eventBus;
            this.eventType = eventType;
            this.condition = event -> true;
        }

        public EventFilter<T> withPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public EventFilter<T> withAsync(boolean async) {
            this.async = async;
            return this;
        }

        public EventFilter<T> withCondition(Predicate<T> condition) {
            this.condition = this.condition.and(condition);
            return this;
        }

        public void subscribe(ConfigEventListener<T> listener) {
            ConfigEventListener<T> wrappedListener = event -> {
                if (condition.test(event)) {
                    listener.onEvent(event);
                }
            };
            eventBus.subscribe(eventType, wrappedListener, priority, async);
        }
    }
}
