package de.happybavarian07.coolstufflib.menusystem;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;

public class MenuListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMenuClick(InventoryClickEvent e) {

        InventoryHolder holder = e.getInventory().getHolder();
        // If the inventory holder of the inventory clicked on
        // Is an instance of Menu, then gg.
        // The reason we can check if the holder is an instance of Menu is because the Menu Class implements InventoryHolder.
        if (holder instanceof Menu menu) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) {
                return;
            }
            int slot = e.getRawSlot();
            if (menu.slotActions.containsKey(slot)) {
                menu.slotActions.get(slot).execute((Player) e.getWhoClicked(), e);
                return;
            }
            menu.handleMenu(e);
            if(CoolStuffLib.getLib().getMenuAddonManager() == null) return;
            for (String menuAddonName : CoolStuffLib.getLib().getMenuAddonManager().getMenuAddons(menu.getConfigMenuAddonFeatureName()).keySet()) {
                MenuAddon addon = CoolStuffLib.getLib().getMenuAddonManager().getMenuAddons(menu.getConfigMenuAddonFeatureName()).get(menuAddonName);
                addon.handleMenu(e);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInvClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Menu) {
            Menu holder = (Menu) event.getInventory().getHolder();

            holder.handleCloseMenu(event);

            if (holder.getClass().isAssignableFrom(Listener.class)) HandlerList.unregisterAll((Listener) holder);

            if(CoolStuffLib.getLib().getMenuAddonManager() == null) return;
            for (String menuAddonName : CoolStuffLib.getLib().getMenuAddonManager().getMenuAddons(holder.getConfigMenuAddonFeatureName()).keySet()) {
                MenuAddon addon = CoolStuffLib.getLib().getMenuAddonManager().getMenuAddons(holder.getConfigMenuAddonFeatureName()).get(menuAddonName);
                addon.onCloseEvent();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInvOpen(InventoryOpenEvent event) {
        if (event.getInventory().getHolder() instanceof Menu) {
            Menu holder = (Menu) event.getInventory().getHolder();

            holder.handleOpenMenu(event);
        }
    }
}
