package de.happybavarian07.coolstufflib.service.api;

import java.util.Map;
import java.util.UUID;

public interface ServiceMetrics {
    long getStartupTime(UUID serviceId);
    long getReloadTime(UUID serviceId);
    Map<UUID, Long> getAllStartupTimes();
    Map<UUID, Long> getAllReloadTimes();
    int getHealthCheckFailures(UUID serviceId);
    Map<UUID, Integer> getAllHealthCheckFailures();
}
