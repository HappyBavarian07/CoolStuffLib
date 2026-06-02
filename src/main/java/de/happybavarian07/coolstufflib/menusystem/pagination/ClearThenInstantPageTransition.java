package de.happybavarian07.coolstufflib.menusystem.pagination;

/**
 * Clears all target slots first, then renders instantly.
 *
 * @param <T> data type
 */
public class ClearThenInstantPageTransition<T> extends InstantPageTransition<T> {
    @Override
    public void render(PageRenderContext<T> context) {
        for (int slot : context.getSlots()) {
            context.getInventory().setItem(slot, null);
            context.getSlotToDataIndex().remove(slot);
        }
        super.render(context);
    }
}
