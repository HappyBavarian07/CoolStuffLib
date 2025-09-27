package de.happybavarian07.coolstufflib.service.spigot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ServiceFailEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String serviceId;
    private final Throwable failure;

    public ServiceFailEvent(String serviceId, Throwable failure) {
        this.serviceId = serviceId;
        this.failure = failure;
    }

    public String getServiceId() { return serviceId; }
    public Throwable getFailure() { return failure; }
    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}

