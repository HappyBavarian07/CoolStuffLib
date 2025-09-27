package de.happybavarian07.coolstufflib.service;

import de.happybavarian07.coolstufflib.service.api.Service;
import de.happybavarian07.coolstufflib.service.api.ServiceDescriptor;
import de.happybavarian07.coolstufflib.service.api.ServiceState;
import de.happybavarian07.coolstufflib.service.impl.DefaultServiceRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class DefaultServiceRegistryTest {
    @Test
    void testRegisterAndLookup() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        ServiceDescriptor desc = new ServiceDescriptor("test", List.of(), Duration.ofSeconds(1), Duration.ofSeconds(1));
        Service svc = new DummyService("test");
        registry.register(desc, svc);
        assertTrue(registry.get("test").isPresent());
        assertEquals(ServiceState.REGISTERED, registry.getState("test"));
    }

    @Test
    void testStartStopOrderWithDependencies() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        ServiceDescriptor aDesc = new ServiceDescriptor("A", List.of(), Duration.ofSeconds(1), Duration.ofSeconds(1));
        ServiceDescriptor bDesc = new ServiceDescriptor("B", List.of("A"), Duration.ofSeconds(1), Duration.ofSeconds(1));
        registry.register(aDesc, new DummyService("A"));
        registry.register(bDesc, new DummyService("B"));
        registry.startAll().join();
        assertEquals(ServiceState.RUNNING, registry.getState("A"));
        assertEquals(ServiceState.RUNNING, registry.getState("B"));
        registry.stopAll().join();
        assertEquals(ServiceState.STOPPED, registry.getState("A"));
        assertEquals(ServiceState.STOPPED, registry.getState("B"));
    }

    @Test
    void testCycleDetection() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        ServiceDescriptor aDesc = new ServiceDescriptor("A", List.of("B"), Duration.ofSeconds(1), Duration.ofSeconds(1));
        ServiceDescriptor bDesc = new ServiceDescriptor("B", List.of("A"), Duration.ofSeconds(1), Duration.ofSeconds(1));
        registry.register(aDesc, new DummyService("A"));
        registry.register(bDesc, new DummyService("B"));
        assertThrows(IllegalStateException.class, () -> registry.startAll().join());
    }

    @Test
    void testTimeout() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        ServiceDescriptor desc = new ServiceDescriptor("slow", List.of(), Duration.ofMillis(100), Duration.ofSeconds(1));
        Service svc = new Service() {
            public String id() {
                return "slow";
            }

            public CompletableFuture<Void> init() {
                return CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                });
            }

            public CompletableFuture<Void> shutdown() {
                return CompletableFuture.completedFuture(null);
            }
        };
        registry.register(desc, svc);
        assertThrows(CompletionException.class, () -> registry.startAll().join());
        assertEquals(ServiceState.FAILED, registry.getState("slow"));
    }

    @Test
    void testHealthCheck() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        ServiceDescriptor desc = new ServiceDescriptor("health", List.of(), Duration.ofSeconds(1), Duration.ofSeconds(1));
        Service svc = new DummyService("health");
        registry.register(desc, svc);
        AtomicBoolean healthy = new AtomicBoolean(true);
        registry.registerHealthCheck("health", () -> CompletableFuture.completedFuture(healthy.get()));
        assertTrue(registry.isHealthy("health").join());
        healthy.set(false);
        assertFalse(registry.isHealthy("health").join());
    }

    static class DummyService implements Service {
        private final String id;

        DummyService(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public CompletableFuture<Void> init() {
            return CompletableFuture.completedFuture(null);
        }

        public CompletableFuture<Void> shutdown() {
            return CompletableFuture.completedFuture(null);
        }
    }
}

