package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.languagemanager.PlaceholderType;
import de.happybavarian07.coolstufflib.menusystem.Menu;
import de.happybavarian07.coolstufflib.menusystem.PaginatedMenu;
import de.happybavarian07.coolstufflib.menusystem.PlayerMenuUtility;
import de.happybavarian07.coolstufflib.menusystem.pagination.InstantPageTransition;
import de.happybavarian07.coolstufflib.menusystem.pagination.NavigationDirection;
import de.happybavarian07.coolstufflib.menusystem.pagination.PageDirection;
import de.happybavarian07.coolstufflib.menusystem.pagination.PaginationZone;
import de.happybavarian07.coolstufflib.service.impl.ChatInputService;
import de.happybavarian07.coolstufflib.utils.ConfigUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>Auto-generated menu for editing AdvancedConfig structures in-game.</p>
 */
public class DynamicConfigMenu extends PaginatedMenu<Map.Entry<String, Object>> {

    private final ConfigSection currentSection;
    private final String currentPath;

    public DynamicConfigMenu(PlayerMenuUtility playerMenuUtility, Menu savedMenu, ConfigSection section, String currentPath) {
        super(playerMenuUtility, savedMenu);
        this.currentSection = section;
        this.currentPath = currentPath == null ? "Root" : currentPath;

        setPaginationZone(new PaginationZone(
                new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43}
        ));
        setNavigationDirection(NavigationDirection.HORIZONTAL);
        setPageDirection(PageDirection.NORMAL);
        setPageTransition(new InstantPageTransition<>());
    }

    @Override
    public String getMenuName() {
        lgm.addPlaceholder(PlaceholderType.MENUTITLE, "%path%", currentPath, false);
        return lgm.getMenuTitle("ConfigUI.MenuTitle", playerMenuUtility.getOwner());
    }

    @Override
    public String getConfigMenuAddonFeatureName() {
        return "DynamicConfigUI";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void preSetMenuItems() {
        List<Map.Entry<String, Object>> entries = new ArrayList<>();
        if (currentSection != null) {
            entries.addAll(currentSection.toSerializableMap().entrySet());
        }

        setPaginatedData(entries, entry -> {
            String key = entry.getKey();
            Object value = entry.getValue();

            lgm.addPlaceholder(PlaceholderType.ITEM, "%key%", key, false);
            lgm.addPlaceholder(PlaceholderType.ITEM, "%value%", value == null ? "null" : value.toString(), false);

            if (value instanceof ConfigSection || value instanceof Map) {
                return lgm.getItem("ConfigUI.Folder", playerMenuUtility.getOwner(), true);
            } else if (value instanceof Boolean b) {
                return lgm.getItem(b ? "Items.ConfigUI.Boolean_True" : "Items.ConfigUI.Boolean_False", playerMenuUtility.getOwner(), true);
            } else if (value instanceof Number) {
                return lgm.getItem("ConfigUI.NumberValue", playerMenuUtility.getOwner(), true);
            } else {
                return lgm.getItem("ConfigUI.StringValue", playerMenuUtility.getOwner(), true);
            }
        });
    }

    @Override
    public void postSetMenuItems() {
        // Additional fixed buttons could be set here
    }

    @Override
    protected void handlePageItemClick(int slot, ItemStack item, InventoryClickEvent event) {
        Map.Entry<String, Object> entry = getPaginatedDataForSlot(slot, item);
        if (entry == null) return;

        Player player = playerMenuUtility.getOwner();
        Object value = entry.getValue();
        String key = entry.getKey();

        if (value instanceof ConfigSection section) {
            new DynamicConfigMenu(playerMenuUtility, this, section, currentPath + "." + key).open();
        } else if (value instanceof Map) {
            ConfigSection child = currentSection.getSection(key);
            if (child != null) new DynamicConfigMenu(playerMenuUtility, this, child, currentPath + "." + key).open();
        } else if (value instanceof Boolean b) {
            currentSection.set(key, !b);
            super.open();
            player.sendMessage(lgm.getMessage("Player.ConfigUI.BooleanToggled", player, true));
        } else {
            player.closeInventory();
            player.sendMessage(lgm.getMessage("Player.ConfigUI.PromptInput", player, true));
            player.sendMessage(lgm.getMessage("Player.ConfigUI.PromptCancel", player, true));

            ChatInputService inputService = lib.requireService("chat-input-service", ChatInputService.class);

            inputService.requestInput(player, input -> {
                if (input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase("exit")) {
                    player.sendMessage(lgm.getMessage("Player.ConfigUI.InputCancelled", player, true));
                    super.open();
                    return;
                }

                Object parsedValue = input;
                if (value instanceof Number) {
                    parsedValue = ConfigUtils.parseNumber(input);
                    if (parsedValue == null) {
                        player.sendMessage(lgm.getMessage("Player.ConfigUI.InvalidNumber", player, true));
                        super.open();
                        return;
                    }
                }

                currentSection.set(key, parsedValue);
                player.sendMessage(lgm.getMessage("Player.ConfigUI.ValueUpdated", player, true));
                super.open();
            });
        }
    }

    @Override
    protected void handleCustomItemClick(int slot, ItemStack item, InventoryClickEvent event) {
        // Handle custom buttons if any were added in postSetMenuItems
    }

    @Override
    public void handleOpenMenu(InventoryOpenEvent e) {}

    @Override
    public void handleCloseMenu(InventoryCloseEvent e) {}
}
