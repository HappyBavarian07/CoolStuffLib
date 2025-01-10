package de.happybavarian07.coolstufflib.menusystem;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/*
Companion class to all menus. This is needed to pass information across the entire
 menu system no matter how many inventories are opened or closed.

 Each player has one of these objects, and only one.
 */

/**
 * <p>
 * The {@code PlayerMenuUtility} class serves as a utility for managing and maintaining menu-related data and
 * player-specific information. It provides functionality for data storage, setting and retrieving player-focused
 * details, and ensures a streamlined approach to handling temporary or session-based data for menus in a Minecraft server environment.
 * </p>
 *
 * <h2>Features:</h2>
 * <ul>
 * <li>Stores the UUID of the owner (player who initialized or owns this utility).</li>
 * <li>Allows for setting and retrieving the UUID of a target player if applicable.</li>
 * <li>Implements a flexible key-value storage system for managing temporary or session-specific data.</li>
 * <li>Provides methods for data retrieval with optional type safety and default values.</li>
 * <li>Supports operations to add, replace, and remove stored data entries efficiently.</li>
 * </ul>
 *
 * <h2>Constructor:</h2>
 * <p>The class provides a constructor to initialize it with the owner's {@code UUID}, which serves as a foundational reference for the utility's operations.</p>
 *
 * <h3>Key Methods:</h3>
 * <ul>
 * <li>{@code getOwnerUUID()} - Retrieves the {@code UUID} of the owner of this utility.</li>
 * <li>{@code getOwner()} - Fetches the {@code Player} corresponding to the owner's {@code UUID}.</li>
 * <li>{@code getTarget()} - Fetches the {@code Player} corresponding to the target's {@code UUID}, if set.</li>
 * <li>{@code setTarget(Player target)} - Sets the target as a {@code Player} object, automatically storing its {@code UUID}.</li>
 * <li>{@code setTargetUUID(UUID targetUUID)} - Directly sets the target's {@code UUID}.</li>
 * <li>{@code getData(String key)} - Fetches data associated with the specified key in the storage map.</li>
 * <li>{@code getData(String key, Class<T> valueType)} - Attempts to fetch data associated with a key and casts it to the given type.</li>
 * <li>{@code getData(String key, Object defaultValue)} - Retrieves data by a key, providing a fallback value when the key is not present.</li>
 * <li>{@code getData(String key, Class<T> valueType, T defaultValue)} - Combines type-safe casting and default return when data is unavailable.</li>
 * <li>{@code setData(String key, Object value, boolean replace)} - Saves data while optionally replacing existing data associated with a key.</li>
 * <li>{@code addData(String key, Object value)} - Adds new data unless a value is already associated with the provided key.</li>
 * <li>{@code removeData(String key)} - Removes data linked to a specific key.</li>
 * <li>{@code replaceData(String key, Object value)} - Replaces data for a key only if it exists in the storage map.</li>
 * <li>{@code hasData(String key)} - Checks if a specific key exists in the storage map.</li>
 * </ul>
 *
 * <h2>Use Case:</h2>
 * <p>This class is structured to facilitate menu handling in Minecraft plugins, enabling seamless data management
 * for individual player menus. By leveraging the key-value storage and player-targeting mechanisms, developers
 * can efficiently customize, extend, and utilize player-specific configurations and session-based data sharing.</p>
 */
public class PlayerMenuUtility {

    /**
     * <p>Represents the unique identifier of the owner associated with an instance of the containing class.</p>
     *
     * <p>This variable is used to uniquely identify the owner, typically a player or entity,
     * ensuring any operations or context-relevant data are tied to the specific instance of that owner.</p>
     *
     * <p><b>Immutable:</b> This field is declared as {@code final
     */
    private final UUID ownerUUID;
    /**
     *
     */
    private UUID targetUUID;

    /**
     *
     */
    // Stores all temporary Data for the PlayerMenuUtility
    private final Map<String, Object> data = new HashMap<>();

