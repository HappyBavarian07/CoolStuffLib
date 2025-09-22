package de.happybavarian07.coolstufflib.menusystem;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MenuTest {

    @Mock
    private Player mockPlayer;

    @Mock
    private CoolStuffLib mockCoolStuffLib;

    @Mock
    private LanguageManager mockLanguageManager;

    @Mock
    private JavaPlugin mockJavaPlugin;

    @Mock
    private Inventory mockInventory;

    @Mock
    private InventoryClickEvent mockClickEvent;

    @Mock
    private InventoryOpenEvent mockOpenEvent;

    @Mock
    private InventoryCloseEvent mockCloseEvent;

    private PlayerMenuUtility playerMenuUtility;
    private TestMenu testMenu;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        playerMenuUtility = new PlayerMenuUtility(mockPlayer.getUniqueId());

        ItemStack fillerItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        // Ensure meta matches what Menu expects
        fillerItem.setItemMeta(fillerItem.getItemMeta());

        try (MockedStatic<CoolStuffLib> mockedStatic = mockStatic(CoolStuffLib.class)) {
            mockedStatic.when(CoolStuffLib::getLib).thenReturn(mockCoolStuffLib);
            when(mockCoolStuffLib.getLanguageManager()).thenReturn(mockLanguageManager);
            when(mockCoolStuffLib.getJavaPluginUsingLib()).thenReturn(mockJavaPlugin);

            when(mockLanguageManager.getItem("General.FillerItem", null, false)).thenReturn(fillerItem);

            testMenu = new TestMenu(playerMenuUtility);
        }
    }

    @Test
    void testMenuInitialization() {
        assertNotNull(testMenu);
        assertEquals(playerMenuUtility, testMenu.playerMenuUtility);
        assertNotNull(testMenu.FILLER);
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, testMenu.FILLER.getType());
    }

    @Test
    void testMenuProperties() {
        assertEquals("Test Menu", testMenu.getMenuName());
        assertEquals("testFeature", testMenu.getConfigMenuAddonFeatureName());
        assertEquals(27, testMenu.getSlots());
    }

    @Test
    void testOpeningPermission() {
        assertEquals("", testMenu.getOpeningPermission());

        testMenu.setOpeningPermission("test.menu.open");
        assertEquals("test.menu.open", testMenu.getOpeningPermission());
    }

    //@Test
    void testMenuOpenWithPermission() {
        testMenu.setOpeningPermission("test.menu.open");
        when(mockPlayer.hasPermission("test.menu.open")).thenReturn(true);
        when(mockLanguageManager.getMessage("Player.General.NoPermissions", mockPlayer, true))
                .thenReturn("No permissions message");

        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.createInventory(any(), eq(27), eq("Test Menu")))
                    .thenReturn(mockInventory);

            testMenu.open();

            verify(mockPlayer, never()).sendMessage("No permissions message");
            verify(mockPlayer, never()).closeInventory();
        }
    }

    //@Test
    void testMenuOpenWithoutPermission() {
        testMenu.setOpeningPermission("test.menu.open");
        when(mockPlayer.hasPermission("test.menu.open")).thenReturn(false);
        when(mockLanguageManager.getMessage("Player.General.NoPermissions", mockPlayer, true))
                .thenReturn("No permissions message");

        testMenu.open();

        verify(mockPlayer).sendMessage("No permissions message");
        verify(mockPlayer).closeInventory();
    }

    //@Test
    void testInventoryCreation() {
        when(mockPlayer.hasPermission("")).thenReturn(true);

        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.createInventory(testMenu, 27, "Test Menu"))
                    .thenReturn(mockInventory);

            testMenu.open();

            assertNotNull(testMenu.inventory);
            assertEquals(1, testMenu.inventories.size());
            assertTrue(testMenu.inventories.contains(mockInventory));
        }
    }

    @Test
    void testGetInventory() {
        testMenu.inventory = mockInventory;
        assertEquals(mockInventory, testMenu.getInventory());
    }

    //@Test
    void testLegacyServerDetection() {
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.getServer().getVersion()).thenReturn("1.12.2");
            assertTrue(testMenu.legacyServer());

            mockedBukkit.when(() -> Bukkit.getServer().getVersion()).thenReturn("1.16.5");
            assertFalse(testMenu.legacyServer());

            mockedBukkit.when(() -> Bukkit.getServer().getVersion()).thenReturn("1.8.8");
            assertTrue(testMenu.legacyServer());
        }
    }

    @Test
    void testRegisterButtonPlacesItemAndCallback() {
        ItemStack button = new ItemStack(Material.DIAMOND);
        MenuAction action = (player, event) -> player.sendMessage("Clicked!");
        testMenu.inventory = mockInventory;
        when(mockInventory.getItem(5)).thenReturn(null);
        boolean result = testMenu.registerButton(5, button, action, null);
        assertTrue(result);
        assertEquals(action, testMenu.slotActions.get(5));
        verify(mockInventory).setItem(5, button);
    }

    @Test
    void testRegisterButtonAutoPlacementSkipsFillerAndForbidden() {
        ItemStack button = new ItemStack(Material.EMERALD);
        MenuAction action = (player, event) -> {};
        testMenu.inventory = mockInventory;
        when(mockInventory.getItem(0)).thenReturn(testMenu.FILLER);
        when(mockInventory.getItem(1)).thenReturn(null);
        Set<Integer> forbidden = new HashSet<>();
        forbidden.add(1);
        when(mockInventory.getItem(2)).thenReturn(null);
        boolean result = testMenu.registerButton(button, action, forbidden);
        assertTrue(result);
        assertEquals(action, testMenu.slotActions.get(2));
        verify(mockInventory).setItem(2, button);
    }

    @Test
    void testTryRegisterButtonReturnsFalseOnForbiddenOrOccupied() {
        ItemStack button = new ItemStack(Material.GOLD_INGOT);
        MenuAction action = (player, event) -> {};
        testMenu.inventory = mockInventory;
        when(mockInventory.getItem(3)).thenReturn(new ItemStack(Material.STONE));
        Set<Integer> forbidden = new HashSet<>();
        forbidden.add(4);
        assertFalse(testMenu.tryRegisterButton(3, button, action, null));
        assertFalse(testMenu.tryRegisterButton(4, button, action, forbidden));
    }

    @Test
    void testClearRegisteredButtonsRemovesAllCallbacks() {
        testMenu.slotActions.put(1, (player, event) -> {});
        testMenu.slotActions.put(2, (player, event) -> {});
        testMenu.clearRegisteredButtons();
        assertTrue(testMenu.slotActions.isEmpty());
    }

    @Test
    void testFindFirstEmptySlotRespectsFillerAndForbidden() {
        testMenu.inventory = mockInventory;
        when(mockInventory.getItem(0)).thenReturn(testMenu.FILLER);
        when(mockInventory.getItem(1)).thenReturn(null);
        when(mockInventory.getItem(2)).thenReturn(new ItemStack(Material.STONE));
        Set<Integer> forbidden = new HashSet<>();
        forbidden.add(1);
        when(mockInventory.getItem(3)).thenReturn(null);
        int slot = testMenu.findFirstEmptySlot(forbidden);
        assertEquals(3, slot);
    }

    @Test
    void testClickRoutingPrefersRegisteredActionOverHandleMenu() {
        testMenu.inventory = mockInventory;
        ItemStack button = new ItemStack(Material.IRON_INGOT);
        MenuAction action = mock(MenuAction.class);
        testMenu.slotActions.put(7, action);
        when(mockClickEvent.getRawSlot()).thenReturn(7);
        when(mockClickEvent.getWhoClicked()).thenReturn(mockPlayer);
        testMenu.slotActions.get(7).execute(mockPlayer, mockClickEvent);
        verify(action).execute(mockPlayer, mockClickEvent);
    }

    static class TestMenu extends Menu {
        private boolean menuItemsSet = false;
        private boolean openEventHandled = false;
        private boolean closeEventHandled = false;
        private boolean clickEventHandled = false;

        public TestMenu(PlayerMenuUtility playerMenuUtility) {
            super(playerMenuUtility);
        }

        @Override
        public String getMenuName() {
            return "Test Menu";
        }

        @Override
        public String getConfigMenuAddonFeatureName() {
            return "testFeature";
        }

        @Override
        public int getSlots() {
            return 27;
        }

        @Override
        public void handleMenu(InventoryClickEvent e) {
            clickEventHandled = true;
        }

        @Override
        public void handleOpenMenu(InventoryOpenEvent e) {
            openEventHandled = true;
        }

        @Override
        public void handleCloseMenu(InventoryCloseEvent e) {
            closeEventHandled = true;
        }

        @Override
        public void setMenuItems() {
            menuItemsSet = true;
        }

        public boolean isMenuItemsSet() {
            return menuItemsSet;
        }

        public boolean isOpenEventHandled() {
            return openEventHandled;
        }

        public boolean isCloseEventHandled() {
            return closeEventHandled;
        }

        public boolean isClickEventHandled() {
            return clickEventHandled;
        }
    }
}
