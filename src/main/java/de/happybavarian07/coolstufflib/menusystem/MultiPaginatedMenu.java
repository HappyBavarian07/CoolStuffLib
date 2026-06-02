package de.happybavarian07.coolstufflib.menusystem;

import de.happybavarian07.coolstufflib.menusystem.pagination.*;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Menu base class with multiple independent paginated zones.
 * <p>
 * Each zone can have its own data source, renderer, controls, direction, and transition strategy.
 */
public abstract class MultiPaginatedMenu extends Menu {
    protected final Map<String, Zone<?>> zones = new LinkedHashMap<>();
    protected final Map<Integer, ZoneClickRef> slotToZoneDataRef = new HashMap<>();
    protected int animationTickDelay = 1;

    public MultiPaginatedMenu(PlayerMenuUtility playerMenuUtility) {
        this(playerMenuUtility, null);
    }

    public MultiPaginatedMenu(PlayerMenuUtility playerMenuUtility, Menu savedMenu) {
        super(playerMenuUtility, savedMenu);
    }

    /**
     * Defines or replaces a zone.
     *
     * @param zoneId unique zone id
     * @param zone slot zone
     * @param data data source for this zone
     * @param renderer renderer for zone data
     * @param <T> zone data type
     */
    public <T> void defineZone(String zoneId, PaginationZone zone, List<T> data, Function<T, ItemStack> renderer) {
        zones.put(zoneId, new Zone<>(zoneId, zone, data, renderer));
    }

    /**
     * Sets a custom transition strategy for one zone.
     */
    public void setZoneTransition(String zoneId, PageTransition<?> transition) {
        Zone<?> zone = zones.get(zoneId);
        if (zone != null && transition != null) zone.transition = transition;
    }

    /**
     * Sets logical page direction for one zone.
     */
    public void setZonePageDirection(String zoneId, PageDirection pageDirection) {
        Zone<?> zone = zones.get(zoneId);
        if (zone != null && pageDirection != null) zone.pageDirection = pageDirection;
    }

    /**
     * Sets navigation controls for one zone.
     */
    public void setZoneControls(String zoneId, PageControlLayout layout) {
        Zone<?> zone = zones.get(zoneId);
        if (zone != null) zone.controls = layout;
    }

    /**
     * Sets global animation tick delay for zone transitions.
     */
    public void setAnimationTickDelay(int animationTickDelay) {
        this.animationTickDelay = Math.max(1, animationTickDelay);
    }

    @Override
    public void setFillerGlass() {
        super.setFillerGlass(getAutoExcludedFillerSlots());
    }

    @Override
    public void setMenuItems() {
        preSetMenuItems();
        slotToZoneDataRef.clear();
        for (Zone<?> zone : zones.values()) {
            renderZone(zone);
        }
        postSetMenuItems();
    }

    /**
     * Hook called before all zone rendering.
     */
    protected abstract void preSetMenuItems();

    /**
     * Hook called after all zone rendering.
     */
    protected abstract void postSetMenuItems();

    /**
     * Handles a click on a paginated item in a specific zone.
     */
    protected abstract void handleZoneItemClick(String zoneId, Object data, int dataIndex, int slot, ItemStack item, InventoryClickEvent event);

    /**
     * Handles clicks outside paginated zone items and controls.
     */
    protected abstract void handleOutsideZoneClick(int slot, ItemStack item, InventoryClickEvent event);

    @Override
    public void handleMenu(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        for (Zone<?> zone : zones.values()) {
            if (zone.controls != null) {
                if (slot == zone.controls.getCloseSlot()) {
                    closeAndReturnOrClose();
                    return;
                }
                if (slot == zone.controls.getRefreshSlot()) {
                    open();
                    return;
                }
                if (slot == zone.controls.getPreviousSlot()) {
                    turnZonePage(zone, true);
                    return;
                }
                if (slot == zone.controls.getNextSlot()) {
                    turnZonePage(zone, false);
                    return;
                }
            }
        }

        ZoneClickRef ref = slotToZoneDataRef.get(slot);
        if (ref != null) {
            handleZoneItemClick(ref.zoneId, ref.data, ref.dataIndex, slot, item, event);
            return;
        }
        handleOutsideZoneClick(slot, item, event);
    }

