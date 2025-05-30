package de.happybavarian07.coolstufflib.configstuff.advanced.interfaces;

import java.util.*;
import java.util.function.Consumer;

public interface AdvancedConfigGroup {
    /**
     * @return the name of this config group
     */
    String getName();

    /**
     * Adds a config to this group.
     * @param config the config to add
     */
    void addConfig(AdvancedConfig config);

    /**
     * Removes a config from this group.
     * @param config the config to remove
     */
    void removeConfig(AdvancedConfig config);

    /**
     * @return an immutable list of all configs in this group
     */
    List<AdvancedConfig> getConfigs();

    /**
     * Gets the values for a key from all configs in the group.
     * @param key the key to query
     * @return a map of config names to values
     */
    Map<String, Object> getValuesFromAll(String key);

    /**
     * Gets the first value of the given type for a key from any config in the group.
     * @param key the key to query
     * @param type the expected type
     * @param <T> the type
     * @return the first found value, or null if not found
     */
    <T> T getFirstValue(String key, Class<T> type);

    /**
     * Checks if any config in the group contains the given key.
     * @param key the key to check
     * @return true if found, false otherwise
     */
    boolean containsKeyInAny(String key);

    /**
     * @return all keys present in any config in the group
     */
    Set<String> getAllKeys();

    /**
     * Saves all configs in the group.
     */
    void saveAll();

    /**
     * Reloads all configs in the group.
     */
    void reloadAll();

    /**
     * Applies a consumer action to all configs in the group.
     * @param action the action to apply
     */
    void applyToAll(Consumer<AdvancedConfig> action);

    /**
     * Registers a group module.
     * @param module the module to register
     */
    void registerGroupModule(GroupConfigModule module);

    /**
     * Unregisters a group module by name.
     * @param name the name of the module
     */
    void unregisterGroupModule(String name);

    /**
     * @return all group modules registered with this group
     */
    Map<String, GroupConfigModule> getGroupModules();
}
