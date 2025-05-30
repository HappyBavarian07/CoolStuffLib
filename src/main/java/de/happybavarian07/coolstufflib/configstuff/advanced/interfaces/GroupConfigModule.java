package de.happybavarian07.coolstufflib.configstuff.advanced.interfaces;

import java.util.Map;
import java.util.function.Consumer;

public interface GroupConfigModule {
    /**
     * @return the name of the group module
     */
    String getName();

    /**
     * Called when this module is attached to a config group.
     *
     * @param group the group being attached
     */
    void onGroupAttach(AdvancedConfigGroup group);

    /**
     * Called when this module is detached from its config group.
     */
    void onGroupDetach();

    /**
     * @return true if the module is enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Sets the enabled state of the module.
     *
     * @param enabled true to enable, false to disable
     */
    void setEnabled(boolean enabled);

    /**
     * Enables the module.
     */
    void enable();

    /**
     * Disables the module.
     */
    void disable();

    /**
     * @return the config group this module is attached to
     */
    AdvancedConfigGroup getGroup();

    /**
     * Reloads the module state.
     */
    void reload();

    /**
     * Saves the module state.
     */
    void save();

    /**
     * Called when an operation is applied to the group.
     */
    void onGroupApply(Consumer<AdvancedConfig> action);

    /**
     * Determines if this module supports the provided config.
     * @param config the config to check
     * @return true if supported, false otherwise
     */
    boolean supportsConfig(AdvancedConfig config);

    /**
     * Called when values for a key are retrieved from all configs in the group.
     * @param key the key being queried
     * @param values the map of config names to values
     */
    void onGetValuesFromAll(String key, Map<String, Object> values);

    /**
     * Called when the first value for a key is retrieved from the group.
     * @param key the key being queried
     * @param type the expected value type
     * @param defaultValue the default value if not found
     * @return the found value or default
     */
    <T> T onGetFirstValue(String key, Class<T> type, T defaultValue);

    /**
     * Called when a config is added to the group.
     * @param config the config being added
     */
    void onConfigAdded(AdvancedConfig config);

    /**
     * Called when a config is removed from the group.
     * @param config the config being removed
     */
    void onConfigRemoved(AdvancedConfig config);
}
