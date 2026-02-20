package de.happybavarian07.coolstufflib.service.impl;

public class WorldServiceRegistry extends DefaultServiceRegistry {
    private final String worldName;

    public WorldServiceRegistry(String worldName) {
        this.worldName = worldName;
    }

    public String getWorldName() {
        return worldName;
    }
}
