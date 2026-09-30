package de.happybavarian07.coolstufflib.service.scan;

import de.happybavarian07.coolstufflib.service.annotation.ServiceComponent;
import de.happybavarian07.coolstufflib.service.api.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@ServiceComponent(serviceName = "scanDependency")
public class ScanDependencyService implements Service {
    private UUID id;
    private String name;

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
}
