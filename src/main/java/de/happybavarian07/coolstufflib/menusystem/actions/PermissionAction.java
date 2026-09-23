package de.happybavarian07.coolstufflib.menusystem.actions;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * A decorator for MenuAction that checks for a permission before executing the underlying action.
 */
public class PermissionAction implements MenuAction {
    private final String permission;
    private final MenuAction action;
    private final LanguageManager lgm;

    /**
     * Uses the library's language manager for the no-permission message.
     */
    public PermissionAction(String permission, MenuAction action) {
        this(permission, action, null);
    }

    public PermissionAction(String permission, MenuAction action, LanguageManager lgm) {
        this.permission = permission;
        this.action = action;
        this.lgm = lgm;
    }

    @Override
    public void execute(Player player, InventoryClickEvent event) {
        if (player.hasPermission(permission)) {
            if (action != null) action.execute(player, event);
            return;
        }
        LanguageManager messages = lgm != null ? lgm : CoolStuffLib.getLib().getLanguageManager();
        player.sendMessage(messages.getMessage("Player.General.NoPermissions", player, true));
    }
}
