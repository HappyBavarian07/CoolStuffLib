package de.happybavarian07.coolstufflib.service.impl;

import java.util.UUID;

public class PlayerServiceRegistry extends DefaultServiceRegistry {
    private final UUID playerId;

    public PlayerServiceRegistry(UUID playerUUID) {
        this.playerId = playerUUID;
    }

    public UUID getPlayerUUID() {
        return playerId;
    }
}


