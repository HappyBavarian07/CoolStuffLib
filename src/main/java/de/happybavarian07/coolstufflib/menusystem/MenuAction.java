package de.happybavarian07.coolstufflib.menusystem;/*
 * @Author HappyBavarian07
 * @Date 21.07.2024 | 12:33
 */

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public interface MenuAction {
    void execute(Player player, InventoryClickEvent event);
    // TODO: Add more methods to this interface
    // TODO: Implement this interface in the Menu Class and make it actually work haha
}
