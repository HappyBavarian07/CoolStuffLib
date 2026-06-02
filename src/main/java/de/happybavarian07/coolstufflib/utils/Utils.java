package de.happybavarian07.coolstufflib.utils;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.menusystem.Menu;
import de.happybavarian07.coolstufflib.menusystem.PlayerMenuUtility;
import de.happybavarian07.coolstufflib.menusystem.actions.MenuAction;
import de.happybavarian07.coolstufflib.menusystem.misc.ConfirmationMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * <p>Legacy utility facade. All methods have been migrated to specialized utility classes.</p>
 */
public class Utils {

    @Deprecated
    public static String chat(String s) {
        return ChatUtils.chat(s);
    }

    @Deprecated
    public static String format(Player player, String message, String prefix) {
        return ChatUtils.format(player, message, prefix);
    }

    public static List<String> emptyList() {
        List<String> list = new ArrayList<>();
        list.add("");
        list.add("");
        return list;
    }

    public static Menu getMenuByClassName(String menuPackage, String className, Player player) {
        String fullClassName = menuPackage + "." + className;
        try {
            Class<?> clazz = Class.forName(fullClassName);
            if (Menu.class.isAssignableFrom(clazz)) {
                return (Menu) clazz.getDeclaredConstructor(PlayerMenuUtility.class).newInstance(CoolStuffLib.getLib().getPlayerMenuUtility(player.getUniqueId()));
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static void openConfirmationMenu(String reason, MenuAction action, Player player, Menu savedMenu) {
        PlayerMenuUtility utility = CoolStuffLib.getLib().getPlayerMenuUtility(player.getUniqueId());
        ConfirmationMenu confirmationMenu = new ConfirmationMenu(utility, savedMenu);
        utility.setData("ConfirmationMenu_Reason", reason, true);
        utility.setData("ConfirmationMenu_Action", action, true);
        confirmationMenu.open();
    }

    @Deprecated
    public static ItemStack createSkull(String headValue, String name, boolean isTexture) {
        return ItemUtils.createSkull(headValue, name, isTexture);
    }

    @Deprecated
    public static ItemStack createSkull(Head headTexture, String name) {
        return ItemUtils.createSkull(headTexture, name);
    }

    @Nullable
    @Deprecated
    public static InputStream getResource(@NotNull String filename) {
        return FileUtils.getResource(filename);
    }

    @Deprecated
    public static void saveResource(File configFolder, @NotNull String resourcePath, boolean replace) {
        FileUtils.saveResource(configFolder, resourcePath, replace);
    }

    @Deprecated
    public static void zipFiles(File[] files, String zipFile, File baseDir) throws IOException {
        FileUtils.zipFiles(files, zipFile, baseDir);
    }

    @Deprecated
    public static void unzipFiles(String zipFilePath, String destDir, boolean replace) {
        FileUtils.unzipFiles(zipFilePath, destDir, replace);
    }

    @Deprecated
    public static String getFileExtension(File file) {
        return FileUtils.getFileExtension(file);
    }

    @Deprecated
    public static boolean isValidPath(String s) {
        return FileUtils.isValidPath(s);
    }

    public static String sanitize(String text) {
        if (text == null) return "";
        return text.replaceAll("[^a-zA-Z0-9_ ]+", "_").trim().replaceAll(" +", "_").toLowerCase().replaceAll("^_+|_+$", "");
    }

    @Deprecated
    public static void createDirectories(File file) {
        FileUtils.createDirectories(file);
    }

    @Deprecated
    public static void copyFile(File src, File dest) {
        FileUtils.copyFile(src, dest);
    }

    @Deprecated
    public static void deleteDirectory(File file) {
        FileUtils.deleteDirectory(file);
    }

    @Deprecated
    public static String joinPath(String separator, String... strings) {
        return FileUtils.joinPath(separator, strings);
    }

    @Deprecated
    public static String[] splitPath(String separator, String s) {
        return FileUtils.splitPath(separator, s);
    }

    public static String formatDuration(long i, TimeUnit inputUnit) {
        long seconds = inputUnit.toSeconds(i);
        long days = seconds / 86400; seconds %= 86400;
        long hours = seconds / 3600; seconds %= 3600;
        long minutes = seconds / 60; seconds %= 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    @Deprecated
    public static String logPrefix(String testMessage) {
        return ChatUtils.logPrefix(testMessage);
    }
}