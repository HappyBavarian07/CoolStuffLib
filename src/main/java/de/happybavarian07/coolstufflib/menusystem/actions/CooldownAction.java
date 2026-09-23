package de.happybavarian07.coolstufflib.menusystem.actions;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wraps another action and blocks repeated execution for a configurable cooldown.
 */
public class CooldownAction implements MenuAction {
    private final long cooldownMillis;
    private final MenuAction action;
    private final Map<UUID, Long> lastExecutionByPlayer = new ConcurrentHashMap<>();
    private final String cooldownMessage;

    public CooldownAction(long cooldownMillis, MenuAction action) {
        this(cooldownMillis, action, null);
    }

    public CooldownAction(long cooldownMillis, MenuAction action, String cooldownMessage) {
        this.cooldownMillis = Math.max(0L, cooldownMillis);
        this.action = action;
        this.cooldownMessage = cooldownMessage;
    }

    @Override
    public void execute(Player player, InventoryClickEvent event) {
        if (action == null) return;
        long now = System.currentTimeMillis();
        lastExecutionByPlayer.values().removeIf(time -> now - time >= cooldownMillis);
        long last = lastExecutionByPlayer.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < cooldownMillis) {
            if (cooldownMessage != null && !cooldownMessage.isEmpty()) player.sendMessage(cooldownMessage);
            return;
        }
        lastExecutionByPlayer.put(player.getUniqueId(), now);
        action.execute(player, event);
    }
}
