package de.happybavarian07.coolstufflib.menusystem;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.menusystem.actions.MenuAction;
import de.happybavarian07.coolstufflib.utils.HybridInventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * <p>Base class for defining inventory-based menus.</p>
 */
public abstract class Menu implements InventoryHolder {

    protected CoolStuffLib lib;
    protected LanguageManager lgm;
    protected ItemStack FILLER;
    protected boolean prepared = false;

    protected String openingPermission = "";
    protected PlayerMenuUtility playerMenuUtility;
    protected Inventory inventory;
    protected final Map<Integer, MenuAction> slotActions = new HashMap<>();
    protected final Set<Integer> forbiddenSlots = new HashSet<>();
    protected boolean forceHybridMode = false;
    protected Menu savedMenu;

    /**
     * <p>Constructs a new Menu instance.</p>
     *
     * @param playerMenuUtility The menu utility
     */
    public Menu(PlayerMenuUtility playerMenuUtility) {
        this(playerMenuUtility, null);
    }

    /**
     * <p>Constructs a new Menu instance with a parent menu.</p>
     *
     * @param playerMenuUtility The menu utility
     * @param savedMenu         The parent menu
     */
    public Menu(PlayerMenuUtility playerMenuUtility, Menu savedMenu) {
        this.playerMenuUtility = playerMenuUtility;
        this.savedMenu = savedMenu;
        ensurePrepared();
    }

    /**
     * <p>Ensures dependencies are injected.</p>
     */
    protected void ensurePrepared() {
        if (prepared) return;
        this.lib = playerMenuUtility.getLib();
        this.lgm = lib.getLanguageManager();
        this.FILLER = lgm.getItem("General.FillerItem", null, false);
        this.prepared = true;
    }

    //let each menu decide their name
    public abstract String getMenuName();

    public abstract String getConfigMenuAddonFeatureName();

    //let each menu decide their slot amount
    public abstract int getSlots();

    //let each menu decide how the items in the menu will be handled when clicked
    @Deprecated(since = "3.0.0", forRemoval = false)
    public void handleMenu(InventoryClickEvent e) {
        // Should now all be handled via the registerButton and the action supplier
    };

    // Inventory Open Event
    public abstract void handleOpenMenu(InventoryOpenEvent e);

    // Inventory Close Event
    public abstract void handleCloseMenu(InventoryCloseEvent e);

    //let each menu decide what items are to be placed in the inventory menu
    public abstract void setMenuItems();

    /**
     * The getOpeningPermission function returns the openingPermission variable.
     *
     * @return The openingpermission variable
     */
    public String getOpeningPermission() {
        return openingPermission;
    }

    /**
     * The setOpeningPermission function sets the openingPermission variable to a new value.
     *
     * @param permission Set the openingpermission variable
     */
    public void setOpeningPermission(String permission) {
        this.openingPermission = permission;
    }

    /**
     * The legacyServer function checks the server version and returns true if it is 1.12 or older,
     * false otherwise. This is used to determine whether to use legacy methods for certain
     * things like setting a player's skin (which changed in 1.13).
     *
     * @return A boolean value, true or false
     */
    protected boolean legacyServer() {
        String serverVersion = Bukkit.getServer().getVersion();
        return serverVersion.contains("1.12") ||
                serverVersion.contains("1.11") ||
                serverVersion.contains("1.10") ||
                serverVersion.contains("1.9") ||
                serverVersion.contains("1.8") ||
                serverVersion.contains("1.7");
    }

    //When called, an inventory is created and opened for the player

