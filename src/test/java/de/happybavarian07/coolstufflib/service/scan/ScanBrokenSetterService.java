package de.happybavarian07.coolstufflib.service.scan;

import de.happybavarian07.coolstufflib.service.annotation.ServiceComponent;
import de.happybavarian07.coolstufflib.service.api.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The {@code setScanConfig} setter always throws, the remaining setters must still be injected.
 */
@ServiceComponent(serviceName = "scanBrokenSetter", dependsOn = {"scanDependency"})
public class ScanBrokenSetterService implements Service {
    private UUID id;
    private String name;
    private ScanConfig config;
    private ScanDependencyService dependency;

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public String serviceName() {
        return name;
    }

    public CompletableFuture<Void> init() {
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    public ScanConfig getConfig() {
        return config;
    }

    public void setScanConfig(ScanConfig config) {
        throw new IllegalStateException("injection refused");
    }

    public ScanDependencyService getDependency() {
        return dependency;
    }

    public void setScanDependencyService(ScanDependencyService dependency) {
        this.dependency = dependency;
    }
}
