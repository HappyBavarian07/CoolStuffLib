package de.happybavarian07.coolstufflib.service.impl;

/**
 * <p>Service registry associated with a specific world.</p>
 */
public class WorldServiceRegistry extends DefaultServiceRegistry {
    private final String worldName;

    /**
     * <p>Creates a new world-specific registry.</p>
     *
     * @param worldName The world name
     */
    public WorldServiceRegistry(String worldName) {
        this.worldName = worldName;
    }

    public String getWorldName() {
        return worldName;
    }
}
