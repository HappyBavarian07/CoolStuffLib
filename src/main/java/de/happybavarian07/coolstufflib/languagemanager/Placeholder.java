package de.happybavarian07.coolstufflib.languagemanager;/*
 * @Author HappyBavarian07
 * @Date 25.04.2022 | 17:07
 */

import de.happybavarian07.coolstufflib.utils.Utils;
import org.bukkit.ChatColor;

public class Placeholder {
    private final String key;
    private final Object value;
    private final PlaceholderType type;

    /**
     * The Placeholder function is used to create a placeholder object that can be
     * used in the Language Manager. The Placeholder function takes three parameters:
     *
     *
     *
     * @param key Identify the placeholder
     * @param value Store the value of the placeholder
     * @param type Determine what type of placeholder is being used
     */
    public Placeholder(String key, Object value, PlaceholderType type) {
        this.key = key;
        this.value = value;
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public PlaceholderType getType() {
        return type;
    }

    /**
     * The replace function replaces the placeholder with the value.
     *
     *
     * @param s Get the string that needs to be replaced
     *
     * @return A string
     */
    public String replace(String s) {
        if (!stringContainsPlaceholder(s)) return s;
        if (value == null) throw new NullPointerException("The Value of Key " + key + " is null");
        if (value instanceof String) {
            return s.replace(key, Utils.chat(value + ChatColor.getLastColors(s.split(key)[0])));
        }
        return s.replace(key, value.toString());
    }

    public boolean stringContainsPlaceholder(String s) {
        return s.contains(key);
    }
}
