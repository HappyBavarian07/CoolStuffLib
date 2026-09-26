package de.happybavarian07.coolstufflib.menusystem.pagination;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ItemStack;

/**
 * Renders page items one-by-one on a repeating scheduler task.
 *
 * @param <T> data type
 */
public class SequentialPageTransition<T> implements PageTransition<T> {
    @Override
    public void render(PageRenderContext<T> context) {
        for (int slot : context.getSlots()) {
            context.getInventory().setItem(slot, null);
            context.getSlotToDataIndex().remove(slot);
        }

        final int generation = context.getMenu().getRenderGeneration();
        final int[] slotIndex = {0};
        final int[] dataIndex = {context.getStartIndexInclusive()};
        new BukkitRunnable() {
            @Override
            public void run() {
                    if (context.getMenu().getRenderGeneration() != generation
                            || slotIndex[0] >= context.getSlots().length || dataIndex[0] >= context.getEndIndexExclusive()) {
                        cancel();
                        return;
                    }
                    while (dataIndex[0] < context.getEndIndexExclusive() && slotIndex[0] < context.getSlots().length) {
                        Object data = context.getData().get(dataIndex[0]);
                        if (data != null) {
                            int slot = context.getSlots()[slotIndex[0]++];
                            ItemStack item = context.getItemSupplier().create(context.getMenu(), (T) data, dataIndex[0], slot);
                            if (item != null) {
                                context.getItemPlacer().place(context.getInventory(), slot, item, dataIndex[0], context.getSlotToDataIndex());
                            }
                            dataIndex[0]++;
                            break;
                        }
                        dataIndex[0]++;
                    }
                if (slotIndex[0] >= context.getSlots().length || dataIndex[0] >= context.getEndIndexExclusive()) {
                    cancel();
                }
            }
        }.runTaskTimer(CoolStuffLib.getLib().getJavaPluginUsingLib(), 0L, context.getAnimationTickDelay());
    }
}
