package de.happybavarian07.coolstufflib.service.impl;

import de.happybavarian07.coolstufflib.service.api.Config;

public class WorldServiceRegistry extends DefaultServiceRegistry {
    private final String worldName;

    public WorldServiceRegistry(String worldName) {
        this.worldName = worldName;
    }

    public String getWorldName() {
        return worldName;
    }

    public void registerAnnotatedServices(String packageName, Config config) {
        super.registerAnnotatedServices(packageName, config);
    }
}
