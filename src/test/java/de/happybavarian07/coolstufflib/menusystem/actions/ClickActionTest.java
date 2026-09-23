package de.happybavarian07.coolstufflib.menusystem.actions;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClickActionTest {
    private final Player player = mock(Player.class);
    private MenuAction left;
    private MenuAction right;
    private MenuAction shift;
    private MenuAction middle;
    private MenuAction fallback;

    @BeforeEach
    void setUp() {
        left = mock(MenuAction.class);
        right = mock(MenuAction.class);
        shift = mock(MenuAction.class);
        middle = mock(MenuAction.class);
        fallback = mock(MenuAction.class);
    }

    @Test
    void routesEachClickType() {
        ClickAction action = ClickAction.of().left(left).right(right).shift(shift).middle(middle).otherwise(fallback);

        InventoryClickEvent l = click(ClickType.LEFT);
        InventoryClickEvent r = click(ClickType.RIGHT);
        InventoryClickEvent s = click(ClickType.SHIFT_RIGHT);
        InventoryClickEvent m = click(ClickType.MIDDLE);
        InventoryClickEvent d = click(ClickType.DROP);
        action.execute(player, l);
        action.execute(player, r);
        action.execute(player, s);
        action.execute(player, m);
        action.execute(player, d);

        verify(left).execute(player, l);
        verify(right).execute(player, r);
        verify(shift).execute(player, s);
        verify(middle).execute(player, m);
        verify(fallback).execute(player, d);
    }

    @Test
    void shiftFallsThroughToLeftOrRightWhenUnset() {
        ClickAction action = ClickAction.of().left(left).right(right);
        InventoryClickEvent shiftLeft = click(ClickType.SHIFT_LEFT);
        InventoryClickEvent shiftRight = click(ClickType.SHIFT_RIGHT);

        action.execute(player, shiftLeft);
        action.execute(player, shiftRight);

        verify(left).execute(player, shiftLeft);
        verify(right).execute(player, shiftRight);
    }

    @Test
    void unmatchedClickWithoutFallbackDoesNothing() {
        ClickAction.of().left(left).execute(player, click(ClickType.RIGHT));
        verify(left, never()).execute(any(), any());
    }

    @Test
    void programmaticCallWithoutEventUsesLeft() {
        ClickAction.of().left(left).execute(player, null);
        verify(left).execute(player, null);
    }

    private static InventoryClickEvent click(ClickType type) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getClick()).thenReturn(type);
        return event;
    }
}
