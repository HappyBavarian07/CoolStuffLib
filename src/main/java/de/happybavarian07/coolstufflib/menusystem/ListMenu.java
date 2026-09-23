package de.happybavarian07.coolstufflib.menusystem;

import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Function;

/**
 * <p>Ready-to-use paginated menu for a list of entries, without subclassing.</p>
 *
 * <pre><code>
 * new ListMenu&lt;&gt;(pmu, this, "&amp;8Players", 54,
 *         new ArrayList&lt;&gt;(Bukkit.getOnlinePlayers()),
 *         target -&gt; ItemUtils.createSkull(target.getName(), target.getName(), false),
 *         (player, target, event) -&gt; openProfile(target)).open();
 * </code></pre>
 *
 * @param <T> entry type
 */
public class ListMenu<T> extends PaginatedMenu<T> {
    private final String title;
    private final int size;

    public ListMenu(PlayerMenuUtility playerMenuUtility, Menu savedMenu, String title, int size,
                    List<T> entries, Function<T, ItemStack> renderer, EntryClickHandler<T> onClick) {
        super(playerMenuUtility, savedMenu);
        if (size < 27 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException("ListMenu size must be 27, 36, 45 or 54, got " + size);
        }
        this.title = title;
        this.size = size;
        setPaginatedData(entries, renderer, onClick);
    }

    @Override
    public String getMenuName() {
        return title;
    }

    @Override
    public String getConfigMenuAddonFeatureName() {
        return "ListMenu";
    }

    @Override
    public int getSlots() {
        return size;
    }

    @Override
    public void handleOpenMenu(InventoryOpenEvent e) {
    }

    @Override
    public void handleCloseMenu(InventoryCloseEvent e) {
    }
}
