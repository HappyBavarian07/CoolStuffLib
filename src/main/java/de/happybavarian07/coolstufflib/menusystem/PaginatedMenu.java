package de.happybavarian07.coolstufflib.menusystem;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.menusystem.pagination.InstantPageTransition;
import de.happybavarian07.coolstufflib.menusystem.pagination.NavigationDirection;
import de.happybavarian07.coolstufflib.menusystem.pagination.PageControlLayout;
import de.happybavarian07.coolstufflib.menusystem.pagination.PageDirection;
import de.happybavarian07.coolstufflib.menusystem.pagination.PageRenderContext;
import de.happybavarian07.coolstufflib.menusystem.pagination.PageTransition;
import de.happybavarian07.coolstufflib.menusystem.pagination.PaginationZone;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public abstract class PaginatedMenu<T> extends Menu {
    private final NamespacedKey itemKey = new NamespacedKey("coolstufflib-menusystem", "paginated_item");
    protected int page = 0;
    protected int maxItemsPerPage;
    protected int index = 0;
    protected List<T> paginatedData;
    protected Function<T, ItemStack> itemRenderer;
    protected EntryClickHandler<T> entryClickHandler;
    protected PaginationZone paginationZone;
    protected final Map<Integer, Integer> paginatedSlotToDataIndex = new HashMap<>();
    protected NavigationDirection navigationDirection = NavigationDirection.HORIZONTAL;
    protected PageDirection pageDirection = PageDirection.NORMAL;
    protected PageTransition<T> pageTransition = new InstantPageTransition<>();
    protected int animationTickDelay = 1;
    protected PageControlLayout controlLayout;
    protected boolean autoDrawDefaultBorder = true;

    public PaginatedMenu(PlayerMenuUtility playerMenuUtility) {
        this(playerMenuUtility, null);
    }

    public PaginatedMenu(PlayerMenuUtility playerMenuUtility, Menu savedMenu) {
        super(playerMenuUtility, savedMenu);
        int slots = getSlots();
        this.maxItemsPerPage = slots % 9 == 0 ? slots : 0;
    }

    public void setPaginatedData(List<T> data, Function<T, ItemStack> renderer) {
        this.paginatedData = data;
        this.itemRenderer = renderer;
        int slots = getSlots();
        this.maxItemsPerPage = slots % 9 == 0 ? slots : 0;
    }

    /**
     * <p>Sets the entries, how each entry is rendered and what happens when an entry is clicked.
     * Page item clicks then go to {@code onClick} instead of {@link #handlePageItemClick}.</p>
     *
     * <pre><code>setPaginatedData(players, this::head, (player, target, event) -&gt; openProfile(target));</code></pre>
     */
    public void setPaginatedData(List<T> data, Function<T, ItemStack> renderer, EntryClickHandler<T> onClick) {
        setPaginatedData(data, renderer);
        this.entryClickHandler = onClick;
    }

    public void setPaginationZone(PaginationZone paginationZone) {
        this.paginationZone = paginationZone;
    }

    public PaginationZone getPaginationZone() {
        return paginationZone;
    }

    @Override
    public void setFillerGlass() {
        super.setFillerGlass(getAutoExcludedFillerSlots());
    }

    public void setNavigationDirection(NavigationDirection navigationDirection) {
        this.navigationDirection = navigationDirection == null ? NavigationDirection.HORIZONTAL : navigationDirection;
    }

    public void setPageDirection(PageDirection pageDirection) {
        this.pageDirection = pageDirection == null ? PageDirection.NORMAL : pageDirection;
    }

    public void setPageTransition(PageTransition<T> pageTransition) {
        this.pageTransition = pageTransition == null ? new InstantPageTransition<>() : pageTransition;
    }

    public void setAnimationTickDelay(int animationTickDelay) {
        this.animationTickDelay = Math.max(1, animationTickDelay);
    }

    public void setControlLayout(PageControlLayout controlLayout) {
        this.controlLayout = controlLayout;
    }

    public void setAutoDrawDefaultBorder(boolean autoDrawDefaultBorder) {
        this.autoDrawDefaultBorder = autoDrawDefaultBorder;
    }

    //Set the border and menu buttons for the menu

    /**
     * The addMenuBorder function adds the border to the menu.
     * It does this by setting all the items in slots 0-9, 17, 18, 26-36 and 44-53 to a filler item.
     * It also sets all the items in slots 48, 49, 50 and 51 to their respective border items (Left arrow for slot 48 etc.)
     */
    public void addMenuBorder() {
        ensurePrepared();
        int size = getSlots();
        if (size % 9 != 0) return;
        int rows = size / 9;
        int bottomRowStart = (rows - 1) * 9;
        int leftBtnSlot = controlLayout == null ? getSlot("General.Left", bottomRowStart + 3) : controlLayout.getPreviousSlot();
        int closeBtnSlot = controlLayout == null ? getSlot("General.Close", bottomRowStart + 4) : controlLayout.getCloseSlot();
        int rightBtnSlot = controlLayout == null ? getSlot("General.Right", bottomRowStart + 5) : controlLayout.getNextSlot();
        int refreshBtnSlot = controlLayout == null ? getSlot("General.Refresh", bottomRowStart + 6) : controlLayout.getRefreshSlot();
        String prevKey = navigationDirection == NavigationDirection.VERTICAL ? "General.Up" : "General.Left";
        String nextKey = navigationDirection == NavigationDirection.VERTICAL ? "General.Down" : "General.Right";
        ItemStack prevItem = lgm.getItem(prevKey, null, false);
        ItemStack nextItem = lgm.getItem(nextKey, null, false);
        if (prevItem == null) prevItem = lgm.getItem("General.Left", null, false);
        if (nextItem == null) nextItem = lgm.getItem("General.Right", null, false);
        if (isValidSlot(leftBtnSlot)) inventory.setItem(leftBtnSlot, prevItem);
        if (isValidSlot(closeBtnSlot)) inventory.setItem(closeBtnSlot, lgm.getItem("General.Close", null, false));
        if (isValidSlot(rightBtnSlot)) inventory.setItem(rightBtnSlot, nextItem);
        if (isValidSlot(refreshBtnSlot)) inventory.setItem(refreshBtnSlot, lgm.getItem("General.Refresh", null, false));
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            boolean isTopRow = row == 0;
            boolean isBottomRow = row == rows - 1;
            boolean isLeftCol = col == 0;
            boolean isRightCol = col == 8;
            boolean isControlSlot = i == leftBtnSlot || i == closeBtnSlot || i == rightBtnSlot || i == refreshBtnSlot;
            if ((isTopRow || isBottomRow || isLeftCol || isRightCol) && !isControlSlot) {
                inventory.setItem(i, super.FILLER);
            }
        }
    }

    protected int[] getPaginatedItemSlots() {
        if (paginationZone != null) {
            int[] configuredSlots = paginationZone.getSlots();
            int size = getSlots();
            int validCount = 0;
            for (int slot : configuredSlots) {
                if (slot >= 0 && slot < size) {
                    validCount++;
                }
            }
            int[] validSlots = new int[validCount];
            int idx = 0;
            for (int slot : configuredSlots) {
                if (slot >= 0 && slot < size) {
                    validSlots[idx++] = slot;
                }
            }
            return validSlots;
        }

        int size = getSlots();
        if (size % 9 != 0) return new int[0];
        int rows = size / 9;
        if (rows < 3) return new int[0];
        int startRow = 1;
        int endRow = rows - 2;
        if (rows == 3) endRow = 1;
        int count = 0;
        for (int row = startRow; row <= endRow; row++) {
            for (int col = 1; col < 8; col++) {
                count++;
            }
        }
        int[] slots = new int[count];
        int idx = 0;
        for (int row = startRow; row <= endRow; row++) {
            for (int col = 1; col < 8; col++) {
                slots[idx++] = row * 9 + col;
            }
        }
        return slots;
    }

    @Override
    public void setMenuItems() {
        preSetMenuItems();
        paginatedSlotToDataIndex.clear();
        if (paginatedData != null && itemRenderer != null) {
            if (autoDrawDefaultBorder) addMenuBorder();
            int[] slots = getPaginatedItemSlots();
            if (slots.length == 0) return;
            maxItemsPerPage = slots.length;
            int start = maxItemsPerPage * page;
            int end = Math.min(start + maxItemsPerPage, paginatedData.size());
            PageRenderContext<T> context = new PageRenderContext<>(
                    this,
                    inventory,
                    paginatedData,
                    slots,
                    start,
                    end,
                    (menu, data, dataIndex, slot) -> {
                        ItemStack menuItem = itemRenderer.apply(data);
                        if (menuItem == null) return null;
                        ItemMeta meta = menuItem.getItemMeta();
                        if (meta != null) {
                            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, String.valueOf(dataIndex));
                            menuItem.setItemMeta(meta);
                        }
                        return menuItem;
                    },
                    (inv, slot, item, dataIndex, map) -> {
                        if (!isValidSlot(slot)) return;
                        inv.setItem(slot, item);
                        map.put(slot, dataIndex);
                    },
                    paginatedSlotToDataIndex,
                    animationTickDelay
            );
            pageTransition.render(context);
        }
        postSetMenuItems();
    }

    public void preSetMenuItems() {
    }

    public void postSetMenuItems() {
    }

    protected MenuItemType getMenuItemType(int slot, ItemStack item) {
        if (paginatedSlotToDataIndex.containsKey(slot)) return MenuItemType.PAGE;
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(itemKey, PersistentDataType.STRING)) {
                return MenuItemType.PAGE;
            }
        }

        if (isBorderSlot(slot)) return MenuItemType.BORDER;
        return MenuItemType.CUSTOM;
    }

    protected boolean isBorderSlot(int slot) {
        int size = getSlots();
        if (size % 9 != 0) return false;
        int rows = size / 9;
        int row = slot / 9;
        int col = slot % 9;
        return row == 0 || row == rows - 1 || col == 0 || col == 8;
    }

    protected boolean handleBorderItemClick(int slot, ItemStack item, InventoryClickEvent event) {
        ensurePrepared();
        Player player = (Player) event.getWhoClicked();
        if (item == null) return false;
        int size = getSlots();
        int rows = size % 9 == 0 ? size / 9 : 0;
        int bottomRowStart = rows > 0 ? (rows - 1) * 9 : 0;
        int prevSlot = controlLayout == null ? getSlot("General.Left", bottomRowStart + 3) : controlLayout.getPreviousSlot();
        int closeSlot = controlLayout == null ? getSlot("General.Close", bottomRowStart + 4) : controlLayout.getCloseSlot();
        int nextSlot = controlLayout == null ? getSlot("General.Right", bottomRowStart + 5) : controlLayout.getNextSlot();
        int refreshSlot = controlLayout == null ? getSlot("General.Refresh", bottomRowStart + 6) : controlLayout.getRefreshSlot();
        String prevKey = navigationDirection == NavigationDirection.VERTICAL ? "General.Up" : "General.Left";
        String nextKey = navigationDirection == NavigationDirection.VERTICAL ? "General.Down" : "General.Right";
        ItemStack prevItem = lgm.getItem(prevKey, player, false);
        ItemStack nextItem = lgm.getItem(nextKey, player, false);
        if (prevItem == null) prevItem = lgm.getItem("General.Left", player, false);
        if (nextItem == null) nextItem = lgm.getItem("General.Right", player, false);

        if (slot == closeSlot || item.isSimilar(lgm.getItem("General.Close", player, false))) {
            closeAndReturnOrClose();
            return true;
        } else if (slot == prevSlot || item.isSimilar(prevItem)) {
            if (pageDirection == PageDirection.REVERSED) return goForward(player);
            return goBackward(player);
        } else if (slot == nextSlot || item.isSimilar(nextItem)) {
            if (pageDirection == PageDirection.REVERSED) return goBackward(player);
            return goForward(player);
        } else if (slot == refreshSlot || item.isSimilar(lgm.getItem("General.Refresh", player, false))) {
            super.open();
            return true;
        }
        return false;
    }

    private boolean goBackward(Player player) {
        if (page == 0) {
            player.sendMessage(lgm.getMessage("Player.General.AlreadyOnFirstPage", player, true));
            return false;
        }
        page--;
        super.open();
        return true;
    }

    private boolean goForward(Player player) {
        if (paginatedData != null && (page + 1) * maxItemsPerPage < paginatedData.size()) {
            page++;
            super.open();
            return true;
        }
        player.sendMessage(lgm.getMessage("Player.General.AlreadyOnLastPage", player, true));
        return false;
    }

    protected void handlePageItemClick(int slot, ItemStack item, InventoryClickEvent event) {
    }

    protected void handleCustomItemClick(int slot, ItemStack item, InventoryClickEvent event) {
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        MenuItemType type = getMenuItemType(slot, item);
        if (type == MenuItemType.BORDER) {
            boolean result = handleBorderItemClick(slot, item, event);
            if (!result) {
                handleCustomItemClick(slot, item, event);
            }
        } else if (type == MenuItemType.PAGE) {
            T entry = entryClickHandler == null ? null : getPaginatedDataForSlot(slot, item);
            if (entry != null) {
                entryClickHandler.onClick((Player) event.getWhoClicked(), entry, event);
            } else {
                handlePageItemClick(slot, item, event);
            }
        } else if (type == MenuItemType.CUSTOM) {
            handleCustomItemClick(slot, item, event);
        }
    }

    @FunctionalInterface
    public interface EntryClickHandler<T> {
        void onClick(Player player, T entry, InventoryClickEvent event);
    }

    public enum MenuItemType {
        BORDER,
        PAGE,
        CUSTOM
    }

    protected Integer getDataIndexForSlot(int slot, ItemStack item) {
        Integer mappedIndex = paginatedSlotToDataIndex.get(slot);
        if (mappedIndex != null) {
            return mappedIndex;
        }

        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(itemKey, PersistentDataType.STRING)) {
                String value = meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
                if (value != null) {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    protected T getPaginatedDataForSlot(int slot, ItemStack item) {
        Integer dataIndex = getDataIndexForSlot(slot, item);
        if (dataIndex == null || paginatedData == null || dataIndex < 0 || dataIndex >= paginatedData.size()) {
            return null;
        }
        return paginatedData.get(dataIndex);
    }

    protected boolean isValidSlot(int slot) {
        return slot >= 0 && slot < getSlots();
    }

    protected Set<Integer> getAutoExcludedFillerSlots() {
        Set<Integer> excluded = new HashSet<>();
        for (int slot : getPaginatedItemSlots()) {
            if (isValidSlot(slot)) excluded.add(slot);
        }
        if (controlLayout != null) {
            if (isValidSlot(controlLayout.getPreviousSlot())) excluded.add(controlLayout.getPreviousSlot());
            if (isValidSlot(controlLayout.getCloseSlot())) excluded.add(controlLayout.getCloseSlot());
            if (isValidSlot(controlLayout.getNextSlot())) excluded.add(controlLayout.getNextSlot());
            if (isValidSlot(controlLayout.getRefreshSlot())) excluded.add(controlLayout.getRefreshSlot());
        } else {
            int size = getSlots();
            if (size % 9 == 0) {
                int bottomRowStart = (size - 9);
                int prev = getSlot("General.Left", bottomRowStart + 3);
                int close = getSlot("General.Close", bottomRowStart + 4);
                int next = getSlot("General.Right", bottomRowStart + 5);
                int refresh = getSlot("General.Refresh", bottomRowStart + 6);
                if (isValidSlot(prev)) excluded.add(prev);
                if (isValidSlot(close)) excluded.add(close);
                if (isValidSlot(next)) excluded.add(next);
                if (isValidSlot(refresh)) excluded.add(refresh);
            }
        }
        return excluded;
    }
}
