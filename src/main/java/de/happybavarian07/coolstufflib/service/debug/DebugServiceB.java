package de.happybavarian07.coolstufflib.service.debug;

import de.happybavarian07.coolstufflib.service.annotation.ServiceComponent;
import de.happybavarian07.coolstufflib.service.api.Service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@ServiceComponent(id = "serviceB", dependsOn = {"serviceA"})
public final class DebugServiceB implements Service {
    private final DebugServiceA serviceA;

    public DebugServiceB(DebugServiceA serviceA) {
        this.serviceA = serviceA;
    }

    public String id() {
        return "serviceB";
    }

    public CompletableFuture<Void> init() {
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    public DebugServiceA getServiceA() {
        return serviceA;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (DebugServiceB) obj;
        return Objects.equals(this.serviceA, that.serviceA);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceA);
    }

    @Override
    public String toString() {
        return "DebugServiceB[" +
                "serviceA=" + serviceA + ']';
    }

}
