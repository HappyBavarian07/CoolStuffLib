package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Drives {@link LanguageManager#renderItem} and {@link ItemBuilder} without a server; ItemMeta is a small stateful fake. */
class ItemRenderingTest {
    @TempDir
    Path folder;
    private MockedStatic<Bukkit> bukkit;
    private LanguageManager lgm;

    @BeforeEach
    void setUp() throws IOException {
        ItemFactory factory = mock(ItemFactory.class);
        when(factory.getItemMeta(any(Material.class))).thenAnswer(call -> fakeMeta());
        when(factory.isApplicable(any(ItemMeta.class), any(Material.class))).thenReturn(true);
        when(factory.asMetaFor(any(ItemMeta.class), any(Material.class))).thenAnswer(call -> call.getArgument(0));
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getItemFactory).thenReturn(factory);

        Files.writeString(folder.resolve("en.yml"), """
                LanguageFullName: English
                Items:
                  Heal:
                    material: GOLDEN_APPLE
                    displayName: '&aHeal %x%'
                    lore:
                      - '&7Heals %x% hearts'
                      - '&7Second line'
                  Hidden:
                    disabled: true
                    material: STONE
                    displayName: 'off'
                  General:
                    DisabledItem:
                      material: BARRIER
                      displayName: '&cDisabled'
                      lore:
                        - '&7Not available'
                """);
        Files.writeString(folder.resolve("de.yml"), "LanguageFullName: Deutsch\nLanguageParent: en\nItems:\n  Heal:\n    material: APPLE\n    displayName: 'Heilen %x%'\n");
        lgm = new LanguageManager(mock(JavaPlugin.class), folder.toFile(), "none", "[P]");
        lgm.addLanguagesToList(false);
        lgm.setCurrentLang(lgm.getLang("en", true), false);
    }

    @AfterEach
    void closeStatic() {
        bukkit.close();
    }

    @Test
    void theBuilderFillsNameAndLoreAndTheLocalPlaceholder() {
        ItemStack item = lgm.item("Heal").with("%x%", 5).build(null);

        assertEquals(Material.GOLDEN_APPLE, item.getType());
        assertEquals("§aHeal 5§a", item.getItemMeta().getDisplayName());
        assertEquals(List.of("§7Heals 5 hearts", "§7Second line"), item.getItemMeta().getLore());
        assertTrue(lgm.getPlaceholders().isEmpty(), "a local placeholder must not leak into the global map");
    }

    @Test
    void theItemsRootIsOptionalAndTheOldApiRendersTheSameItem() {
        ItemStack viaBuilder = lgm.item("Items.Heal").with("%x%", 2).build(null);
        ItemStack viaOldApi = lgm.getItem("Heal", null, false);

        assertEquals("§aHeal 2§a", viaBuilder.getItemMeta().getDisplayName());
        assertEquals("§aHeal %x%", viaOldApi.getItemMeta().getDisplayName());
        assertEquals(Material.GOLDEN_APPLE, viaOldApi.getType());
    }

    @Test
    void aLanguageWithTheItemUsesItAndOtherLanguagesFallBackToTheParent() {
        ItemStack german = lgm.item("Heal").lang("de").with("%x%", 1).build(null);
        assertEquals(Material.APPLE, german.getType());
        assertEquals("Heilen 1", german.getItemMeta().getDisplayName());

        ItemStack disabledParentItem = lgm.item("General.DisabledItem").lang("de").build(null);
        assertEquals(Material.BARRIER, disabledParentItem.getType());
        assertEquals("§cDisabled", disabledParentItem.getItemMeta().getDisplayName());
    }

    @Test
    void aDisabledItemRendersTheDisabledItem() {
        ItemStack item = lgm.item("Hidden").build(null);

        assertEquals(Material.BARRIER, item.getType());
        assertEquals("§cDisabled", item.getItemMeta().getDisplayName());
        assertEquals(List.of("§7Not available"), item.getItemMeta().getLore());
    }

    @Test
    void anUnknownPathGivesTheErrorItem() {
        ItemStack item = lgm.item("Nowhere").build(null);

        assertEquals(Material.BARRIER, item.getType());
        assertEquals("Config Path not found!", item.getItemMeta().getDisplayName());
        assertTrue(item.getItemMeta().getLore().contains("Path: Items.Nowhere"));
    }

    @Test
    void debugAddsTheKeyAndOriginAsTheLastLoreLine() {
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        assertTrue(lgm.toggleDebug(console));

        List<String> lore = lgm.item("Heal").build(null).getItemMeta().getLore();

        assertEquals("§8Items.Heal @ en/items/Heal.yml:2", lore.get(lore.size() - 1));
        assertEquals(3, lore.size());
    }

    private static ItemMeta fakeMeta() {
        ItemMeta meta = mock(ItemMeta.class);
        String[] name = {null};
        List<List<String>> lore = new ArrayList<>();
        lore.add(null);
        doAnswer(call -> {
            name[0] = call.getArgument(0);
            return null;
        }).when(meta).setDisplayName(any());
        when(meta.getDisplayName()).thenAnswer(call -> name[0]);
        doAnswer(call -> {
            List<String> given = call.getArgument(0);
            lore.set(0, given == null ? null : new ArrayList<>(given));
            return null;
        }).when(meta).setLore(any());
        when(meta.getLore()).thenAnswer(call -> lore.get(0));
        when(meta.clone()).thenReturn(meta);
        return meta;
    }
}
