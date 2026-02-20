package de.happybavarian07.coolstufflib.service.api;

import java.util.UUID;

public interface ServiceLifecycleListener {
    void onStateChange(UUID serviceId, String serviceName, ServiceState from, ServiceState to, Throwable failure);
}
