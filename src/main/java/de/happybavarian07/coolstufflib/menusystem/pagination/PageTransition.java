package de.happybavarian07.coolstufflib.menusystem.pagination;

import de.happybavarian07.coolstufflib.menusystem.Menu;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Strategy interface for rendering paginated data into inventory slots.
 *
 * @param <T> data type
 */
public interface PageTransition<T> {
    /**
     * Renders the current page.
     *
     * @param context rendering context
     */
    void render(PageRenderContext<T> context);

    /**
     * Produces an item for one data object.
     */
    interface ItemSupplier<T> {
        ItemStack create(Menu menu, T data, int dataIndex, int slot);
    }

    /**
     * Applies one rendered item to the inventory and updates slot/data mapping.
     */
    interface ItemPlacer {
        void place(Inventory inventory, int slot, ItemStack item, int dataIndex, Map<Integer, Integer> slotToDataIndex);
    }
}
