package de.happybavarian07.coolstufflib.service.api;

public class ServiceEvent {
    private final String serviceId;
    private boolean cancelled;
    private final Throwable failure;

    public ServiceEvent(String serviceId, Throwable failure) {
        this.serviceId = serviceId;
        this.failure = failure;
    }

    public String getServiceId() { return serviceId; }
    public Throwable getFailure() { return failure; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}

