package de.happybavarian07.coolstufflib.utils;/*
 * @Author HappyBavarian07
 * @Date 24.04.2023 | 17:24
 */

import de.happybavarian07.coolstufflib.CoolStuffLib;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

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
}
