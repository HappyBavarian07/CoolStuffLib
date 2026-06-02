package de.happybavarian07.coolstufflib.menusystem.actions;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Executes multiple {@link MenuAction}s in sequence.
 */
public class CompositeAction implements MenuAction {
    private final List<MenuAction> actions = new ArrayList<>();

    public CompositeAction(MenuAction... actions) {
        if (actions != null) this.actions.addAll(Arrays.asList(actions));
    }

    @Override
    public void execute(Player player, InventoryClickEvent event) {
        for (MenuAction action : actions) {
            if (action != null) action.execute(player, event);
        }
    }
}
