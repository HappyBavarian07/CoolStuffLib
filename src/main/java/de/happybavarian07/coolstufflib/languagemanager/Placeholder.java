package de.happybavarian07.coolstufflib.languagemanager;/*
 * @Author HappyBavarian07
 * @Date 25.04.2022 | 17:07
 */

import de.happybavarian07.coolstufflib.utils.Utils;
import org.bukkit.ChatColor;

public record Placeholder(String key, Object value, PlaceholderType type) {
    /**
     * The Placeholder function is used to create a placeholder object that can be
     * used in the Language Manager. The Placeholder function takes three parameters:
     *
     * @param key   Identify the placeholder
     * @param value Store the value of the placeholder
     * @param type  Determine what type of placeholder is being used
     */
    public Placeholder {
    }

    /**
     * The replace function replaces the placeholder with the value.
     *
     * @param s Get the string that needs to be replaced
     * @return A string
     */
    public String replace(String s) {
        if (!stringContainsPlaceholder(s)) return s;
        if (value == null) throw new NullPointerException("The Value of Key " + key + " is null");
        if (value instanceof String) {
            StringBuilder result = new StringBuilder();
            int start = 0;
            int idx;
            while ((idx = s.indexOf(key, start)) != -1) {
                String prefix = s.substring(0, idx);
                result.append(s, start, idx);
                result.append(Utils.chat(value + ChatColor.getLastColors(prefix)));
                start = idx + key.length();
            }
            result.append(s.substring(start));
            return result.toString();
        }
        return s.replace(key, value.toString());
    }

    public boolean stringContainsPlaceholder(String s) {
        return s.contains(key);
    }
}
