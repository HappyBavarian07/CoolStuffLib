package de.happybavarian07.coolstufflib.menusystem;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ListMenuTest {
    private PlayerMenuUtility pmu;
    private Player player;
    private Inventory inventory;
    private final Map<String, ItemStack> rendered = Map.of("a", mock(ItemStack.class), "b", mock(ItemStack.class));

    @BeforeEach
    void setUp() {
        CoolStuffLib lib = mock(CoolStuffLib.class);
        LanguageManager lgm = mock(LanguageManager.class);
        when(lib.getLanguageManager()).thenReturn(lgm);
        when(lgm.getCustomObject(anyString(), any(), any(), anyBoolean())).thenAnswer(invocation -> invocation.getArgument(2));
        player = mock(Player.class);
        when(player.hasPermission(anyString())).thenReturn(true);
        inventory = mock(Inventory.class);
        when(player.openInventory(inventory)).thenReturn(mock(InventoryView.class));
        pmu = mock(PlayerMenuUtility.class);
        when(pmu.getLib()).thenReturn(lib);
        when(pmu.getOwner()).thenReturn(player);
    }

    @Test
    void rendersEntriesIntoContentSlotsAndRoutesClicks() {
        List<String> clicked = new java.util.ArrayList<>();
        ListMenu<String> menu = new ListMenu<>(pmu, null, "Title", 27, List.of("a", "b"), rendered::get,
                (p, entry, event) -> clicked.add(entry));
        open(menu);

        verify(inventory).setItem(10, rendered.get("a"));
        verify(inventory).setItem(11, rendered.get("b"));

        menu.handleMenu(click(11, rendered.get("b")));
        assertEquals(List.of("b"), clicked);
    }

    @Test
    void clickOnEmptyContentSlotIsIgnored() {
        List<String> clicked = new java.util.ArrayList<>();
        ListMenu<String> menu = new ListMenu<>(pmu, null, "Title", 27, List.of("a"), rendered::get,
                (p, entry, event) -> clicked.add(entry));
        open(menu);

        menu.handleMenu(click(12, mock(ItemStack.class)));
        assertTrue(clicked.isEmpty());
    }

    @Test
    void rejectsInvalidSize() {
        assertThrows(IllegalArgumentException.class,
                () -> new ListMenu<>(pmu, null, "Title", 20, List.of(), rendered::get, (p, e, ev) -> {}));
    }

    private void open(Menu menu) {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), anyInt(), anyString())).thenReturn(inventory);
            menu.open();
        }
    }

    private InventoryClickEvent click(int slot, ItemStack item) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getRawSlot()).thenReturn(slot);
        when(event.getCurrentItem()).thenReturn(item);
        when(event.getWhoClicked()).thenReturn(player);
        return event;
    }
}
