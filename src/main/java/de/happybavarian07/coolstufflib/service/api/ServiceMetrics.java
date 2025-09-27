package de.happybavarian07.coolstufflib.service.api;
import java.util.Map;

public interface ServiceMetrics {
    long getStartupTime(String serviceId);
    long getReloadTime(String serviceId);
    Map<String, Long> getAllStartupTimes();
    Map<String, Long> getAllReloadTimes();
    int getHealthCheckFailures(String serviceId);
    Map<String, Integer> getAllHealthCheckFailures();
}

