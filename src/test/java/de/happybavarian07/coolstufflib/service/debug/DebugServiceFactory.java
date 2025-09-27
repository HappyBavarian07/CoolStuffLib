package de.happybavarian07.coolstufflib.service.debug;
import de.happybavarian07.coolstufflib.service.api.*;
import java.util.concurrent.CompletableFuture;

public class DebugServiceFactory implements ServiceFactory<DebugServiceA> {
    public CompletableFuture<DebugServiceA> create(ServiceRegistry registry) {
        DebugServiceA serviceA = new DebugServiceA();
        serviceA.setConfig(new DebugConfig());
        return CompletableFuture.completedFuture(serviceA);
    }
}

