package de.happybavarian07.coolstufflib.languagemanager;

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
     * <p>Replaces the placeholder in the given message.</p>
     *
     * @param s The input string
     * @return The string with the placeholder replaced
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

    /**
     * <p>Checks if the string contains the placeholder.</p>
     *
     * @param s The string
     * @return {@code true} if present
     */
    public boolean stringContainsPlaceholder(String s) {
        return s.contains(key);
    }
}
