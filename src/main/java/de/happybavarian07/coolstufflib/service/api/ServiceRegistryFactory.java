package de.happybavarian07.coolstufflib.service.api;

public interface ServiceRegistryFactory {
    ServiceRegistry getGlobalRegistry();
    ServiceRegistry getWorldRegistry(String worldName);
    ServiceRegistry getPlayerRegistry(String playerName);
}