    @SuppressWarnings("unchecked")
    private <T> void renderZone(Zone<T> zone) {
        int[] rawSlots = zone.zone.getSlots();
        int[] slots = filterValidSlots(rawSlots);
        if (slots.length == 0 || zone.data == null || zone.renderer == null) return;
        int start = zone.page * slots.length;
        int end = Math.min(start + slots.length, zone.data.size());
        zone.maxItemsPerPage = slots.length;
        PageRenderContext<T> context = new PageRenderContext<>(
                this, inventory, zone.data, slots, start, end,
                (menu, data, dataIndex, slot) -> {
                    ItemStack stack = zone.renderer.apply(data);
                    if (stack == null) return null;
                    ItemMeta meta = stack.getItemMeta();
                    if (meta != null) {
                        meta.getPersistentDataContainer().set(zone.zoneKey, PersistentDataType.STRING, zone.id + ":" + dataIndex);
                        stack.setItemMeta(meta);
                    }
                    return stack;
                },
                (inv, slot, item, dataIndex, map) -> {
                    inv.setItem(slot, item);
                    map.put(slot, dataIndex);
                    slotToZoneDataRef.put(slot, new ZoneClickRef(zone.id, zone.data.get(dataIndex), dataIndex));
                },
                zone.slotToDataIndex,
                animationTickDelay
        );
        ((PageTransition<T>) zone.transition).render(context);
    }

    private int[] filterValidSlots(int[] slots) {
        int count = 0;
        for (int slot : slots) if (slot >= 0 && slot < getSlots()) count++;
        int[] valid = new int[count];
        int idx = 0;
        for (int slot : slots) if (slot >= 0 && slot < getSlots()) valid[idx++] = slot;
        return valid;
    }

    private void turnZonePage(Zone<?> zone, boolean previous) {
        boolean goPrev = zone.pageDirection == PageDirection.REVERSED ? !previous : previous;
        if (goPrev) {
            if (zone.page > 0) zone.page--;
        } else {
            int nextStart = (zone.page + 1) * zone.maxItemsPerPage;
            if (zone.data != null && nextStart < zone.data.size()) zone.page++;
        }
        open();
    }

    protected static class Zone<T> {
        private final String id;
        private final PaginationZone zone;
        private final NamespacedKey zoneKey;
        private final List<T> data;
        private final Function<T, ItemStack> renderer;
        private final Map<Integer, Integer> slotToDataIndex = new HashMap<>();
        private PageTransition<?> transition = new InstantPageTransition<>();
        private PageDirection pageDirection = PageDirection.NORMAL;
        private PageControlLayout controls;
        private int page = 0;
        private int maxItemsPerPage = 1;

        private Zone(String id, PaginationZone zone, List<T> data, Function<T, ItemStack> renderer) {
            this.id = id;
            this.zone = zone;
            this.data = data;
            this.renderer = renderer;
            this.zoneKey = new NamespacedKey("coolstufflib-menusystem", "multi_paginated_item_" + id);
        }
    }

    private Set<Integer> getAutoExcludedFillerSlots() {
        Set<Integer> excluded = new HashSet<>();
        for (Zone<?> zone : zones.values()) {
            for (int slot : zone.zone.getSlots()) {
                if (slot >= 0 && slot < getSlots()) excluded.add(slot);
            }
            if (zone.controls != null) {
                if (zone.controls.getPreviousSlot() >= 0 && zone.controls.getPreviousSlot() < getSlots()) excluded.add(zone.controls.getPreviousSlot());
                if (zone.controls.getCloseSlot() >= 0 && zone.controls.getCloseSlot() < getSlots()) excluded.add(zone.controls.getCloseSlot());
                if (zone.controls.getNextSlot() >= 0 && zone.controls.getNextSlot() < getSlots()) excluded.add(zone.controls.getNextSlot());
                if (zone.controls.getRefreshSlot() >= 0 && zone.controls.getRefreshSlot() < getSlots()) excluded.add(zone.controls.getRefreshSlot());
            }
        }
        return excluded;
    }

    protected static class ZoneClickRef {
        private final String zoneId;
        private final Object data;
        private final int dataIndex;

        private ZoneClickRef(String zoneId, Object data, int dataIndex) {
            this.zoneId = zoneId;
            this.data = data;
            this.dataIndex = dataIndex;
        }
    }
}
