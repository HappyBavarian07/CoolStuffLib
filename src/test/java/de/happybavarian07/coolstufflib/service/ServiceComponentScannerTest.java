package de.happybavarian07.coolstufflib.service;

import de.happybavarian07.coolstufflib.service.annotation.ServiceComponent;
import de.happybavarian07.coolstufflib.service.api.ServiceDescriptor;
import de.happybavarian07.coolstufflib.service.impl.DefaultServiceRegistry;
import de.happybavarian07.coolstufflib.service.scan.ScanBrokenSetterService;
import de.happybavarian07.coolstufflib.service.scan.ScanConfig;
import de.happybavarian07.coolstufflib.service.scan.ScanDependencyService;
import de.happybavarian07.coolstufflib.service.scan.ScanPinnedIdService;
import de.happybavarian07.coolstufflib.service.util.ServiceComponentScanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ServiceComponentScannerTest {
    private static final String SCAN_PACKAGE = "de.happybavarian07.coolstufflib.service.scan";

    private final List<LogRecord> logged = new ArrayList<>();
    private final Handler collector = new Handler() {
        @Override
        public void publish(LogRecord record) {
            logged.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    @BeforeEach
    void captureLibraryLog() {
        Logger.getLogger("CoolStuffLib").addHandler(collector);
    }

    @AfterEach
    void releaseLibraryLog() {
        Logger.getLogger("CoolStuffLib").removeHandler(collector);
    }

    @Test
    void failedSetterInjectionIsReportedWithServiceAndProperty() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        registry.registerAnnotatedServices(SCAN_PACKAGE, new ScanConfig());

        List<String> messages = logged.stream().map(LogRecord::getMessage).toList();
        assertTrue(messages.stream().anyMatch(message ->
                        message != null
                                && message.contains("setScanConfig")
                                && message.contains(ScanBrokenSetterService.class.getName())),
                "expected a report naming setScanConfig and the service class, got: " + messages);
        assertEquals(1, messages.stream().filter(message -> message != null
                && message.contains("setScanConfig")).count());
    }

    @Test
    void aFailingSetterDoesNotAbortTheRemainingProperties() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        registry.registerAnnotatedServices(SCAN_PACKAGE, new ScanConfig());

        ScanBrokenSetterService service = (ScanBrokenSetterService) registry.getByName("scanBrokenSetter").orElseThrow();
        assertNull(service.getConfig());
        assertNotNull(service.getDependency());
        assertInstanceOf(ScanDependencyService.class, service.getDependency());
    }

    @Test
    void anUnresolvedServiceSetterStaysQuiet() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        registry.registerAnnotatedServices(SCAN_PACKAGE, new ScanConfig());

        List<String> messages = logged.stream().map(LogRecord::getMessage).toList();
        assertTrue(messages.stream().noneMatch(message -> message != null
                        && message.contains("setScanDependencyService")),
                "a dependency that is simply not registered is not an injection failure, got: " + messages);
    }

    @Test
    void annotatedServiceIdIsDerivedFromTheServiceName() {
        for (Class<?> clazz : List.of(ScanDependencyService.class, ScanBrokenSetterService.class)) {
            ServiceComponent meta = clazz.getAnnotation(ServiceComponent.class);
            assertEquals("", meta.uuid());
            assertEquals(ServiceDescriptor.of(meta.serviceName()).id(), ServiceComponentScanner.resolveServiceId(meta));
        }
    }

    @Test
    void derivedIdIsStableAcrossCalls() {
        ServiceComponent meta = ScanDependencyService.class.getAnnotation(ServiceComponent.class);
        assertEquals(ServiceComponentScanner.resolveServiceId(meta), ServiceComponentScanner.resolveServiceId(meta));
        assertNotEquals(ServiceComponentScanner.resolveServiceId(meta),
                ServiceComponentScanner.resolveServiceId(ScanBrokenSetterService.class.getAnnotation(ServiceComponent.class)));
    }

    @Test
    void anExplicitAnnotationUuidWins() {
        ServiceComponent meta = ScanPinnedIdService.class.getAnnotation(ServiceComponent.class);
        assertEquals(UUID.fromString("6f1d6b2e-0a1c-4f0a-9f2b-2a1f3c4d5e6f"),
                ServiceComponentScanner.resolveServiceId(meta));
    }
}
