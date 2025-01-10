package de.happybavarian07.coolstufflib.utils;/*
 * @Author HappyBavarian07
 * @Date 24.04.2023 | 17:24
 */

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.menusystem.Menu;
import de.happybavarian07.coolstufflib.menusystem.PlayerMenuUtility;
import de.happybavarian07.coolstufflib.menusystem.misc.ConfirmationMenu;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public class Utils {
    public static String chat(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String format(Player player, String message, String prefix) {
        try {
            String withColor = ChatColor.translateAlternateColorCodes('&', message.replace("%prefix%", prefix));
            if(!CoolStuffLib.getLib().isPlaceholderAPIEnabled()) return withColor;
            return PlaceholderAPI.setPlaceholders(player, withColor);
        } catch (Exception e) {
            return ChatColor.translateAlternateColorCodes('&', message.replace("%prefix%", prefix));
        }
    }

    public static List<String> emptyList() {
        List<String> list = new ArrayList<>();
        list.add("");
        list.add("");
        return list;
    }

    /**
     * Gets a menu by its class name.
     * The menu must be in the menu package.
     * If the menu is not found, it will return null.
     * @param menuPackage The package of the menu
     * @param className The name of the class
     * @param player The player to open the menu for
     * @return The menu
     */
    public static Menu getMenuByClassName(String menuPackage, String className, Player player) {
        //String menuPackage = "de.happybavarian07.adminpanel.menusystem.menu";
        String fullClassName = menuPackage + "." + className;

        try {
            Class<?> clazz = Class.forName(fullClassName);
            if (Menu.class.isAssignableFrom(clazz)) {
                return (Menu) clazz.getDeclaredConstructor(PlayerMenuUtility.class).newInstance(CoolStuffLib.getLib().getPlayerMenuUtility(player.getUniqueId()));
            } else {
                System.err.println("The class does not extend Menu: " + fullClassName);
                return null; // Or handle it as needed
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + fullClassName);
            if(CoolStuffLib.getLib().getPluginFileLogger() != null)
                CoolStuffLib.getLib().getPluginFileLogger().writeToLog(Level.SEVERE, "Class not found: " + fullClassName, LogPrefix.ERROR, true);
            return null; // You can modify the return value based on your needs
        } catch (IllegalAccessException | InstantiationException e) {
            System.err.println("Error creating an instance: " + fullClassName);
            if(CoolStuffLib.getLib().getPluginFileLogger() != null)
                CoolStuffLib.getLib().getPluginFileLogger().writeToLog(Level.SEVERE, "Error creating an instance: " + fullClassName, LogPrefix.ERROR, true);
            return null; // You can modify the return value based on your needs
        } catch (InvocationTargetException | NoSuchMethodException e) {
            if(CoolStuffLib.getLib().getPluginFileLogger() != null)
                CoolStuffLib.getLib().getPluginFileLogger().writeToLog(Level.SEVERE, "Error invoking constructor: " + fullClassName, LogPrefix.ERROR, true);
            throw new RuntimeException("Error invoking constructor: " + fullClassName, e);
        }
    }

    /**
     * Opens a confirmation menu for the player to confirm an action.
     * If the player confirms the action, the methodToExecuteAfter will be executed.
     * If the player cancels the action, the menuToOpenAfter will be opened.
     * @param reason The reason for the confirmation
     * @param menuToOpenAfter The menu to open after the confirmation
     * @param menuPackage The package of the menu
     * @param methodToExecuteAfter The method to execute after the confirmation
     * @param objectToInvokeOn The object to invoke the method on
     * @param methodArgs The arguments for the method
     * @param exceptionsToCatch The exceptions to catch
     * @param player The player to open the menu for
     */
    public static void openConfirmationMenu(String reason,
                                            String menuToOpenAfter,
                                            String menuPackage,
                                            Method methodToExecuteAfter,
                                            Object objectToInvokeOn,
                                            List<Object> methodArgs,
                                            List<Class<? extends Exception>> exceptionsToCatch,
                                            Player player) {
        PlayerMenuUtility playerMenuUtility = CoolStuffLib.getLib().getPlayerMenuUtility(player.getUniqueId());
        ConfirmationMenu confirmationMenu = new ConfirmationMenu(playerMenuUtility);
        playerMenuUtility.setData("ConfirmationMenu_MenuToOpenAfter", menuToOpenAfter, true);
        playerMenuUtility.setData("ConfirmationMenu_MenuPackage", menuPackage, true);
        playerMenuUtility.setData("ConfirmationMenu_Reason", reason, true);
        playerMenuUtility.setData("ConfirmationMenu_MethodToExecuteAfter", methodToExecuteAfter, true);
        playerMenuUtility.setData("ConfirmationMenu_ObjectToInvokeMethodOn", objectToInvokeOn, true);
        for(int i = 0; i < methodArgs.size(); i++) {
            playerMenuUtility.setData("ConfirmationMenu_MethodArgs_" + i, methodArgs.get(i), true);
        }
        playerMenuUtility.setData("ConfirmationMenu_ExceptionsToCatch", exceptionsToCatch, true);
        confirmationMenu.open();
    }

    /**
     * Creates a skull with a head texture and a name from a String.
     * <br>
     * Use {@link #createSkull(Head, String)}, if there's a need to use predefined Head Values.
     *
     * @param headTexture The head texture as a String
     * @param name
     * @return
     */
    public static ItemStack createSkull(String headTexture, String name) {
        ItemStack head = new ItemStack(legacyServer() ? Objects.requireNonNull(Material.matchMaterial("SKULL_ITEM")) : Material.PLAYER_HEAD, 1);
        if (headTexture.isEmpty()) return head;

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assert meta != null;
        meta.setDisplayName(Utils.chat(name));
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "CustomHead");

        try {
            profile.getTextures().setSkin(new URL("https://textures.minecraft.net/texture/" + headTexture));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        meta.setOwnerProfile(profile);

        head.setItemMeta(meta);
        return head;
    }

    /**
     * Creates a skull with a head texture and a name from a Head Object.
     * <br>
     * This method can only handle values from {@link Head}
     *
     * @param headTexture Element from the Head Enum with Head String Values
     * @param name The name of the skull
     * @return The skull
     */
    public static ItemStack createSkull(Head headTexture, String name) {
        ItemStack head = new ItemStack(legacyServer() ? Objects.requireNonNull(Material.matchMaterial("SKULL_ITEM")) : Material.PLAYER_HEAD, 1);
        if (headTexture.getTexture().isEmpty()) return head;

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assert meta != null;
        meta.setDisplayName(Utils.chat(name));
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "CustomHead");

        try {
            profile.getTextures().setSkin(new URL("https://textures.minecraft.net/texture/" + headTexture.getTexture()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        meta.setOwnerProfile(profile);

        head.setItemMeta(meta);
        return head;
    }

    private static boolean legacyServer() {
        String serverVersion = Bukkit.getServer().getVersion();
        return serverVersion.contains("1.12") ||
                serverVersion.contains("1.11") ||
                serverVersion.contains("1.10") ||
                serverVersion.contains("1.9") ||
                serverVersion.contains("1.8") ||
                serverVersion.contains("1.7");
    }
}
