package de.happybavarian07.coolstufflib.service.debug;
import de.happybavarian07.coolstufflib.service.api.Service;
import de.happybavarian07.coolstufflib.service.annotation.ServiceComponent;
import java.util.concurrent.CompletableFuture;

@ServiceComponent(id = "serviceB", dependsOn = {"serviceA"})
public class DebugServiceB implements Service {
    private final DebugServiceA serviceA;
    public DebugServiceB(DebugServiceA serviceA) { this.serviceA = serviceA; }
    public String id() { return "serviceB"; }
    public CompletableFuture<Void> init() { return CompletableFuture.completedFuture(null); }
    public CompletableFuture<Void> shutdown() { return CompletableFuture.completedFuture(null); }
    public CompletableFuture<Void> onReload() { return CompletableFuture.completedFuture(null); }
    public DebugServiceA getServiceA() { return serviceA; }
}

