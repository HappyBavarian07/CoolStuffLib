package de.happybavarian07.coolstufflib.menusystem;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import de.happybavarian07.coolstufflib.CoolStuffLib;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <p>Companion class to all menus to pass information across the entire menu system.</p>
 */
public class PlayerMenuUtility {

    private final CoolStuffLib lib;
    private final UUID ownerUUID;
    private UUID targetUUID;
    private final Map<String, Object> data = new HashMap<>();

    /**
     * <p>Constructs a new instance.</p>
     *
     * @param lib       The library instance
     * @param ownerUUID The owner's UUID
     */
    public PlayerMenuUtility(CoolStuffLib lib, UUID ownerUUID) {
        this.lib = lib;
        this.ownerUUID = ownerUUID;
    }

    public CoolStuffLib getLib() {
        return lib;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public Player getOwner() {
        return Bukkit.getPlayer(getOwnerUUID());
    }

    public Player getTarget() {
        return Bukkit.getPlayer(getTargetUUID());
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }

    public void setTarget(Player target) {
        this.targetUUID = target.getUniqueId();
    }

    public void setTargetUUID(UUID targetUUID) {
        this.targetUUID = targetUUID;
    }

    public Object getData(String key) {
        return data.get(key);
    }

    public <T> T getData(String key, Class<T> valueType) {
        Object value = data.get(key);
        if (!valueType.isInstance(value)) {
            return null;
        }
        return valueType.cast(value);
    }

    public Object getData(String key, Object defaultValue) {
        return data.getOrDefault(key, defaultValue);
    }

    public <T> T getData(String key, Class<T> valueType, T defaultValue) {
        Object value = data.get(key);
        if (!valueType.isInstance(value)) {
            return defaultValue;
        }
        return valueType.cast(value);
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

    public boolean hasData(String key) {
        return data.containsKey(key);
    }
}
