package de.happybavarian07.coolstufflib.service.impl;

import java.util.UUID;

/**
 * <p>Service registry associated with a specific player.</p>
 */
public class PlayerServiceRegistry extends DefaultServiceRegistry {
    private final UUID playerId;

    /**
     * <p>Creates a new player-specific registry.</p>
     *
     * @param playerUUID The player's UUID
     */
    public PlayerServiceRegistry(UUID playerUUID) {
        this.playerId = playerUUID;
    }

    public UUID getPlayerUUID() {
        return playerId;
    }
}


