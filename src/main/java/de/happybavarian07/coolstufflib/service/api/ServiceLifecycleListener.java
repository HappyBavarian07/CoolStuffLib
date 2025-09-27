package de.happybavarian07.coolstufflib.service.api;

public interface ServiceLifecycleListener {
    void onStateChange(String serviceId, ServiceState from, ServiceState to, Throwable failure);
}

