package de.happybavarian07.coolstufflib.menusystem;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/*
    Defines the behavior and attributes of all menus in our plugin
 */
public abstract class Menu implements InventoryHolder {

    //Protected values that can be accessed in the menus
    protected final CoolStuffLib lib = CoolStuffLib.getLib();
    protected final LanguageManager lgm = lib.getLanguageManager();
    protected ItemStack FILLER = lgm.getItem("General.FillerItem", null, false);
    protected String openingPermission = "";
    protected PlayerMenuUtility playerMenuUtility;
    protected Inventory inventory;
    protected List<Inventory> inventorys = new ArrayList<>();

    //Constructor for Menu. Pass in a PlayerMenuUtility so that
    // we have information on who's menu this is and
    // what info is to be transfered
    /**
     * The Menu function is the main function of this class. It creates a menu for the player to interact with, and
     * allows them to choose what they want to do next. The Menu function takes in no parameters.

     *
     * @param playerMenuUtility Pass the playermenuutility object to the menu class
     */
    public Menu(PlayerMenuUtility playerMenuUtility) {
        this.playerMenuUtility = playerMenuUtility;
    }

    //let each menu decide their name
    public abstract String getMenuName();

    public abstract String getConfigMenuAddonFeatureName();

    //let each menu decide their slot amount
    public abstract int getSlots();

    //let each menu decide how the items in the menu will be handled when clicked
    public abstract void handleMenu(InventoryClickEvent e);

    // Inventory Open Event
    public abstract void handleOpenMenu(InventoryOpenEvent e);

    // Inventory Close Event
    public abstract void handleCloseMenu(InventoryCloseEvent e);

    //let each menu decide what items are to be placed in the inventory menu
    public abstract void setMenuItems();

    /**
     * The getOpeningPermission function returns the openingPermission variable.
     *
     *
     *
     * @return The openingpermission variable
     */
    public String getOpeningPermission() {
        return openingPermission;
    }

    /**
     * The setOpeningPermission function sets the openingPermission variable to a new value.
     *
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
     * The open function is the main function that opens a menu.
     * It creates an inventory, sets the items in it, and then opens it for the player.
     */
    public void open() {
        //The owner of the inventory created is the Menu itself,
        // so we are able to reverse engineer the Menu object from the
        // inventoryHolder in the MenuListener class when handling clicks
        if (!playerMenuUtility.getOwner().hasPermission(this.openingPermission)) {
            playerMenuUtility.getOwner().sendMessage(
                    lib.getLanguageManager().getMessage("Player.General.NoPermissions", playerMenuUtility.getOwner(), true));
            playerMenuUtility.getOwner().closeInventory();
            return;
        }

        inventory = Bukkit.createInventory(this, getSlots(), getMenuName());
        inventorys.add(inventory);
        Map<String, MenuAddon> addonList = new HashMap<>();
        if (lib.getMenuAddonManager() != null) {
            addonList = lib.getMenuAddonManager().getMenuAddons(this.getConfigMenuAddonFeatureName());
        }

        //grab all the items specified to be used for this menu and add to inventory
        this.setMenuItems();

        for (Map.Entry<String, MenuAddon> menuAddonName : addonList.entrySet()) {
            MenuAddon addon = menuAddonName.getValue();
            addon.setMenuAddonItems();
        }

        if (Listener.class.isAssignableFrom(this.getClass())) {
            Bukkit.getPluginManager().registerEvents((Listener) this, CoolStuffLib.getLib().getJavaPluginUsingLib());
        }

        //open the inventory for the player
        playerMenuUtility.getOwner().openInventory(inventory);

        // Try executing Menu Addons onOpenEvent
        for (Map.Entry<String, MenuAddon> menuAddonName : addonList.entrySet()) {
            MenuAddon addon = menuAddonName.getValue();
            addon.onOpenEvent();
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
     *
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
        for (int i = 0; i < getSlots(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, FILLER);
            }
        }
    }

    /**
     * The makeItem function is a function that creates an ItemStack with the given parameters.
     *
     *
     * @param material Set the material of the item
     * @param displayName Set the name of the item
     * @param lore Make the lore variable a string array
     *
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
     *
     * @param path Get the path of the item
     * @param defaultInt Set a default value for the slot if it is not found in the config
     *
     * @return The slot of the item
     */
    public int getSlot(String path, int defaultInt) {
        return lgm.getCustomObject("Items." + path + ".slot", null, defaultInt, false);
    }
}

