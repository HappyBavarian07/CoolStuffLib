package de.happybavarian07.coolstufflib.service;

import de.happybavarian07.coolstufflib.service.api.Service;
import de.happybavarian07.coolstufflib.service.api.ServiceDescriptor;
import de.happybavarian07.coolstufflib.service.api.ServiceState;
import de.happybavarian07.coolstufflib.service.exception.ServiceDependencyCycleException;
import de.happybavarian07.coolstufflib.service.impl.DefaultServiceRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class DefaultServiceRegistryTest {
    @Test
    void testRegisterAndLookup() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        UUID uuid = UUID.randomUUID();
        ServiceDescriptor desc = new ServiceDescriptor(uuid, "test", List.of(), Duration.ofSeconds(1), Duration.ofSeconds(1));
        Service svc = new DummyService("test");
        registry.register(desc, svc, uuid);
        assertTrue(registry.get(uuid).isPresent());
        assertEquals(ServiceState.REGISTERED, registry.getState(uuid));
    }

    @Test
    void testStartStopOrderWithDependencies() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        ServiceDescriptor aDesc = new ServiceDescriptor(aId, "A", List.of(), Duration.ofSeconds(1), Duration.ofSeconds(1));
        ServiceDescriptor bDesc = new ServiceDescriptor(bId, "B", List.of(aId), Duration.ofSeconds(1), Duration.ofSeconds(1));
        registry.register(aDesc, new DummyService("A"), aId);
        registry.register(bDesc, new DummyService("B"), bId);
        registry.startAll().join();
        assertEquals(ServiceState.RUNNING, registry.getState(aId));
        assertEquals(ServiceState.RUNNING, registry.getState(bId));
        registry.stopAll().join();
        assertEquals(ServiceState.STOPPED, registry.getState(aId));
        assertEquals(ServiceState.STOPPED, registry.getState(bId));
    }

    @Test
    void testCycleDetection() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        ServiceDescriptor aDesc = new ServiceDescriptor(aId, "A", List.of(bId), Duration.ofSeconds(1), Duration.ofSeconds(1));
        ServiceDescriptor bDesc = new ServiceDescriptor(bId, "B", List.of(aId), Duration.ofSeconds(1), Duration.ofSeconds(1));
        registry.register(aDesc, new DummyService("A"), aId);
        assertThrows(ServiceDependencyCycleException.class, () -> registry.register(bDesc, new DummyService("B"), bId));
    }

    @Test
    void testTimeout() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        UUID uuid = UUID.randomUUID();
        ServiceDescriptor desc = new ServiceDescriptor(uuid, "slow", List.of(), Duration.ofMillis(100), Duration.ofSeconds(1));
        Service svc = new Service() {
            private UUID id;
            private String name;

            @Override
            public UUID id() {
                return id;
            }

            public String serviceName() {
                return name;
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
        registry.register(desc, svc, uuid);
        assertThrows(CompletionException.class, () -> registry.startAll().join());
        assertEquals(ServiceState.FAILED, registry.getState(uuid));
    }

    @Test
    void testHealthCheck() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        UUID uuid = UUID.randomUUID();
        ServiceDescriptor desc = new ServiceDescriptor(uuid, "health", List.of(), Duration.ofSeconds(1), Duration.ofSeconds(1));
        Service svc = new DummyService("health");
        registry.register(desc, svc, uuid);
        AtomicBoolean healthy = new AtomicBoolean(true);
        registry.registerHealthCheck(uuid, () -> CompletableFuture.completedFuture(healthy.get()));
        assertTrue(registry.isHealthy(uuid).join());
        healthy.set(false);
        assertFalse(registry.isHealthy(uuid).join());
    }

    static class DummyService implements Service {
        private UUID id;
        private String name;

        DummyService(String name) {
            this.name = name;
        }

        @Override
        public UUID id() {
            return id;
        }

        public String serviceName() {
            return name;
        }

        public CompletableFuture<Void> init() {
            return CompletableFuture.completedFuture(null);
        }

        public CompletableFuture<Void> shutdown() {
            return CompletableFuture.completedFuture(null);
        }
    }
}
