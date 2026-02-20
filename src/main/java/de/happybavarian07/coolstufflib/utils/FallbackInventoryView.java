package de.happybavarian07.coolstufflib.utils;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FallbackInventoryView extends InventoryView {

    private final HumanEntity player;
    private final Inventory topInventory;
    private final Inventory bottomInventory;

    public FallbackInventoryView(HumanEntity player, Inventory topInventory) {
        this.player = player;
        this.topInventory = topInventory;
        this.bottomInventory = player.getInventory();
    }

    @Override
    @NotNull
    public Inventory getTopInventory() {
        return topInventory;
    }

    @Override
    @NotNull
    public Inventory getBottomInventory() {
        return bottomInventory;
    }

    @Override
    @NotNull
    public HumanEntity getPlayer() {
        return player;
    }

    @Override
    @NotNull
    public InventoryType getType() {
        return topInventory.getType();
    }

    @Override
    @Nullable
    public ItemStack getItem(int slot) {
        if (slot < topInventory.getSize()) {
            return topInventory.getItem(slot);
        } else {
            return bottomInventory.getItem(slot - topInventory.getSize());
        }
    }

    @Override
    public void setItem(int slot, @Nullable ItemStack item) {
        if (slot < topInventory.getSize()) {
            topInventory.setItem(slot, item);
        } else {
            bottomInventory.setItem(slot - topInventory.getSize(), item);
        }
    }

    @Override
    @NotNull
    public String getTitle() {
        if (topInventory instanceof org.bukkit.inventory.InventoryHolder) {
            return "Inventory";
        }
        return topInventory.getType().getDefaultTitle();
    }

    @Override
    @NotNull
    public String getOriginalTitle() {
        return getTitle();
    }

    @Override
    public void setTitle(@NotNull String s) {
        // Titles cannot be changed in this fallback implementation
    }
}

