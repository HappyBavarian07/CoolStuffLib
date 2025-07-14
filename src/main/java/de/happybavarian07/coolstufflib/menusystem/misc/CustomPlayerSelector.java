package de.happybavarian07.coolstufflib.menusystem.misc;/*
 * @Author HappyBavarian07
 * @Date 27.11.2022 | 10:51
 */

import de.happybavarian07.coolstufflib.languagemanager.PlaceholderType;
import de.happybavarian07.coolstufflib.menusystem.Menu;
import de.happybavarian07.coolstufflib.menusystem.PaginatedMenu;
import de.happybavarian07.coolstufflib.menusystem.PlayerMenuUtility;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class CustomPlayerSelector<T, R> extends PaginatedMenu<Player> {
    private final Function<T, R> functionToExecute;
    private final Map<UUID, InventoryAction> clickedPlayers = new HashMap<>();
    private final Menu oldMenu;
    private final List<Player> players;
    private final String action;
    private final String infoItemExtraInfos;
    private final boolean multiSelect;
    // T is either a UUID or a Map<UUID, InventoryAction> (If you want to use Multi-Select)
    // This Predicate is used for making the Program using this Class able to filter on what actions they want to have the Name in what Color.
    private final Function<Map.Entry<UUID, InventoryAction>, ChatColor> functionForDecidingItemColor;
    private CompletableFuture<R> future;

    /**
     * This Constructor is used to set up a Custom Player Selector Menu.<br>
     * If you want to use Multi-Select, you need to have T as a {@code Map<UUID, InventoryAction>}.
     * <br>
     * <br>
     * The following Values can be null if Multi-Select isn't enabled:<br>
     * - functionForDecidingItemColor<br>
     *
     * @param action                       Info for the Player what the Menu is used for.
     *                                     (Example: "Select a Player to invite to your Guild")
     * @param infoItemExtraInfos           Extra Infos for the Info Item.
     *                                     (Example: "(Green = Online, Red = Offline, Black = Inactive)")
     * @param playerMenuUtility            The PlayerMenuUtility for the Menu.
     * @param functionToExecute            The Function that should be executed after the Player selected a Player.
     *                                     (If Multi-Select is enabled, this will first be executed after the Player clicked the Confirm Button)
     * @param functionForDecidingItemColor The Function that should be executed to decide what Color the Name of the Player should have.
     * @param oldMenu                      The Menu that should be opened after the Player selected a Player.
     * @param players                      The List of Players that should be displayed in the Menu.
     * @param multiSelect                  If the Player should be able to select multiple Players.
     */
    public CustomPlayerSelector(String action,
                                String infoItemExtraInfos,
                                PlayerMenuUtility playerMenuUtility,
                                Function<T, R> functionToExecute,
                                Function<Map.Entry<UUID, InventoryAction>, ChatColor> functionForDecidingItemColor,
                                Menu oldMenu,
                                List<Player> players,
                                boolean multiSelect) {
        super(playerMenuUtility, oldMenu);
        this.oldMenu = oldMenu;
        this.players = players;
        setPlayers(players, player -> {
            lgm.addPlaceholder(PlaceholderType.ITEM, "%player_name%", player.getName(), false);
            lgm.addPlaceholder(PlaceholderType.ITEM, "%player_uuid%", player.getUniqueId().toString(), false);
            ItemStack item = lgm.getItem("CustomPlayerSelector.PlayerItem", player, false);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (functionForDecidingItemColor != null) {
                    ChatColor color = functionForDecidingItemColor.apply(new AbstractMap.SimpleEntry<>(player.getUniqueId(), InventoryAction.PICKUP_ALL));
                    meta.setDisplayName(color + ChatColor.stripColor(meta.getDisplayName()));
                }
                if(meta instanceof SkullMeta skullMeta) {
                    skullMeta.setOwningPlayer(player);
                }
                item.setItemMeta(meta);
            }
            item.getItemMeta().getPersistentDataContainer().set(new NamespacedKey(lib.getJavaPluginUsingLib(), "player_head"), PersistentDataType.STRING, player.getUniqueId().toString());
            return item;
        });
        this.functionToExecute = functionToExecute;
        this.action = action;
        this.infoItemExtraInfos = infoItemExtraInfos;
        this.multiSelect = multiSelect;
        this.functionForDecidingItemColor = functionForDecidingItemColor;
    }

    public CompletableFuture<R> getFuture() {
        return future;
    }


    @Override
    public String getMenuName() {
        return lgm.getMenuTitle("CustomPlayerSelector", playerMenuUtility.getOwner());
    }

    @Override
    public String getConfigMenuAddonFeatureName() {
        return "CustomPlayerSelector";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleOpenMenu(InventoryOpenEvent e) {

    }

    @Override
    public void handleCloseMenu(InventoryCloseEvent e) {

    }

    @Override
    public void preSetMenuItems() {

    }

    @Override
    public void postSetMenuItems() {
        lgm.addPlaceholder(PlaceholderType.ITEM, "%action%", action, false);
        ItemStack infoItem = lgm.getItem("CustomPlayerSelector.InfoItem", playerMenuUtility.getOwner(), false);
        inventory.setItem(4, infoItem);
        if (multiSelect) {
            ItemStack confirmItem = lgm.getItem("CustomPlayerSelector.Confirm", null, false);
            inventory.setItem(getSlots() - 6, confirmItem);
            ItemStack deselectItem = lgm.getItem("CustomPlayerSelector.DeSelect", null, false);
            inventory.setItem(getSlots() - 7, deselectItem);
        }
    }

    @Override
    protected void handlePageItemClick(int slot, ItemStack item, InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (item == null || item.getItemMeta() == null) return;
        String uuidStr = item.getItemMeta().getPersistentDataContainer().get(new org.bukkit.NamespacedKey(lib.getJavaPluginUsingLib(), "player_head"), PersistentDataType.STRING);
        if (uuidStr == null) return;
        UUID target;
        try {
            target = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException ex) {
            return;
        }
        playerMenuUtility.setTargetUUID(target);
        if (multiSelect) {
            if (clickedPlayers.containsKey(target)) {
                clickedPlayers.remove(target);
            } else {
                clickedPlayers.put(target, event.getAction());
            }
            super.open();
            return;
        }
        future = CompletableFuture.supplyAsync(() -> functionToExecute.apply((T) target));
        future.thenAccept(result -> Bukkit.getScheduler().runTask(lib.getJavaPluginUsingLib(), () -> {
            if (oldMenu != null) {
                oldMenu.open();
            } else {
                player.closeInventory();
            }
        })).exceptionally(throwable -> {
            lgm.addPlaceholder(PlaceholderType.MESSAGE, "%error%", throwable + ": " + throwable.getMessage(), false);
            lgm.addPlaceholder(PlaceholderType.MESSAGE, "%stacktrace%", Arrays.toString(throwable.getStackTrace()), false);
            player.sendMessage(lgm.getMessage("Player.General.Error", player, true));
            return null;
        });
    }

    @Override
    protected void handleCustomItemClick(int slot, ItemStack item, InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (item == null) return;
        if (multiSelect && item.equals(lgm.getItem("CustomPlayerSelector.Confirm", null, false))) {
            future = CompletableFuture.supplyAsync(() -> functionToExecute.apply((T) clickedPlayers));
            future.thenAccept(result -> Bukkit.getScheduler().runTask(lib.getJavaPluginUsingLib(), () -> {
                if (oldMenu != null) {
                    oldMenu.open();
                } else {
                    player.closeInventory();
                }
            })).exceptionally(throwable -> {
                lgm.addPlaceholder(PlaceholderType.MESSAGE, "%error%", throwable + ": " + throwable.getMessage(), false);
                lgm.addPlaceholder(PlaceholderType.MESSAGE, "%stacktrace%", Arrays.toString(throwable.getStackTrace()), false);
                player.sendMessage(lgm.getMessage("Player.General.Error", player, true));
                return null;
            });
        }
    }

    public void setPlayers(List<Player> players, Function<Player, ItemStack> renderer) {
        setPaginatedData(players, renderer);
    }
}
