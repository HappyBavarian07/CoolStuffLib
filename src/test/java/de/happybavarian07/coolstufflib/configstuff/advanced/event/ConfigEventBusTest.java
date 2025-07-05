package de.happybavarian07.coolstufflib.configstuff.advanced.event;

import de.happybavarian07.coolstufflib.configstuff.advanced.AdvancedInMemoryConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEventBus.EventPriority;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConfigEventBusTest {

    private ConfigEventBus eventBus;
    private AdvancedConfig config;

    @BeforeEach
    void setUp() {
        eventBus = new ConfigEventBus();
        config = new AdvancedInMemoryConfig("test");
    }

    @AfterEach
    void tearDown() {
        eventBus.shutdown();
    }

    @Test
    void testBasicSubscribeAndPublish() {
        AtomicInteger callCount = new AtomicInteger(0);

        eventBus.subscribe(ConfigLifecycleEvent.class, event -> {
            callCount.incrementAndGet();
        });

        ConfigLifecycleEvent event = ConfigLifecycleEvent.configLoad(config);
        eventBus.publish(event);

        assertEquals(1, callCount.get());
    }

    @Test
    void testUnsubscribe() {
        AtomicInteger callCount = new AtomicInteger(0);

        ConfigEventListener<ConfigLifecycleEvent> listener = event -> {
            callCount.incrementAndGet();
        };

        eventBus.subscribe(ConfigLifecycleEvent.class, listener);

        // First publish should trigger listener
        ConfigLifecycleEvent event1 = ConfigLifecycleEvent.configLoad(config);
        eventBus.publish(event1);
        assertEquals(1, callCount.get());

        // Unsubscribe the listener
        eventBus.unsubscribe(ConfigLifecycleEvent.class, listener);

        // Second publish should not trigger the listener
        ConfigLifecycleEvent event2 = ConfigLifecycleEvent.configSave(config);
        eventBus.publish(event2);
        assertEquals(1, callCount.get()); // Count should remain unchanged
    }

    @Test
    void testEventPriority() {
        List<String> executionOrder = new ArrayList<>();

        // Register listeners with different priorities
        eventBus.subscribe(ConfigLifecycleEvent.class, event -> {
            executionOrder.add("LOWEST");
        }, EventPriority.LOWEST);

        eventBus.subscribe(ConfigLifecycleEvent.class, event -> {
            executionOrder.add("NORMAL");
        }, EventPriority.NORMAL);

        eventBus.subscribe(ConfigLifecycleEvent.class, event -> {
            executionOrder.add("HIGHEST");
        }, EventPriority.HIGHEST);

        // Publish an event
        ConfigLifecycleEvent event = ConfigLifecycleEvent.configLoad(config);
        eventBus.publish(event);

        // Verify execution order (highest priority first)
        assertEquals(3, executionOrder.size());
        assertEquals("HIGHEST", executionOrder.get(0));
        assertEquals("NORMAL", executionOrder.get(1));
        assertEquals("LOWEST", executionOrder.get(2));
    }

    @Test
    void testEventCancellation() {
        List<String> executedListeners = new ArrayList<>();

        // First listener - will cancel the event
        eventBus.subscribe(ConfigChangeEvent.class, event -> {
            executedListeners.add("listener1");
            event.setCancelled(true);
        }, EventPriority.HIGHEST);

        // Second listener - should not be called due to cancellation
        eventBus.subscribe(ConfigChangeEvent.class, event -> {
            executedListeners.add("listener2");
        }, EventPriority.NORMAL);

        // Third listener - should not be called due to cancellation
        eventBus.subscribe(ConfigChangeEvent.class, event -> {
            executedListeners.add("listener3");
        }, EventPriority.LOWEST);

        // Create and publish a cancellable event
        ConfigChangeEvent event = new ConfigChangeEvent(config, ConfigChangeEvent.ChangeType.VALUE_CHANGE, "testKey", "oldValue", "newValue");
        eventBus.publish(event);

        // Verify only the first listener executed before cancellation
        assertEquals(1, executedListeners.size());
        assertEquals("listener1", executedListeners.get(0));
        assertTrue(event.isCancelled());
    }

    @Test
    void testAsyncEventHandling() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger callCount = new AtomicInteger(0);

        // Subscribe with async flag
        eventBus.subscribe(ConfigLifecycleEvent.class, event -> {
            try {
                // Simulate work that takes time
                Thread.sleep(100);
                callCount.incrementAndGet();
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, EventPriority.NORMAL, true);

        // Publish event
        ConfigLifecycleEvent event = ConfigLifecycleEvent.configLoad(config);
        eventBus.publishAsync(event);

        // Control should return immediately, count should be 0
        assertEquals(0, callCount.get());

        // Wait for async processing to complete
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, callCount.get());
    }

    @Test
    void testEventTypeHierarchy() {
        AtomicInteger baseEventCount = new AtomicInteger(0);
        AtomicInteger specificEventCount = new AtomicInteger(0);

        // Subscribe to the base event type
        eventBus.subscribe(ConfigEvent.class, event -> {
            baseEventCount.incrementAndGet();
        });

        // Subscribe to a specific event type
        eventBus.subscribe(ConfigLifecycleEvent.class, event -> {
            specificEventCount.incrementAndGet();
        });

        // Publish specific event type
        ConfigLifecycleEvent lifecycleEvent = ConfigLifecycleEvent.configLoad(config);
        eventBus.publish(lifecycleEvent);

        // Both listeners should be called
        assertEquals(1, baseEventCount.get());
        assertEquals(1, specificEventCount.get());

        // Publish change event
        ConfigChangeEvent changeEvent = new ConfigChangeEvent(config, ConfigChangeEvent.ChangeType.VALUE_CHANGE, "key", "old", "new");
        eventBus.publish(changeEvent);

        // Only base listener should be called
        assertEquals(2, baseEventCount.get());
        assertEquals(1, specificEventCount.get());
    }

    @Test
    void testEventFiltering() {
        AtomicInteger valueChangeCount = new AtomicInteger(0);
        AtomicInteger sectionChangeCount = new AtomicInteger(0);

        // Subscribe with filtering
        eventBus.subscribe(ConfigChangeEvent.class, event -> {
            if (event.getChangeType() == ConfigChangeEvent.ChangeType.VALUE_CHANGE) {
                valueChangeCount.incrementAndGet();
            }
        });

        eventBus.subscribe(ConfigChangeEvent.class, event -> {
            if (event.getChangeType() == ConfigChangeEvent.ChangeType.SECTION_CHANGE) {
                sectionChangeCount.incrementAndGet();
            }
        });

        // Publish value change event
        ConfigChangeEvent valueEvent = new ConfigChangeEvent(config, ConfigChangeEvent.ChangeType.VALUE_CHANGE, "key", "old", "new");
        eventBus.publish(valueEvent);

        // Publish section change event
        ConfigChangeEvent sectionEvent = new ConfigChangeEvent(config, ConfigChangeEvent.ChangeType.SECTION_CHANGE, "section", null, null);
        eventBus.publish(sectionEvent);

        // Verify correct filtering
        assertEquals(1, valueChangeCount.get());
        assertEquals(1, sectionChangeCount.get());
    }

    @Test
    void testBulkOperations() {
        AtomicInteger callCount = new AtomicInteger(0);
        List<ConfigEventListener<ConfigEvent>> listeners = new ArrayList<>();

        // Create and register multiple listeners
        for (int i = 0; i < 5; i++) {
            ConfigEventListener<ConfigEvent> listener = event -> callCount.incrementAndGet();
            listeners.add(listener);
            eventBus.subscribe(ConfigEvent.class, listener);
        }

        // Publish an event
        ConfigLifecycleEvent event = ConfigLifecycleEvent.configLoad(config);
        eventBus.publish(event);

        // All listeners should be called
        assertEquals(5, callCount.get());

        // Unsubscribe all listeners at once
        eventBus.unsubscribeAll(ConfigEvent.class);

        // Reset call count
        callCount.set(0);

        // Publish another event
        ConfigLifecycleEvent event2 = ConfigLifecycleEvent.configReload(config);
        eventBus.publish(event2);

        // No listeners should be called
        assertEquals(0, callCount.get());
    }
}
