package de.happybavarian07.coolstufflib.service.impl;
import de.happybavarian07.coolstufflib.service.api.*;

public class PlayerServiceRegistry extends DefaultServiceRegistry {
    private final String playerName;
    public PlayerServiceRegistry(String playerName) {
        this.playerName = playerName;
    }
    public String getPlayerName() { return playerName; }
}


