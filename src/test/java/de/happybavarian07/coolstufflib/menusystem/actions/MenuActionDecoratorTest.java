package de.happybavarian07.coolstufflib.menusystem.actions;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.menusystem.Menu;
import de.happybavarian07.coolstufflib.utils.Utils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

import java.util.UUID;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MenuActionDecoratorTest {
    private Player player;
    private MenuAction inner;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        inner = mock(MenuAction.class);
    }

    @Test
    void requiresRunsActionWithPermission() {
        when(player.hasPermission("perm")).thenReturn(true);
        inner.requires("perm").execute(player, null);
        verify(inner).execute(player, null);
    }

    @Test
    void requiresSendsNoPermissionMessageFromLibLanguageManager() {
        CoolStuffLib lib = mock(CoolStuffLib.class);
        LanguageManager lgm = mock(LanguageManager.class);
        when(lib.getLanguageManager()).thenReturn(lgm);
        when(lgm.getMessage("Player.General.NoPermissions", player, true)).thenReturn("nope");
        try (MockedStatic<CoolStuffLib> statics = mockStatic(CoolStuffLib.class)) {
            statics.when(CoolStuffLib::getLib).thenReturn(lib);
            inner.requires("perm").execute(player, null);
        }
        verify(inner, never()).execute(any(), any());
        verify(player).sendMessage("nope");
    }

    @Test
    void outermostDecoratorRunsFirst() {
        MenuAction chained = inner.cooldown(60_000).requires("perm");
        chained.execute(player, null);
        verify(inner, never()).execute(any(), any());
    }

    @Test
    void thenRunsInOrder() {
        MenuAction second = mock(MenuAction.class);
        inner.then(second).execute(player, null);
        InOrder order = inOrder(inner, second);
        order.verify(inner).execute(player, null);
        order.verify(second).execute(player, null);
    }

    @Test
    void thenCloseClosesAfterAction() {
        inner.thenClose().execute(player, null);
        InOrder order = inOrder(inner, player);
        order.verify(inner).execute(player, null);
        order.verify(player).closeInventory();
    }

    @Test
    void backReturnsToParentOfClickedMenu() {
        Menu menu = mock(Menu.class);
        MenuAction.back().execute(player, clickOn(menu));
        verify(menu).closeAndReturnOrClose();
        verify(player, never()).closeInventory();
    }

    @Test
    void backWithoutMenuClosesInventory() {
        MenuAction.back().execute(player, null);
        verify(player).closeInventory();
    }

    @Test
    @SuppressWarnings("unchecked")
    void openBuildsMenuOnlyWhenClicked() {
        Menu menu = mock(Menu.class);
        Supplier<Menu> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn(menu);
        MenuAction action = MenuAction.open(supplier);
        verify(supplier, never()).get();
        action.execute(player, null);
        verify(menu).open();
    }

    @Test
    void cooldownBlocksSecondClick() {
        MenuAction action = inner.cooldown(60_000, "wait");
        action.execute(player, null);
        action.execute(player, null);
        verify(inner, times(1)).execute(player, null);
        verify(player).sendMessage("wait");
    }

    @Test
    void withSoundPlaysBeforeAction() {
        inner.withSound(Sound.UI_BUTTON_CLICK).execute(player, null);
        InOrder order = inOrder(player, inner);
        order.verify(player).playSound(any(), eq(Sound.UI_BUTTON_CLICK), eq(1f), eq(1f));
        order.verify(inner).execute(player, null);
    }

    @Test
    void confirmReturnsToClickedMenu() {
        Menu menu = mock(Menu.class);
        MenuAction action = inner.confirm("sure?");
        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            action.execute(player, clickOn(menu));
            utils.verify(() -> Utils.openConfirmationMenu(eq("sure?"), any(MenuAction.class), eq(player), eq(menu)));
        }
        verify(inner, never()).execute(any(), any());
    }

    @Test
    void noneDoesNothing() {
        MenuAction.none().execute(player, null);
        verifyNoInteractions(player);
    }

    private static InventoryClickEvent clickOn(Menu menu) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        Inventory inventory = mock(Inventory.class);
        when(inventory.getHolder()).thenReturn(menu);
        when(event.getInventory()).thenReturn(inventory);
        return event;
    }
}
