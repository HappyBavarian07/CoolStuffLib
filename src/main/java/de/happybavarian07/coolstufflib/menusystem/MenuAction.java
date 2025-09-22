package de.happybavarian07.coolstufflib.menusystem;/*
 * @Author HappyBavarian07
 * @Date 21.07.2024 | 12:33
 */

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

@FunctionalInterface
public interface MenuAction {
    void execute(Player player, InventoryClickEvent event);
}
