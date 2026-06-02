package de.happybavarian07.coolstufflib.menusystem.actions;/*
 * @Author HappyBavarian07
 * @Date 21.07.2024 | 12:33
 */

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Functional menu click action.
 */
@FunctionalInterface
public interface MenuAction {
    /**
     * Executes the action.
     *
     * @param player clicker
     * @param event click event
     */
    void execute(Player player, InventoryClickEvent event);
}
