package de.happybavarian07.coolstufflib.menusystem.actions;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import de.happybavarian07.coolstufflib.utils.CooldownTracker;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CooldownActionTest {
    @Test
    void blocksRepeatWithinCooldownAndSendsMessage() {
        MenuAction inner = mock(MenuAction.class);
        CooldownAction action = new CooldownAction(60_000, inner, "wait");
        Player player = player();

        action.execute(player, null);
        action.execute(player, null);

        verify(inner, times(1)).execute(player, null);
        verify(player).sendMessage("wait");
    }

    @Test
    void cooldownIsPerPlayer() {
        MenuAction inner = mock(MenuAction.class);
        CooldownAction action = new CooldownAction(60_000, inner);
        Player a = player();
        Player b = player();

        action.execute(a, null);
        action.execute(b, null);

        verify(inner).execute(a, null);
        verify(inner).execute(b, null);
    }

    @Test
    void expiredEntriesArePruned() throws Exception {
        CooldownAction action = new CooldownAction(20, mock(MenuAction.class));
        action.execute(player(), null);
        Thread.sleep(50);
        action.execute(player(), null);

        assertEquals(1, tracker(action).size());
    }

    @Test
    void zeroCooldownAlwaysRuns() {
        MenuAction inner = mock(MenuAction.class);
        CooldownAction action = new CooldownAction(0, inner);
        Player player = player();

        action.execute(player, null);
        action.execute(player, null);

        verify(inner, times(2)).execute(player, null);
    }

    private static Player player() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        return player;
    }

    private static CooldownTracker tracker(CooldownAction action) throws Exception {
        Field field = CooldownAction.class.getDeclaredField("tracker");
        field.setAccessible(true);
        return (CooldownTracker) field.get(action);
    }
}
