package de.happybavarian07.coolstufflib.service.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class ServiceFailEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final UUID serviceId;
    private final String serviceName;
    private final Throwable failure;

    public ServiceFailEvent(UUID serviceId, String serviceName, Throwable failure) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.failure = failure;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public String getServiceName() {
        return serviceName;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public Throwable getFailure() {
        return failure;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}

