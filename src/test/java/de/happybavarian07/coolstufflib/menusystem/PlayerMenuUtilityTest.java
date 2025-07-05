package de.happybavarian07.coolstufflib.menusystem;

import de.happybavarian07.coolstufflib.menusystem.PlayerMenuUtility;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerMenuUtilityTest {

    @Mock
    private Player mockPlayer;

    private PlayerMenuUtility playerMenuUtility;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockPlayer.getName()).thenReturn("TestPlayer");
        when(mockPlayer.getUniqueId()).thenReturn(java.util.UUID.randomUUID());

        playerMenuUtility = new PlayerMenuUtility(mockPlayer.getUniqueId());
    }

    @Test
    void testPlayerMenuUtilityInitialization() {
        assertNotNull(playerMenuUtility);
        assertEquals(mockPlayer.getUniqueId(), playerMenuUtility.getOwnerUUID());
    }

    @Test
    void testPlayerAccess() {
        UUID retrievedPlayer = playerMenuUtility.getOwnerUUID();
        assertSame(mockPlayer.getUniqueId(), retrievedPlayer);
    }
}
