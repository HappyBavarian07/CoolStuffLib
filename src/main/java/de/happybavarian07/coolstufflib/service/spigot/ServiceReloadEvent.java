package de.happybavarian07.coolstufflib.service.spigot;
import org.bukkit.event.Event;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class ServiceReloadEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final String serviceId;
    private boolean cancelled;

    public ServiceReloadEvent(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceId() { return serviceId; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}

