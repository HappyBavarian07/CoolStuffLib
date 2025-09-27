package de.happybavarian07.coolstufflib.service.debug;
import de.happybavarian07.coolstufflib.service.api.Service;
import de.happybavarian07.coolstufflib.service.annotation.ServiceComponent;
import java.util.concurrent.CompletableFuture;

@ServiceComponent(id = "serviceA")
public class DebugServiceA implements Service {
    private DebugConfig config;
    public void setConfig(DebugConfig config) { this.config = config; }
    public String id() { return "serviceA"; }
    public CompletableFuture<Void> init() { return CompletableFuture.completedFuture(null); }
    public CompletableFuture<Void> shutdown() { return CompletableFuture.completedFuture(null); }
    public CompletableFuture<Void> onReload() { return CompletableFuture.completedFuture(null); }
    public DebugConfig getConfig() { return config; }
}

