package de.happybavarian07.coolstufflib.service.debug;

import de.happybavarian07.coolstufflib.service.api.Service;
import de.happybavarian07.coolstufflib.service.api.ServiceDescriptor;
import de.happybavarian07.coolstufflib.service.api.ServiceRegistry;
import de.happybavarian07.coolstufflib.service.exception.ServiceDependencyCycleException;
import de.happybavarian07.coolstufflib.service.impl.DefaultServiceRegistry;
import de.happybavarian07.coolstufflib.service.impl.WorldServiceRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class DebugServiceTest {
    @Test
    public void testAnnotatedRegistrationAndDI() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        DebugConfig config = new DebugConfig();
        registry.registerAnnotatedServices("de.happybavarian07.coolstufflib.service.debug", config);
        Service serviceA = registry.get(registry.getIdByName("serviceA")).orElse(null);
        Service serviceB = registry.get(registry.getIdByName("serviceB")).orElse(null);
        System.out.println("Registered services: " + registry.snapshotStates());
        assertNotNull(serviceA);
        assertNotNull(serviceB);
        assertInstanceOf(DebugConfig.class, ((DebugServiceA) serviceA).getConfig());
        assertInstanceOf(DebugServiceA.class, ((DebugServiceB) serviceB).getServiceA());
    }

    @Test
    public void testFactoryLazyInit() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        UUID idA = UUID.randomUUID();
        ServiceDescriptor desc = new ServiceDescriptor(idA, "serviceA", List.of(), Duration.ofSeconds(5), Duration.ofSeconds(5));
        registry.registerFactory(desc, new DebugServiceFactory(), idA);
        Service serviceA = registry.get(idA).orElse(null);
        assertNotNull(serviceA);
        assertInstanceOf(DebugConfig.class, ((DebugServiceA) serviceA).getConfig());
    }

    @Test
    public void testHealthCheckAndLifecycle() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        DebugConfig config = new DebugConfig();
        registry.registerAnnotatedServices("de.happybavarian07.coolstufflib.service.debug", config);
        registry.registerHealthCheck(registry.getIdByName("serviceA"), () -> CompletableFuture.completedFuture(true));
        CompletableFuture<Boolean> healthy = registry.isHealthy(registry.getIdByName("serviceA"));
        assertTrue(healthy.join());
        registry.startAll().join();
        registry.stopAll().join();
        registry.reloadAll().join();
    }

    @Test
    public void testSetterInjectionConfig() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        DebugConfig config = new DebugConfig();
        registry.registerAnnotatedServices("de.happybavarian07.coolstufflib.service.debug", config);
        DebugServiceA serviceA = (DebugServiceA) registry.get(registry.getIdByName("serviceA")).orElse(null);
        assertNotNull(serviceA);
        assertNotNull(serviceA.getConfig());
        assertInstanceOf(DebugConfig.class, serviceA.getConfig());
    }

    @Test
    public void testSetterInjectionServiceDependency() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        DebugConfig config = new DebugConfig();
        registry.registerAnnotatedServices("de.happybavarian07.coolstufflib.service.debug", config);
        DebugServiceB serviceB = (DebugServiceB) registry.get(registry.getIdByName("serviceB")).orElse(null);
        assertNotNull(serviceB);
        assertNotNull(serviceB.getServiceA());
        assertInstanceOf(DebugServiceA.class, serviceB.getServiceA());
    }

    @Test
    public void testMetricsStartupAndReload() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        DebugConfig config = new DebugConfig();
        registry.registerAnnotatedServices("de.happybavarian07.coolstufflib.service.debug", config);
        registry.startAll().join();
        long startupTime = registry.getStartupTime(registry.getIdByName("serviceA"));
        assertTrue(startupTime >= 0);
        registry.reloadAll().join();
        long reloadTime = registry.getReloadTime(registry.getIdByName("serviceA"));
        assertTrue(reloadTime >= 0);
    }

    @Test
    public void testScopedRegistryIsolation() {
        ServiceRegistry worldRegistry1 = new WorldServiceRegistry("world1");
        ServiceRegistry worldRegistry2 = new WorldServiceRegistry("world2");
        DebugConfig config = new DebugConfig();
        worldRegistry1.registerAnnotatedServices("de.happybavarian07.coolstufflib.service.debug", config);
        worldRegistry2.registerAnnotatedServices("de.happybavarian07.coolstufflib.service.debug", config);
        Service serviceA1 = worldRegistry1.getByName("serviceA").orElse(null);
        Service serviceA2 = worldRegistry2.getByName("serviceA").orElse(null);
        assertNotNull(serviceA1);
        assertNotNull(serviceA2);
        assertNotSame(serviceA1, serviceA2);
    }

    @Test
    public void testCycleDetection() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID();
        ServiceDescriptor descA = new ServiceDescriptor(idA, "A", List.of(idB), Duration.ofSeconds(5), Duration.ofSeconds(5));
        ServiceDescriptor descB = new ServiceDescriptor(idB, "B", List.of(idA), Duration.ofSeconds(5), Duration.ofSeconds(5));
        registry.register(descA, new DebugServiceA(), idA);
        assertThrows(ServiceDependencyCycleException.class, () -> registry.register(descB, new DebugServiceA(), idB));
    }

    @Test
    public void testSetterInjectionConfig_direct() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        DebugServiceA serviceA = new DebugServiceA();
        serviceA.setConfig(new DebugConfig());
        UUID idA = UUID.randomUUID();
        ServiceDescriptor descA = new ServiceDescriptor(idA, "serviceA", List.of(), Duration.ofSeconds(5), Duration.ofSeconds(5));
        registry.register(descA, serviceA, idA);
        DebugServiceA result = (DebugServiceA) registry.get(idA).orElse(null);
        assertNotNull(result);
        assertNotNull(result.getConfig());
        assertInstanceOf(DebugConfig.class, result.getConfig());
    }

    @Test
    public void testSetterInjectionServiceDependency_direct() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        DebugServiceA serviceA = new DebugServiceA();
        serviceA.setConfig(new DebugConfig());
        DebugServiceB serviceB = new DebugServiceB(serviceA);
        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID();
        ServiceDescriptor descA = new ServiceDescriptor(idA, "serviceA", List.of(), Duration.ofSeconds(5), Duration.ofSeconds(5));
        ServiceDescriptor descB = new ServiceDescriptor(idB, "serviceB", List.of(idA), Duration.ofSeconds(5), Duration.ofSeconds(5));
        registry.register(descA, serviceA, idA);
        registry.register(descB, serviceB, idB);
        DebugServiceB result = (DebugServiceB) registry.get(idB).orElse(null);
        assertNotNull(result);
        assertNotNull(result.getServiceA());
        assertInstanceOf(DebugServiceA.class, result.getServiceA());
    }
}
