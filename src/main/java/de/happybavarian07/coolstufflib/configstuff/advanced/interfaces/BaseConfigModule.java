package de.happybavarian07.coolstufflib.configstuff.advanced.interfaces;

/**
 * This interface defines a module for an AdvancedConfig.
 * Modules can be used to extend the functionality of an AdvancedConfig.
 * They can be enabled or disabled, and they can react to changes in the config.
 * <p>
 * Modules can be used for various purposes, such as adding custom validation,
 * handling specific data types, or providing additional features.
 * </p>
 * By implementing this interface, developers can create custom modules that integrate seamlessly with the AdvancedConfig system.
 * * <p>
 *
 * @author HappyBavarian07
 * @since 1.0
 */
public interface BaseConfigModule {
    /**
     * Get the name of the module.
     * @return The name of the module.
     */
    String getName();

    /**
     * Check if the module is enabled.
     * @return true if the module is enabled, false if not.
     */
    boolean isEnabled();

    /**
     * Set the enabled state of the module.
     * @param enabled true to enable the module, false to disable it.
     */
    void setEnabled(boolean enabled);

    /**
     * During Enabling of the module.
     */
    void enable();

    /**
     * During Disabling of the module.
     */
    void disable();

    /**
     * Called when the module is attached to an AdvancedConfig.
     * @param config The AdvancedConfig that the module is attached to.
     */
    void onAttach(AdvancedConfig config);

    /**
     * Called when the module is detached from an AdvancedConfig.
     */
    void onDetach();

    /**
     * Reload the module.
     */
    void reload();

    /**
     * Save the module.
     */
    void save();

    /**
     * Called when a value is retrieved from the config.
     * @param key The key of the value.
     * @param value The value.
     * @return The value or a modified version of the value.
     */
    Object onGetValue(String key, Object value);

    /**
     * Called when the config is changed.
     * @param key The key of the changed value.
     * @param oldValue The old value.
     * @param newValue The new value.
     */
    void onConfigChange(String key, Object oldValue, Object newValue);

    /**
     * Check if the module supports the given config.
     * @param config The config to check.
     * @return true if the module supports the config, false if not.
     */
    boolean supportsConfig(AdvancedConfig config);

    /**
     * Get the state of the module.
     * @return The state of the module as a map.
     */
    java.util.Map<String, Object> getModuleState();

    /**
     * Get the AdvancedConfig that the module is attached to.
     * @return The AdvancedConfig that the module is attached to.
     */
    AdvancedConfig getConfig();
}
