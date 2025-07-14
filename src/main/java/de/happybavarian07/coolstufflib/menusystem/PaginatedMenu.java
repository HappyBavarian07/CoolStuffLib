package de.happybavarian07.coolstufflib.menusystem;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.function.Function;

public abstract class PaginatedMenu<T> extends Menu {
    private final NamespacedKey itemKey = new NamespacedKey("coolstufflib-menusystem", "paginated_item");
    protected int page = 0;
    protected int maxItemsPerPage;
    protected int index = 0;
    protected List<T> paginatedData;
    protected Function<T, ItemStack> itemRenderer;
    protected Menu savedMenu;

    public PaginatedMenu(PlayerMenuUtility playerMenuUtility) {
        this(playerMenuUtility, null);
    }

    public PaginatedMenu(PlayerMenuUtility playerMenuUtility, Menu savedMenu) {
        super(playerMenuUtility);
        int slots = getSlots();
        this.maxItemsPerPage = slots % 9 == 0 ? slots : 0;
        this.savedMenu = savedMenu;
    }

    public void setPaginatedData(List<T> data, Function<T, ItemStack> renderer) {
        this.paginatedData = data;
        this.itemRenderer = renderer;
        int slots = getSlots();
        this.maxItemsPerPage = slots % 9 == 0 ? slots : 0;
    }

    //Set the border and menu buttons for the menu

    /**
     * The addMenuBorder function adds the border to the menu.
     * It does this by setting all the items in slots 0-9, 17, 18, 26-36 and 44-53 to a filler item.
     * It also sets all the items in slots 48, 49, 50 and 51 to their respective border items (Left arrow for slot 48 etc.)
     */
    public void addMenuBorder() {
        LanguageManager lgm = CoolStuffLib.getLib().getLanguageManager();
        int size = getSlots();
        if (size % 9 != 0) return;
        int rows = size / 9;
        int bottomRowStart = (rows - 1) * 9;
        int leftBtnSlot = bottomRowStart + 3;
        int closeBtnSlot = bottomRowStart + 4;
        int rightBtnSlot = bottomRowStart + 5;
        int refreshBtnSlot = bottomRowStart + 6;
        inventory.setItem(getSlot("General.Left", leftBtnSlot), lgm.getItem("General.Left", null, false));
        inventory.setItem(getSlot("General.Close", closeBtnSlot), lgm.getItem("General.Close", null, false));
        inventory.setItem(getSlot("General.Right", rightBtnSlot), lgm.getItem("General.Right", null, false));
        inventory.setItem(getSlot("General.Refresh", refreshBtnSlot), lgm.getItem("General.Refresh", null, false));
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

    public void closeAndReturnOrClose() {
        if (savedMenu != null) {
            savedMenu.open();
        } else {
            Player player = playerMenuUtility.getOwner();
            if (player != null) {
                player.closeInventory();
            }
        }
    }

    @Override
    public void setMenuItems() {
        preSetMenuItems();
        if (paginatedData != null && itemRenderer != null) {
            addMenuBorder();
            int[] slots = getPaginatedItemSlots();
            if (slots.length == 0) return;
            maxItemsPerPage = slots.length;
            int start = maxItemsPerPage * page;
            int end = Math.min(start + maxItemsPerPage, paginatedData.size());
            int slotIdx = 0;
            for (int i = start; i < end && slotIdx < slots.length; i++) {
                T item = paginatedData.get(i);
                if (item != null) {
                    ItemStack menuItem = itemRenderer.apply(item);
                    if (menuItem == null) continue;
                    ItemMeta meta = menuItem.getItemMeta();
                    if (meta != null) {
                        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, String.valueOf(i));
                        menuItem.setItemMeta(meta);
                    }
                    inventory.setItem(slots[slotIdx], menuItem);
                    slotIdx++;
                }
            }
        }
        postSetMenuItems();
    }

    public abstract void preSetMenuItems();

    public abstract void postSetMenuItems();

    protected MenuItemType getMenuItemType(int slot, ItemStack item) {
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
        Player player = (Player) event.getWhoClicked();
        if (item == null) return false;
        if (item.isSimilar(lgm.getItem("General.Close", null, false))) {
            closeAndReturnOrClose();
            return true;
        } else if (item.isSimilar(lgm.getItem("General.Left", null, false))) {
            if (page == 0) {
                player.sendMessage(lgm.getMessage("Player.General.AlreadyOnFirstPage", player, true));
            } else {
                page = page - 1;
                super.open();
            }
            return true;
        } else if (item.isSimilar(lgm.getItem("General.Right", null, false))) {
            if ((page + 1) * maxItemsPerPage < paginatedData.size()) {
                page = page + 1;
                super.open();
            } else {
                player.sendMessage(lgm.getMessage("Player.General.AlreadyOnLastPage", player, true));
            }
            return true;
        } else if (item.isSimilar(lgm.getItem("General.Refresh", null, false))) {
            super.open();
            return true;
        }
        return false;
    }

    protected abstract void handlePageItemClick(int slot, ItemStack item, InventoryClickEvent event);

    protected abstract void handleCustomItemClick(int slot, ItemStack item, InventoryClickEvent event);

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
            handlePageItemClick(slot, item, event);
        } else if (type == MenuItemType.CUSTOM) {
            handleCustomItemClick(slot, item, event);
        }
    }

    public void setSavedMenu(Menu savedMenu) {
        this.savedMenu = savedMenu;
    }

    public enum MenuItemType {
        BORDER,
        PAGE,
        CUSTOM
    }
}
