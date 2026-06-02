package de.happybavarian07.coolstufflib.menusystem.actions;

import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import de.happybavarian07.coolstufflib.CoolStuffLib;

/**
 * A decorator for MenuAction that checks for a permission before executing the underlying action.
 */
public class PermissionAction implements MenuAction {
    private final String permission;
    private final MenuAction action;
    private final LanguageManager lgm;

    public PermissionAction(String permission, MenuAction action, LanguageManager lgm) {
        this.permission = permission;
        this.action = action;
        this.lgm = lgm;
    }

    @Override
    public void execute(Player player, InventoryClickEvent event) {
        if (player.hasPermission(permission)) {
            action.execute(player, event);
        } else {
            String noPerms = lgm.getMessage("Player.General.NoPermissions", player, true);
            player.sendMessage(noPerms);
        }
    }
}
