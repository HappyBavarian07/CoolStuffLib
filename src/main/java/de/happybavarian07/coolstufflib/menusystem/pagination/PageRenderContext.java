package de.happybavarian07.coolstufflib.menusystem.pagination;

import de.happybavarian07.coolstufflib.menusystem.Menu;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;

/**
 * <p>Immutable render context passed to {@link PageTransition} implementations.</p>
 *
 * @param <T> data type
 */
public class PageRenderContext<T> {
    private final Menu menu;
    private final Inventory inventory;
    private final List<T> data;
    private final int[] slots;
    private final int startIndexInclusive;
    private final int endIndexExclusive;
    private final PageTransition.ItemSupplier<T> itemSupplier;
    private final PageTransition.ItemPlacer itemPlacer;
    private final Map<Integer, Integer> slotToDataIndex;
    private final int animationTickDelay;

    /**
     * <p>Creates a new context.</p>
     * ...
     */
    public PageRenderContext(Menu menu,
                             Inventory inventory,
                             List<T> data,
                             int[] slots,
                             int startIndexInclusive,
                             int endIndexExclusive,
                             PageTransition.ItemSupplier<T> itemSupplier,
                             PageTransition.ItemPlacer itemPlacer,
                             Map<Integer, Integer> slotToDataIndex,
                             int animationTickDelay) {
        this.menu = menu;
        this.inventory = inventory;
        this.data = data;
        this.slots = slots;
        this.startIndexInclusive = startIndexInclusive;
        this.endIndexExclusive = endIndexExclusive;
        this.itemSupplier = itemSupplier;
        this.itemPlacer = itemPlacer;
        this.slotToDataIndex = slotToDataIndex;
        this.animationTickDelay = Math.max(1, animationTickDelay);
    }

    public Menu getMenu() { return menu; }
    public Inventory getInventory() { return inventory; }
    public List<T> getData() { return data; }
    public int[] getSlots() { return slots; }
    public int getStartIndexInclusive() { return startIndexInclusive; }
    public int getEndIndexExclusive() { return endIndexExclusive; }
    public PageTransition.ItemSupplier<T> getItemSupplier() { return itemSupplier; }
    public PageTransition.ItemPlacer getItemPlacer() { return itemPlacer; }
    public Map<Integer, Integer> getSlotToDataIndex() { return slotToDataIndex; }
    public int getAnimationTickDelay() { return animationTickDelay; }
}
