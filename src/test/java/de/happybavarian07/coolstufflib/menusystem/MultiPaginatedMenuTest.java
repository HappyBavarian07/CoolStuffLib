package de.happybavarian07.coolstufflib.menusystem;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.menusystem.pagination.PageControlLayout;
import de.happybavarian07.coolstufflib.menusystem.pagination.PaginationZone;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MultiPaginatedMenuTest {
    private final ItemStack a = mock(ItemStack.class);
    private final ItemStack b = mock(ItemStack.class);
    private final ItemStack c = mock(ItemStack.class);
    private final ItemStack next = mock(ItemStack.class);
    private PlayerMenuUtility pmu;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        CoolStuffLib lib = mock(CoolStuffLib.class);
        LanguageManager lgm = mock(LanguageManager.class);
        when(lib.getLanguageManager()).thenReturn(lgm);
        Player player = mock(Player.class);
        when(player.hasPermission(anyString())).thenReturn(true);
        when(lgm.getItem("General.Right", player, false)).thenReturn(next);
        inventory = mock(Inventory.class);
        when(player.openInventory(inventory)).thenReturn(mock(InventoryView.class));
        pmu = mock(PlayerMenuUtility.class);
        when(pmu.getLib()).thenReturn(lib);
        when(pmu.getOwner()).thenReturn(player);
    }

    @Test
    void zoneDefinedOnEveryRenderKeepsItsPageAndDrawsControls() {
        ZoneMenu menu = new ZoneMenu(pmu);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), anyInt(), anyString())).thenReturn(inventory);
            menu.open();
            verify(inventory).setItem(8, next);
            verify(inventory).setItem(0, a);

            InventoryClickEvent click = mock(InventoryClickEvent.class);
            when(click.getRawSlot()).thenReturn(8);
            menu.handleMenu(click);
        }
        verify(inventory).setItem(0, c);
    }

    class ZoneMenu extends MultiPaginatedMenu {
        private final Map<String, ItemStack> items = Map.of("a", a, "b", b, "c", c);

        ZoneMenu(PlayerMenuUtility pmu) {
            super(pmu);
        }

        @Override
        protected void preSetMenuItems() {
            defineZone("list", new PaginationZone(0, 1), List.of("a", "b", "c"), items::get);
            setZoneControls("list", new PageControlLayout(7, -1, 8, -1));
        }

        @Override
        protected void postSetMenuItems() {
        }

        @Override
        protected void handleZoneItemClick(String zoneId, Object data, int dataIndex, int slot, ItemStack item, InventoryClickEvent event) {
        }

        @Override
        protected void handleOutsideZoneClick(int slot, ItemStack item, InventoryClickEvent event) {
        }

        @Override
        public String getMenuName() {
            return "Zones";
        }

        @Override
        public String getConfigMenuAddonFeatureName() {
            return "zones";
        }

        @Override
        public int getSlots() {
            return 9;
        }

        @Override
        public void handleOpenMenu(InventoryOpenEvent e) {
        }

        @Override
        public void handleCloseMenu(InventoryCloseEvent e) {
        }
    }
}