    /**
     * Constructs a new instance of {@code PlayerMenuUtility} for the specified player.
     * This utility provides a way to interact with and manage menu-related functionality
     * tied to a specific player.
     *
     * <p>
     * The {@code ownerUUID} parameter represents the UUID of the owner of this menu utility.
     * This is used to identify the player associated with this utility object.
     **/
    public PlayerMenuUtility(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    /**
     * Retrieves the UUID of the owner associated with this instance.
     *
     * <p>
     * This method returns the {@link UUID} that represents the unique identifier
     * of the owner entity for the {@code PlayerMenuUtility}
     */
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    /**
     * Retrieves the owner of this {@code PlayerMenuUtility} instance.
     *
     * <p>This method uses the {@code ownerUUID} field to identify the associated
     * player and retrieves the corresponding {@link Player} object using
     * {@link Bukkit#getPlayer(UUID)}.
     * </p>
     *
     * <p>If the UUID does not correspond to an online player, this method will return {@code null}.
     * </p>
     *
     * @return The {@link Player} associated with the owner UUID of this instance,
     * or {@code null} if the owner is not currently online.
     */
    public Player getOwner() {
        return Bukkit.getPlayer(getOwnerUUID());
    }

    /**
     * Retrieves the target player associated with this {@code PlayerMenuUtility} instance.
     *
     * <p>This method uses the UUID stored in the {@code targetUUID} field to identify the
     * targeted player. It then retrieves the corresponding {@link Player} object using
     * {@link Bukkit#getPlayer(UUID)}.
     * </p>
     *
     * <p>Note that this method will return {@code null} if the target UUID does not correspond to
     * an online player or if no target UUID has been set.
     * </p>
     *
     * @return The {@link Player} instance associated with the target UUID of this instance,
     * or {@code null} if the target is not currently online or no target has been set.
     */
    public Player getTarget() {
        return Bukkit.getPlayer(getTargetUUID());
    }

    /**
     * Retrieves the UUID representing the current target associated with this {@code PlayerMenuUtility}.
     *
     * <p>This method provides access to the value stored in the {@code targetUUID} field,
     * which represents the unique identifier of the target player or entity in the
     * context of this utility.</p>
     *
     * @return The {@link UUID} of the target associated with this instance,
     * or {@code null} if no target has been set.
     */
    public UUID getTargetUUID() {
        return targetUUID;
    }

    /**
     * Sets the target of this {@code PlayerMenuUtility} instance to the specified player.
     * <p>
     * This method assigns the unique identifier (UUID) of the provided {@link Player}
     * to the internal {@code targetUUID} field, which represents the player
     * that is the target of this utility in the given context.
     * </p>
     *
     * @param target The {@link Player} instance to set as the target. Must not be {@code null}.
     */
    public void setTarget(Player target) {
        this.targetUUID = target.getUniqueId();
    }

    /**
     * Sets the target UUID for the current instance.
     * <p>
     * This method assigns the provided UUID to the targetUUID field,
     * which is intended to represent the target player or entity in the associated context.
     *
     * @param targetUUID The UUID of the target to be stored.
     */
    public void setTargetUUID(UUID targetUUID) {
        this.targetUUID = targetUUID;
    }

    /**
     * Retrieves the data associated with the specified key from the data map.
     *
     * <p>This method is used to fetch a value stored in the internal data structure
     * by providing the corresponding key.</p>
     *
     * @param key The key used to identify the data to retrieve.
     * @return The value associated with the provided key, or {@code null} if the key does not
     */
    public Object getData(String key) {
        return data.get(key);
    }

    /**
     * Retrieves a value from the internal data storage and attempts to cast it to the specified type.
     * <p>
     * The method fetches the value associated with the provided key and ensures that its type matches
     * the specified valueType. If the value cannot be cast to the specified type or does not exist,
     * the method returns null.
     * </p>
     *
     * @param key The key to identify the value in the internal data
     */
    public <T> T getData(String key, Class<T> valueType) {
        Object value = data.get(key);
        if (!valueType.isInstance(value)) {
            return null; // Return null if the key is not found or the value cannot be cast.
        }
        return valueType.cast(value);
    }

    /**
     * Retrieves a value associated with the specified key from the internal data map,
     * or returns the provided default value if the key does not exist.
     *
     * <p>This method provides a way to safely retrieve data entries from the internal
     * storage while ensuring a fallback value is provided if the requested key is not found.</p>
     *
     * @param key          The key used to identify the data to retrieve. Must not be null.
     * @param defaultValue The value to return if the key does not exist in the data map.
     * @return The value associated with the provided key, or the {@code defaultValue}
     * if the key does not exist in the data map.
     */
    public Object getData(String key, Object defaultValue) {
        return data.getOrDefault(key, defaultValue);
    }

    /**
     * Retrieves data associated with a specified key, casting it to the desired type, or returns a default value if
     * the data is not present or not of the expected type.
     * <p>
     * This method allows for safe fetching and type casting of stored data.
     * </p>
     *
     * @param <T> The type of the value expected to be retrieved.
     * @param key
     */
    public <T> T getData(String key, Class<T> valueType, T defaultValue) {
        Object value = data.get(key);
        if (!valueType.isInstance(value)) {
            return defaultValue;
        }
        return valueType.cast(value);
    }


    /**
     * Sets a data entry in the internal data storage.
     *
     * <p>This method allows adding or updating a key-value pair in the data map. If the {@code replace}
     * parameter is set to {@code true} and the key already exists, the value associated with the key will be
     * updated. Otherwise, a new entry will be added to the map if the key does not exist.</p>
     *
     * @param key     The key for the data entry to be added or updated.
     * @param value   The value to be associated with the specified key.
     * @param replace A boolean flag indicating whether to replace the existing value if the key already exists.
     */
    public void setData(String key, Object value, boolean replace) {
        if (replace && data.containsKey(key))
            data.replace(key, value);
        else
            data.put(key, value);
    }

    /**
     * Adds a new data entry to the internal data map associated with the given key, if the key does not already exist.
     *
     * <p>This method checks if the specified key is already present in the internal data storage.
     * If the key is not found, it inserts the provided key-value pair into the data map.
     * If the key is already present, the method does nothing.</p>
     *
     * @param key   The key for the data entry to be added. Must be a non-null, unique identifier.
     * @param value The value to associate with the specified key. Can be any object or null.
     */
    public void addData(String key, Object value) {
        if (!data.containsKey(key))
            data.put(key, value);
    }

    /**
     * Removes a data entry associated with the specified key.
     * <p>
     * This method removes the entry associated with the provided
     * key from the underlying data storage. If the key does not exist,
     * the
     */
    public void removeData(String key) {
        data.remove(key);
    }

    /**
     * Replaces the value associated with a specific key in the internal data storage
     * if the key is already present.
     *
     * <p>This method checks if the provided key exists in the data storage. If the key exists,
     * the value associated with it will be replaced by the provided value. If the key does not exist,
     * no operation will be performed.</p>
     */
    public void replaceData(String key, Object value) {
        if (!data.containsKey(key)) return;
        data.replace(key, value);
    }

    /**
     * Checks whether the specified key exists within the internal data storage.
     *
     * <p>This method determines if there is an entry associated with the specified key
     * in the internal data map.</p>
     *
     * @param key The key to be checked in the internal data storage. Must not be null.
     * @return {@code true} if the key exists in the data storage, {@code false} otherwise.
     */
    public boolean hasData(String key) {
        return data.containsKey(key);
    }
}