    /**
     * <p>Opens the menu for the player.</p>
     */
    public void open() {
        ensurePrepared();

        if (!playerMenuUtility.getOwner().hasPermission(this.openingPermission)) {
            playerMenuUtility.getOwner().sendMessage(
                    lgm.getMessage("Player.General.NoPermissions", playerMenuUtility.getOwner(), true));
            playerMenuUtility.getOwner().closeInventory();
            return;
        }

        inventory = Bukkit.createInventory(this, getSlots(), getMenuName());
        slotActions.clear();
        Map<String, MenuAddon> addonList = new HashMap<>();
        if (lib.getMenuAddonManager() != null) {
            addonList = lib.getMenuAddonManager().getMenuAddons(this.getConfigMenuAddonFeatureName());
        }

        this.setMenuItems();

        for (Map.Entry<String, MenuAddon> menuAddonName : addonList.entrySet()) {
            MenuAddon addon = menuAddonName.getValue();
            addon.setMenuAddonItems();
        }

        if (Listener.class.isAssignableFrom(this.getClass())) {
            Bukkit.getPluginManager().registerEvents((Listener) this, lib.getJavaPluginUsingLib());
        }

        if (forceHybridMode) {
            HybridInventoryUtils.setCompatibilityMode(true);
        }

        InventoryView view = HybridInventoryUtils.openInventorySafe(playerMenuUtility.getOwner(), inventory);

        if (view == null) {
            Bukkit.getLogger().severe("[CoolStuffLib] Failed to open menu for " +
                    playerMenuUtility.getOwner().getName());
            playerMenuUtility.getOwner().sendMessage(
                    ChatColor.RED + "Failed to open menu. Please contact an administrator.");
            playerMenuUtility.getOwner().closeInventory();
            return;
        }

        for (Map.Entry<String, MenuAddon> menuAddonName : addonList.entrySet()) {
            MenuAddon addon = menuAddonName.getValue();
            addon.onOpenEvent();
        }
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

    /**
     * Opens a menu for the player. This function is thread-safe.
     */
    public void openThreadSafe() {
        Bukkit.getScheduler().runTask(lib.getJavaPluginUsingLib(), this::open);
    }

    //Overridden method from the InventoryHolder interface

    /**
     * The getInventory function returns the inventory of the player.
     *
     * @return The inventory object
     */
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    //Helpful utility method to fill all remaining slots with "filler glass"

    /**
     * The setFillerGlass function is used to fill the empty slots in a player's inventory with glass panes.
     * This function is called when a player opens their inventory, and it ensures that all of the empty slots are filled with glass panes.
     */
    public void setFillerGlass() {
        setFillerGlass(Collections.emptySet());
    }

    /**
     * Fills all empty slots with the configured filler item while skipping the provided slots.
     *
     * @param excludedSlots slots that should never be filled by this call
     */
    public void setFillerGlass(Set<Integer> excludedSlots) {
        Set<Integer> excluded = excludedSlots == null ? Collections.emptySet() : excludedSlots;
        for (int i = 0; i < getSlots(); i++) {
            if (excluded.contains(i)) continue;
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, FILLER);
            }
        }
    }

    /**
     * Convenience overload for excluding slots while filling.
     *
     * @param excludedSlots slots to skip
     */
    public void setFillerGlass(int... excludedSlots) {
        if (excludedSlots == null || excludedSlots.length == 0) {
            setFillerGlass();
            return;
        }
        Set<Integer> excluded = new HashSet<>();
        for (int slot : excludedSlots) {
            if (slot >= 0 && slot < getSlots()) excluded.add(slot);
        }
        setFillerGlass(excluded);
    }

    /**
     * The makeItem function is a function that creates an ItemStack with the given parameters.
     *
     * @param material    Set the material of the item
     * @param displayName Set the name of the item
     * @param lore        Make the lore variable a string array
     * @return An itemstack
     */
    public ItemStack makeItem(Material material, String displayName, String... lore) {

        ItemStack item = new ItemStack(material);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(displayName);

        itemMeta.setLore(Arrays.asList(lore));
        item.setItemMeta(itemMeta);

        return item;
    }


    /**
     * The getSlot function is used to get the slot of an item.
     *
     * @param path       Get the path of the item
     * @param defaultInt Set a default value for the slot if it is not found in the config
     * @return The slot of the item
     */
    public int getSlot(String path, int defaultInt) {
        ensurePrepared();
        return lgm.getCustomObject("Items." + path + ".slot", null, defaultInt, false);
    }

    public boolean registerButton(int slot, ItemStack item, MenuAction action, Set<Integer> forbidden) {
        if (forbidden != null && forbidden.contains(slot)) throw new IllegalArgumentException("Slot forbidden");
        if (forbiddenSlots.contains(slot)) throw new IllegalArgumentException("Slot forbidden");
        ItemStack stack = inventory.getItem(slot);
        if (stack != null && !(stack.getType().isAir() || stack.isSimilar(FILLER)))
            throw new IllegalStateException("Slot occupied");
        inventory.setItem(slot, item);
        slotActions.put(slot, action);
        return true;
    }

    public boolean registerButton(ItemStack item, MenuAction action, Set<Integer> forbidden) {
        int slot = findFirstEmptySlot(forbidden);
        if (slot == -1) return false;
        return registerButton(slot, item, action, forbidden);
    }

    public boolean registerButton(int slot, ItemStack item, MenuAction action) {
        return registerButton(slot, item, action, null);
    }

    public boolean registerButton(ItemStack item, MenuAction action) {
        return registerButton(item, action, null);
    }

    /**
     * <p>Registers a button whose item comes from {@code Items.<itemPath>} in the language file and whose
     * slot comes from {@code Items.<itemPath>.slot}. Without a configured slot the first free slot is used.</p>
     *
     * <pre><code>button("PlayerManager.Heal", MenuAction.of((p, e) -&gt; heal(p)).requires("admin.heal"));</code></pre>
     *
     * @return {@code false} if no slot was configured and no free slot is left
     */
    public boolean button(String itemPath, MenuAction action) {
        ensurePrepared();
        ItemStack item = lgm.getItem(itemPath, playerMenuUtility.getOwner(), false);
        int slot = getSlot(itemPath, -1);
        return slot >= 0 ? registerButton(slot, item, action, null) : registerButton(item, action, null);
    }

    public boolean tryRegisterButton(int slot, ItemStack item, MenuAction action, Set<Integer> forbidden) {
        try {
            return registerButton(slot, item, action, forbidden);
        } catch (Exception e) {
            return false;
        }
    }

    public void clearRegisteredButtons() {
        slotActions.clear();
    }

    public int findFirstEmptySlot(Set<Integer> forbidden) {
        for (int i = 0; i < getSlots(); i++) {
            if ((forbidden != null && forbidden.contains(i)) || forbiddenSlots.contains(i)) continue;
            ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.getType().isAir() || stack.isSimilar(FILLER)) return i;
        }
        return -1;
    }

    public void setForbiddenSlots(Set<Integer> slots) {
        forbiddenSlots.clear();
        if (slots != null) forbiddenSlots.addAll(slots);
    }

    public void setSavedMenu(Menu savedMenu) {
        this.savedMenu = savedMenu;
    }
}
