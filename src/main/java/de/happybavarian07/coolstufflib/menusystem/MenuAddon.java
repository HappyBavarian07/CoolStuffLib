package de.happybavarian07.coolstufflib.menusystem;

import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * <p>Base class for menu addons.</p>
 */
public abstract class MenuAddon {

    public MenuAddon() {}

    public abstract Menu getMenu();

    public abstract String getName();

    /**
     * <p>Sets items for the addon.</p>
     */
    public abstract void setMenuAddonItems();

    /**
     * <p>Handles clicks within the addon.</p>
     *
     * @param event The click event
     */
    public abstract void handleMenu(InventoryClickEvent event);

    /**
     * <p>Called when the menu opens.</p>
     */
    public abstract void onOpenEvent();

    /**
     * <p>Called when the menu closes.</p>
     */
    public abstract void onCloseEvent();
}
