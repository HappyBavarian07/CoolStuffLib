package de.happybavarian07.coolstufflib.menusystem;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.menusystem.actions.MenuAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MenuActionDispatchTest {
    private CoolStuffLib lib;
    private PlayerMenuUtility pmu;
    private Player player;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        lib = mock(CoolStuffLib.class);
        when(lib.getLanguageManager()).thenReturn(mock(LanguageManager.class));
        player = mock(Player.class);
        when(player.hasPermission(anyString())).thenReturn(true);
        inventory = mock(Inventory.class);
        when(player.openInventory(inventory)).thenReturn(mock(InventoryView.class));
        pmu = mock(PlayerMenuUtility.class);
        when(pmu.getLib()).thenReturn(lib);
        when(pmu.getOwner()).thenReturn(player);
    }

    @Test
    void reopeningDropsActionsThatAreNoLongerRegistered() {
        TestMenu menu = new TestMenu(pmu, (p, e) -> {});
        open(menu);
        assertTrue(menu.slotActions.containsKey(3));

        menu.registerSlot3 = false;
        open(menu);
        assertFalse(menu.slotActions.containsKey(3));
    }

    @Test
    void clickOnRegisteredSlotRunsActionAndCancels() {
        MenuAction action = mock(MenuAction.class);
        TestMenu menu = new TestMenu(pmu, action);
        open(menu);
        InventoryClickEvent click = clickEvent(menu, 3);

        new MenuListener().onMenuClick(click);

        verify(click).setCancelled(true);
        verify(action).execute(player, click);
    }

    @Test
    void closingListenerMenuUnregistersIt() {
        ListenerMenu menu = mock(ListenerMenu.class);
        try (MockedStatic<CoolStuffLib> statics = mockStatic(CoolStuffLib.class);
             MockedStatic<HandlerList> handlers = mockStatic(HandlerList.class)) {
            statics.when(CoolStuffLib::getLib).thenReturn(lib);
            new MenuListener().onInvClose(closeEvent(menu));
            handlers.verify(() -> HandlerList.unregisterAll((Listener) menu));
        }
    }

    @Test
    void closingPlainMenuDoesNotUnregister() {
        TestMenu menu = new TestMenu(pmu, null);
        try (MockedStatic<CoolStuffLib> statics = mockStatic(CoolStuffLib.class);
             MockedStatic<HandlerList> handlers = mockStatic(HandlerList.class)) {
            statics.when(CoolStuffLib::getLib).thenReturn(lib);
            new MenuListener().onInvClose(closeEvent(menu));
            handlers.verify(() -> HandlerList.unregisterAll(any(Listener.class)), never());
        }
    }

    private void open(Menu menu) {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), anyInt(), anyString())).thenReturn(inventory);
            menu.open();
        }
    }

    private InventoryClickEvent clickEvent(Menu menu, int slot) {
        InventoryClickEvent click = mock(InventoryClickEvent.class);
        Inventory clicked = mock(Inventory.class);
        when(clicked.getHolder()).thenReturn(menu);
        when(click.getInventory()).thenReturn(clicked);
        when(click.getCurrentItem()).thenReturn(mock(ItemStack.class));
        when(click.getRawSlot()).thenReturn(slot);
        when(click.getWhoClicked()).thenReturn(player);
        return click;
    }

    private InventoryCloseEvent closeEvent(Menu menu) {
        InventoryCloseEvent close = mock(InventoryCloseEvent.class);
        Inventory closed = mock(Inventory.class);
        when(closed.getHolder()).thenReturn(menu);
        when(close.getInventory()).thenReturn(closed);
        return close;
    }

    @Test
    void refreshRerendersIntoSameInventoryWithoutReopening() {
        TestMenu menu = new TestMenu(pmu, (p, e) -> {});
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), anyInt(), anyString())).thenReturn(inventory);
            menu.open();
            menu.registerSlot3 = false;
            menu.refresh();
            bukkit.verify(() -> Bukkit.createInventory(any(InventoryHolder.class), anyInt(), anyString()), times(1));
        }
        verify(inventory).clear();
        verify(player, times(1)).openInventory(inventory);
        assertFalse(menu.slotActions.containsKey(3));
    }

    @Test
    void refreshBeforeOpenOpens() {
        TestMenu menu = new TestMenu(pmu, (p, e) -> {});
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), anyInt(), anyString())).thenReturn(inventory);
            menu.refresh();
        }
        verify(player).openInventory(inventory);
        assertTrue(menu.slotActions.containsKey(3));
    }

    @Test
    void buttonUsesItemAndSlotFromLanguageFile() {
        LanguageManager lgm = lib.getLanguageManager();
        ItemStack item = mock(ItemStack.class);
        when(lgm.getItem("Test.Heal", player, false)).thenReturn(item);
        when(lgm.getCustomObject("Items.Test.Heal.slot", null, -1, false)).thenReturn(5);
        MenuAction action = mock(MenuAction.class);
        TestMenu menu = new TestMenu(pmu, null);
        menu.render = m -> assertTrue(m.button("Test.Heal", action));
        menu.registerSlot3 = false;

        open(menu);

        verify(inventory).setItem(5, item);
        assertSame(action, menu.slotActions.get(5));
    }

    @Test
    void buttonWithoutConfiguredSlotTakesFirstFreeSlot() {
        LanguageManager lgm = lib.getLanguageManager();
        when(lgm.getCustomObject("Items.Test.Info.slot", null, -1, false)).thenReturn(-1);
        TestMenu menu = new TestMenu(pmu, null);
        menu.render = m -> assertTrue(m.button("Test.Info", MenuAction.none()));
        menu.registerSlot3 = false;

        open(menu);

        assertTrue(menu.slotActions.containsKey(0));
    }

    @Test
    void registerButtonOverloadsWithoutForbiddenSet() {
        MenuAction a = mock(MenuAction.class);
        MenuAction b = mock(MenuAction.class);
        TestMenu menu = new TestMenu(pmu, null);
        menu.registerSlot3 = false;
        menu.render = m -> {
            assertTrue(m.registerButton(7, mock(ItemStack.class), a));
            assertTrue(m.registerButton(mock(ItemStack.class), b));
        };

        open(menu);

        assertSame(a, menu.slotActions.get(7));
        assertSame(b, menu.slotActions.get(0));
    }

    static class TestMenu extends Menu {
        private final MenuAction action;
        boolean registerSlot3 = true;
        java.util.function.Consumer<TestMenu> render;

        TestMenu(PlayerMenuUtility pmu, MenuAction action) {
            super(pmu);
            this.action = action;
        }

        @Override
        public String getMenuName() {
            return "Test";
        }

        @Override
        public String getConfigMenuAddonFeatureName() {
            return "test";
        }

        @Override
        public int getSlots() {
            return 9;
        }

        @Override
        public void handleMenu(InventoryClickEvent e) {
        }

        @Override
        public void handleOpenMenu(InventoryOpenEvent e) {
        }

        @Override
        public void handleCloseMenu(InventoryCloseEvent e) {
        }

        @Override
        public void setMenuItems() {
            if (registerSlot3) registerButton(3, mock(ItemStack.class), action, null);
            if (render != null) render.accept(this);
        }
    }

    abstract static class ListenerMenu extends Menu implements Listener {
        ListenerMenu(PlayerMenuUtility pmu) {
            super(pmu);
        }
    }
}
