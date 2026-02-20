package de.happybavarian07.coolstufflib.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

public class InventoryViewWrapper implements InventoryView {

    private static final Logger LOGGER = Bukkit.getLogger();

    private final Object wrappedView;
    private final Class<?> wrappedClass;
    private Inventory cachedTopInventory;
    private Inventory cachedBottomInventory;
    private HumanEntity cachedPlayer;

    public InventoryViewWrapper(Object wrappedView) {
        this.wrappedView = wrappedView;
        this.wrappedClass = wrappedView.getClass();
        cacheInventories();
    }

    private void cacheInventories() {
        try {
            var topMethod = wrappedClass.getMethod("getTopInventory");
            topMethod.setAccessible(true);
            cachedTopInventory = (Inventory) topMethod.invoke(wrappedView);

            var bottomMethod = wrappedClass.getMethod("getBottomInventory");
            bottomMethod.setAccessible(true);
            cachedBottomInventory = (Inventory) bottomMethod.invoke(wrappedView);

            var playerMethod = wrappedClass.getMethod("getPlayer");
            playerMethod.setAccessible(true);
            cachedPlayer = (HumanEntity) playerMethod.invoke(wrappedView);
        } catch (Exception e) {
            LOGGER.warning("[CoolStuffLib] Failed to cache inventories from wrapped view: " + e.getMessage());
        }
    }

    private Object invokeMethod(String methodName, Class<?>[] paramTypes, Object... args) {
        try {
            var method = wrappedClass.getMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(wrappedView, args);
        } catch (Exception e) {
            LOGGER.fine("[CoolStuffLib] Failed to invoke " + methodName + " on wrapped InventoryView: " + e.getMessage());
            return null;
        }
    }

    @Override
    @NotNull
    public Inventory getTopInventory() {
        return cachedTopInventory != null ? cachedTopInventory : cachedBottomInventory;
    }

    @Override
    @NotNull
    public Inventory getBottomInventory() {
        return cachedBottomInventory != null ? cachedBottomInventory : cachedTopInventory;
    }

    @Override
    @NotNull
    public HumanEntity getPlayer() {
        return cachedPlayer;
    }

    @Override
    @NotNull
    public InventoryType getType() {
        Object result = invokeMethod("getType", new Class<?>[0]);
        return result instanceof InventoryType ? (InventoryType) result : InventoryType.CHEST;
    }

    @Override
    @Nullable
    public ItemStack getItem(int slot) {
        Object result = invokeMethod("getItem", new Class<?>[]{int.class}, slot);
        return result instanceof ItemStack ? (ItemStack) result : null;
    }

    @Override
    public void setCursor(@Nullable ItemStack itemStack) {
        invokeMethod("setCursor", new Class<?>[]{ItemStack.class}, itemStack);
    }

    @Override
    public @Nullable ItemStack getCursor() {
        return invokeMethod("getCursor", new Class<?>[0]) instanceof ItemStack ? (ItemStack) invokeMethod("getCursor", new Class<?>[0]) : null;
    }

    @Override
    public @Nullable Inventory getInventory(int i) {
        return invokeMethod("getInventory", new Class<?>[]{int.class}, i) instanceof Inventory ? (Inventory) invokeMethod("getInventory", new Class<?>[]{int.class}, i) : null;
    }

    @Override
    public int convertSlot(int i) {
        Object result = invokeMethod("convertSlot", new Class<?>[]{int.class}, i);
        return result instanceof Integer ? (Integer) result : -1;
    }

    @Override
    public @NotNull InventoryType.SlotType getSlotType(int i) {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public int countSlots() {
        return 0;
    }

    @Override
    public boolean setProperty(@NotNull InventoryView.Property property, int i) {
        return false;
    }

    @Override
    public void setItem(int slot, @Nullable ItemStack item) {
        invokeMethod("setItem", new Class<?>[]{int.class, ItemStack.class}, slot, item);
    }

    @Override
    @NotNull
    public String getTitle() {
        Object result = invokeMethod("getTitle", new Class<?>[0]);
        return result instanceof String ? (String) result : "";
    }

    @Override
    @NotNull
    public String getOriginalTitle() {
        try {
            Object result = invokeMethod("getOriginalTitle", new Class<?>[0]);
            return result instanceof String ? (String) result : getTitle();
        } catch (Exception e) {
            return getTitle();
        }
    }

    @Override
    public void setTitle(@NotNull String title) {
        try {
            invokeMethod("setTitle", new Class<?>[]{String.class}, title);
        } catch (Exception e) {
            LOGGER.fine("[CoolStuffLib] setTitle not supported on this server version");
        }
    }

    public Object getWrappedView() {
        return wrappedView;
    }
}

