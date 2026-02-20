package de.happybavarian07.coolstufflib.service.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class ServiceShutdownEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final UUID serviceId;
    private final String serviceName;
    private boolean cancelled;

    public ServiceShutdownEvent(UUID serviceId, String serviceName) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
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

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}

