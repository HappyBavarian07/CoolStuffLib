package de.happybavarian07.coolstufflib.service.debug;

import de.happybavarian07.coolstufflib.service.annotation.ServiceComponent;
import de.happybavarian07.coolstufflib.service.api.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@ServiceComponent(serviceName = "serviceA")
public class DebugServiceA implements Service {
    public UUID id;
    public String name;
    private DebugConfig config;

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

    public CompletableFuture<Void> onReload() {
        return CompletableFuture.completedFuture(null);
    }

    public DebugConfig getConfig() {
        return config;
    }

    public void setConfig(DebugConfig config) {
        this.config = config;
    }
}
