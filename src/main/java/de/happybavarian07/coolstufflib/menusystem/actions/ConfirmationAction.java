package de.happybavarian07.coolstufflib.menusystem.actions;

import de.happybavarian07.coolstufflib.menusystem.Menu;
import de.happybavarian07.coolstufflib.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * A decorator for MenuAction that triggers a confirmation menu before executing the action.
 * Without an explicit return menu, the menu that was clicked is reopened afterwards.
 */
public class ConfirmationAction implements MenuAction {
    private final String reason;
    private final MenuAction action;
    private final Menu savedMenu;

    public ConfirmationAction(String reason, MenuAction action) {
        this(reason, action, null);
    }

    public ConfirmationAction(String reason, MenuAction action, Menu savedMenu) {
        this.reason = reason;
        this.action = action;
        this.savedMenu = savedMenu;
    }

    @Override
    public void execute(Player player, InventoryClickEvent event) {
        Menu returnTo = savedMenu;
        if (returnTo == null && event != null && event.getInventory().getHolder() instanceof Menu clicked) {
            returnTo = clicked;
        }
        Utils.openConfirmationMenu(reason, action, player, returnTo);
    }
}
