package de.happybavarian07.coolstufflib.menusystem.pagination;

import org.bukkit.inventory.ItemStack;

/**
 * Renders a page immediately without clearing or animation.
 *
 * @param <T> data type
 */
public class InstantPageTransition<T> implements PageTransition<T> {
    @Override
    public void render(PageRenderContext<T> context) {
        int slotIndex = 0;
        for (int dataIndex = context.getStartIndexInclusive();
             dataIndex < context.getEndIndexExclusive() && slotIndex < context.getSlots().length;
             dataIndex++) {
            T data = context.getData().get(dataIndex);
            if (data == null) continue;
            int slot = context.getSlots()[slotIndex++];
            ItemStack item = context.getItemSupplier().create(context.getMenu(), data, dataIndex, slot);
            if (item == null) continue;
            context.getItemPlacer().place(context.getInventory(), slot, item, dataIndex, context.getSlotToDataIndex());
        }
    }
}
