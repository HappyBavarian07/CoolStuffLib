package de.happybavarian07.coolstufflib.service.api;

import de.happybavarian07.coolstufflib.service.api.ServiceState;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ServiceManagementAPI {
    Map<UUID, ServiceState> getServiceStates();
    Optional<ServiceState> getServiceState(UUID id);
    CompletableFuture<Void> restartService(UUID id);
    CompletableFuture<Void> reloadService(UUID id);
    CompletableFuture<Void> reloadAllServices();
    Map<UUID, Long> getStartupTimes();
    Map<UUID, Long> getReloadTimes();
    Map<UUID, Integer> getHealthFailures();
}

