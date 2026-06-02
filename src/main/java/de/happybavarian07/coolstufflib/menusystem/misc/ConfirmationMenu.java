package de.happybavarian07.coolstufflib.menusystem.misc;/*
 * @Author HappyBavarian07
 * @Date 04.11.2023 | 13:27
 */

import de.happybavarian07.coolstufflib.languagemanager.PlaceholderType;
import de.happybavarian07.coolstufflib.menusystem.Menu;
import de.happybavarian07.coolstufflib.menusystem.actions.MenuAction;
import de.happybavarian07.coolstufflib.menusystem.PlayerMenuUtility;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

public class ConfirmationMenu extends Menu {
    public ConfirmationMenu(PlayerMenuUtility playerMenuUtility, Menu savedMenu) {
        super(playerMenuUtility, savedMenu);
    }

    @Override
    public String getMenuName() {
        return lgm.getMenuTitle("Misc.ConfirmationMenu", playerMenuUtility.getOwner());
    }

    @Override
    public String getConfigMenuAddonFeatureName() {
        return "ConfirmationMenu";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        // Actions are handled directly via setItemWithAction
    }

    @Override
    public void handleOpenMenu(InventoryOpenEvent e) {

    }

    @Override
    public void handleCloseMenu(InventoryCloseEvent e) {

    }

    @Override
    public void setMenuItems() {
        Player player = playerMenuUtility.getOwner();
        // Retrieve items using the getItem method with appropriate paths.
        ItemStack confirmItem = lgm.getItem("General.ConfirmationMenu.Confirm", player, false);
        ItemStack cancelItem = lgm.getItem("General.ConfirmationMenu.Cancel", player, false);

        // Define Actions
        MenuAction confirmAction = (p, event) -> {
            MenuAction storedAction = (MenuAction) playerMenuUtility.getData("ConfirmationMenu_Action");
            if (storedAction != null) {
                storedAction.execute(p, event);
            }
            closeAndReturnOrClose();
        };
        
        MenuAction cancelAction = (p, event) -> closeAndReturnOrClose();

        // Calculate the middle row based on the number of slots in the menu
        int middleRow = getSlots() / 9 / 2;

        // Calculate the number of slots for the left and right sides
        int leftSideSlots = middleRow * 9;
        int rightSideSlots = middleRow * 9;

        // Fill the left side with Confirm buttons
        for (int i = 0; i < leftSideSlots; i++) {
            if (i % 9 != 4) { // Skip the middle column
                setItemWithAction(i, confirmItem, confirmAction);
            }
        }

        // Fill the right side with Cancel buttons
        for (int i = getSlots() - 1; i >= getSlots() - rightSideSlots; i--) {
            if (i % 9 != 4) { // Skip the middle column
                setItemWithAction(i, cancelItem, cancelAction);
            }
        }

        // Place the reason item in the middle row
        lgm.addPlaceholder(PlaceholderType.ITEM, "%reason%", playerMenuUtility.getData("ConfirmationMenu_Reason"), true);
        inventory.setItem(4 + middleRow * 9, lgm.getItem("General.ConfirmationMenu.ReasonItem", player, true));
    }
}
