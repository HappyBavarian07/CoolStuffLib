package de.happybavarian07.coolstufflib.service.debug;

import de.happybavarian07.coolstufflib.service.annotation.ServiceComponent;
import de.happybavarian07.coolstufflib.service.api.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@ServiceComponent(serviceName = "serviceB", dependsOn = {"serviceA"})
public class DebugServiceB implements Service {
    private final DebugServiceA serviceA;
    private UUID id;
    private String name;

    public DebugServiceB(DebugServiceA serviceA) {
        this.serviceA = serviceA;
    }

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

    public CompletableFuture<Void> onReload() {
        return CompletableFuture.completedFuture(null);
    }

    public DebugServiceA getServiceA() {
        return serviceA;
    }
}

