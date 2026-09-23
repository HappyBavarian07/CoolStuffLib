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

/**
 * <p>Handles inventory events for menus.</p>
 */
public class MenuListener implements Listener {

    /**
     * <p>Handles inventory click events for menus.</p>
     *
     * @param e The event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMenuClick(InventoryClickEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();
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
            MenuAddonManager addonManager = CoolStuffLib.getLib().getMenuAddonManager();
            if (addonManager == null) return;
            for (MenuAddon addon : addonManager.getMenuAddons(menu.getConfigMenuAddonFeatureName()).values()) {
                addon.handleMenu(e);
            }
        }
    }

    /**
     * <p>Handles inventory close events for menus.</p>
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInvClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Menu holder) {
            holder.handleCloseMenu(event);

            if (holder instanceof Listener listener) HandlerList.unregisterAll(listener);

            MenuAddonManager addonManager = CoolStuffLib.getLib().getMenuAddonManager();
            if (addonManager == null) return;
            for (MenuAddon addon : addonManager.getMenuAddons(holder.getConfigMenuAddonFeatureName()).values()) {
                addon.onCloseEvent();
            }
        }
    }

    /**
     * <p>Handles inventory open events for menus.</p>
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInvOpen(InventoryOpenEvent event) {
        if (event.getInventory().getHolder() instanceof Menu holder) {
            holder.handleOpenMenu(event);
        }
    }
}
