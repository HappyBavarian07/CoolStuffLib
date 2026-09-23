package de.happybavarian07.coolstufflib.menusystem.actions;

import de.happybavarian07.coolstufflib.utils.CooldownTracker;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Wraps another action and blocks repeated execution for a configurable cooldown.
 */
public class CooldownAction implements MenuAction {
    private final CooldownTracker tracker;
    private final MenuAction action;
    private final String cooldownMessage;

    public CooldownAction(long cooldownMillis, MenuAction action) {
        this(cooldownMillis, action, null);
    }

    public CooldownAction(long cooldownMillis, MenuAction action, String cooldownMessage) {
        this.tracker = new CooldownTracker(cooldownMillis);
        this.action = action;
        this.cooldownMessage = cooldownMessage;
    }

    @Override
    public void execute(Player player, InventoryClickEvent event) {
        if (action == null) return;
        if (!tracker.tryUse(player.getUniqueId())) {
            if (cooldownMessage != null && !cooldownMessage.isEmpty()) player.sendMessage(cooldownMessage);
            return;
        }
        action.execute(player, event);
    }
}
