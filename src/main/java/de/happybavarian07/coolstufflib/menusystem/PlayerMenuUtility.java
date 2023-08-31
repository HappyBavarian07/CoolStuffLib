package de.happybavarian07.coolstufflib.menusystem;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/*
Companion class to all menus. This is needed to pass information across the entire
 menu system no matter how many inventories are opened or closed.

 Each player has one of these objects, and only one.
 */

public class PlayerMenuUtility {

    private final Player owner;
    private Player target;
    // Stores all temporary Data for the PlayerMenuUtility
    private final Map<String, Object> data = new HashMap<>();

    public PlayerMenuUtility(Player p) {
        this.owner = p;
    }

    public Player getOwner() {
        return owner;
    }

    public Player getTarget() {
        return target;
    }

    public void setTarget(Player target) {
        this.target = target;
    }

    public Object getData(String key) {
        return data.get(key);
    }

    public Object getData(String key, Object defaultValue) {
        return data.getOrDefault(key, defaultValue);
    }

    public void setData(String key, Object value, boolean replace) {
        if (replace && data.containsKey(key))
            data.replace(key, value);
        else
            data.put(key, value);
    }

    public void addData(String key, Object value) {
        if (!data.containsKey(key))
            data.put(key, value);
    }

    public void removeData(String key) {
        data.remove(key);
    }

    public void replaceData(String key, Object value) {
        if (!data.containsKey(key)) return;
        data.replace(key, value);
    }
}

