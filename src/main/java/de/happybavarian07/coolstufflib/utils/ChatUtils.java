package de.happybavarian07.coolstufflib.utils;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * <p>Utilities for chat formatting and placeholder replacement.</p>
 */
public final class ChatUtils {

    private ChatUtils() {}

    public static String chat(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String format(Player player, String message, String prefix) {
        try {
            String withColor = ChatColor.translateAlternateColorCodes('&', message.replace("%prefix%", prefix));
            if (!CoolStuffLib.getLib().isPlaceholderAPIEnabled()) return withColor;
            return PlaceholderAPI.setPlaceholders(player, withColor);
        } catch (Exception e) {
            return ChatColor.translateAlternateColorCodes('&', message.replace("%prefix%", prefix));
        }
    }

    public static String logPrefix(String testMessage) {
        String prefix = "&c[&6CoolStuffLib&c]";
        if (CoolStuffLib.getLib() != null && CoolStuffLib.getLib().getLanguageManager() != null) {
            String customPrefix = CoolStuffLib.getLib().getLanguageManager().getPrefix();
            if (customPrefix != null && !customPrefix.isEmpty()) {
                prefix = customPrefix;
            }
        }

        String colorSuffix = " &7";
        if (testMessage.startsWith("§c") || testMessage.startsWith("&c")) {
            colorSuffix = " &c";
        } else if (testMessage.startsWith("§e") || testMessage.startsWith("&e")) {
            colorSuffix = " &e";
        } else if (testMessage.startsWith("§a") || testMessage.startsWith("&a")) {
            colorSuffix = " &a";
        }
        
        return chat(prefix + colorSuffix) + testMessage;
    }
}
