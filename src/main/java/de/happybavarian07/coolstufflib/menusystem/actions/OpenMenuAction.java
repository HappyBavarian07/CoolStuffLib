package de.happybavarian07.coolstufflib.menusystem.actions;

import de.happybavarian07.coolstufflib.menusystem.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Opens a target menu and optionally executes a wrapped action first.
 */
public class OpenMenuAction implements MenuAction {
    private final Menu targetMenu;
    private final MenuAction beforeOpenAction;

    public OpenMenuAction(Menu targetMenu) {
        this(targetMenu, null);
    }

    public OpenMenuAction(Menu targetMenu, MenuAction beforeOpenAction) {
        this.targetMenu = targetMenu;
        this.beforeOpenAction = beforeOpenAction;
    }

    @Override
    public void execute(Player player, InventoryClickEvent event) {
        if (beforeOpenAction != null) beforeOpenAction.execute(player, event);
        if (targetMenu != null) targetMenu.open();
    }
}
