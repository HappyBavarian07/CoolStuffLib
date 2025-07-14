package de.happybavarian07.coolstufflib.menusystem.misc;

import de.happybavarian07.coolstufflib.menusystem.Menu;
import de.happybavarian07.coolstufflib.menusystem.PlayerMenuUtility;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

public class CasinoMenu<T, R> extends Menu {
    private final List<T> itemPool;
    private final Function<T, ItemStack> animationFunction;
    private final Function<T, R> resultFunction;
    private final Consumer<R> resultHandler;
    private final Menu previousMenu;
    private final Random random = new Random();
    private int animationTicks = 0;
    private int finalIndex = -1;
    private T finalItem;
    private List<T> expandedPool;

    public CasinoMenu(PlayerMenuUtility playerMenuUtility,
                      List<T> itemPool,
                      Function<T, ItemStack> animationFunction,
                      Function<T, R> resultFunction,
                      Consumer<R> resultHandler,
                      Menu previousMenu) {
        super(playerMenuUtility);
        this.itemPool = itemPool;
        this.animationFunction = animationFunction;
        this.resultFunction = resultFunction;
        this.resultHandler = resultHandler;
        this.previousMenu = previousMenu;
        this.expandedPool = expandPool(itemPool);
    }

    @Override
    public String getMenuName() {
        return "Casino";
    }

    @Override
    public String getConfigMenuAddonFeatureName() {
        return "CasinoMenu";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        e.setCancelled(true);
        if (finalIndex != -1 && e.getRawSlot() == 13) {
            R result = resultFunction.apply(finalItem);
            if (resultHandler != null) resultHandler.accept(result);
            if (previousMenu != null) {
                previousMenu.open();
            } else {
                e.getWhoClicked().closeInventory();
            }
        }
    }

    @Override
    public void handleOpenMenu(InventoryOpenEvent e) {
        startAnimation();
    }

    @Override
    public void handleCloseMenu(InventoryCloseEvent e) {
        animationTicks = 0;
        finalIndex = -1;
        finalItem = null;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();
        if (!expandedPool.isEmpty()) {
            inventory.setItem(13, animationFunction.apply(expandedPool.get(0)));
        }
    }

    private void startAnimation() {
        animationTicks = 0;
        finalIndex = -1;
        finalItem = null;
        Bukkit.getScheduler().runTaskTimer(lib.getJavaPluginUsingLib(), () -> {
            if (animationTicks < 20) {
                int idx = random.nextInt(expandedPool.size());
                inventory.setItem(13, animationFunction.apply(expandedPool.get(idx)));
                animationTicks++;
            } else {
                finalIndex = random.nextInt(expandedPool.size());
                finalItem = expandedPool.get(finalIndex);
                ItemStack resultStack = animationFunction.apply(finalItem);
                inventory.setItem(13, resultStack);
                Bukkit.getScheduler().cancelTasks(lib.getJavaPluginUsingLib());
            }
        }, 0L, 5L);
    }

    private List<T> expandPool(List<T> pool) {
        int minSize = 9;
        if (pool.size() >= minSize) return pool;
        List<T> expanded = new ArrayList<>(pool);
        while (expanded.size() < minSize) {
            expanded.addAll(pool);
        }
        return expanded.subList(0, minSize);
    }
}
