package de.happybavarian07.coolstufflib.configstuff.advanced.interfaces;

import java.util.Map;
import java.util.Set;

/**
 * <p>Interface for modules that operate at the configuration group level,
 * providing functionality that spans across multiple configurations within a group.</p>
 *
 * <p>Group modules enable:</p>
 * <ul>
 *   <li>Cross-configuration synchronization and validation</li>
 *   <li>Group-wide policy enforcement</li>
 *   <li>Aggregate operations and reporting</li>
 *   <li>Shared resource management</li>
 * </ul>
 *
 * <pre><code>
 * public class SyncModule implements GroupConfigModule {
 *     public void onConfigAdded(AdvancedConfig config) {
 *         // Sync new config with group policies
 *     }
 *
 *     public boolean appliesTo(AdvancedConfig config) {
 *         return config.getName().startsWith("server");
 *     }
 * }
 * </code></pre>
 */
public interface GroupConfigModule {

    /**
     * <p>Gets the unique identifier name for this group module.</p>
     *
     * <pre><code>
     * String moduleName = groupModule.getName();
     * </code></pre>
     *
     * @return the module name, never null
     */
    String getName();

    /**
     * <p>Gets a human-readable description of the module's functionality.</p>
     *
     * <pre><code>
     * String description = groupModule.getDescription();
     * </code></pre>
     *
     * @return the module description, never null
     */
    String getDescription();

    /**
     * <p>Gets the version string for this module implementation.</p>
     *
     * <pre><code>
     * String version = groupModule.getVersion();
     * </code></pre>
     *
     * @return the module version, never null
     */
    String getVersion();

    /**
     * <p>Initializes the module with a configuration group, preparing it for operation.</p>
     *
     * <pre><code>
     * SyncModule syncModule = new SyncModule();
     * syncModule.initialize(configGroup);
     * </code></pre>
     *
     * @param group the configuration group to attach to
     * @throws IllegalStateException if module is already initialized
     */
    void initialize(AdvancedConfigGroup group);

    /**
     * <p>Enables the module, activating its functionality across the configuration group.</p>
     *
     * <pre><code>
     * groupModule.enable();
     * assert groupModule.isEnabled();
     * </code></pre>
     *
     * @throws IllegalStateException if module is not initialized
     */
    void enable();

    /**
     * <p>Disables the module while preserving its configuration and state.</p>
     *
     * <pre><code>
     * groupModule.disable();
     * assert !groupModule.isEnabled();
     * </code></pre>
     */
    void disable();

    /**
     * <p>Performs complete cleanup, resetting the module to uninitialized state.</p>
     *
     * <pre><code>
     * groupModule.cleanup();
     * </code></pre>
     */
    void cleanup();

    /**
     * <p>Gets the configuration group this module is attached to.</p>
     *
     * <pre><code>
     * AdvancedConfigGroup group = groupModule.getGroup();
     * if (group != null) {
     *     // Module is initialized
     * }
     * </code></pre>
     *
     * @return the configuration group, or null if not initialized
     */
    AdvancedConfigGroup getGroup();

    /**
     * <p>Checks if the module is currently enabled and active.</p>
     *
     * <pre><code>
     * if (groupModule.isEnabled()) {
     *     // Module is processing group events
     * }
     * </code></pre>
     *
     * @return true if the module is enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * <p>Called when a new configuration is added to the group.</p>
     *
     * <pre><code>
     * public void onConfigAdded(AdvancedConfig config) {
     *     // Apply group policies to new config
     *     config.set("group.policy.enabled", true);
     * }
     * </code></pre>
     *
     * @param config the newly added configuration
     */
    void onConfigAdded(AdvancedConfig config);

    /**
     * <p>Called when a configuration is removed from the group.</p>
     *
     * <pre><code>
     * public void onConfigRemoved(AdvancedConfig config) {
     *     // Cleanup any resources associated with this config
     *     cleanupConfigResources(config.getName());
     * }
     * </code></pre>
     *
     * @param config the configuration being removed
     */
    void onConfigRemoved(AdvancedConfig config);

    /**
     * <p>Determines if this module should apply its functionality to the given configuration.</p>
     *
     * <pre><code>
     * public boolean appliesTo(AdvancedConfig config) {
     *     return config.getName().startsWith("server") ||
     *            config.getBoolean("sync.enabled", false);
     * }
     * </code></pre>
     *
     * @param config the configuration to check
     * @return true if the module should process this configuration, false otherwise
     */
    boolean appliesTo(AdvancedConfig config);

    /**
     * <p>Gets the set of module names that are required by this module to function properly.</p>
     *
     * <pre><code>
     * Set&lt;String&gt; dependencies = groupModule.getRequiredModules();
     * </code></pre>
     *
     * @return a set of required module names, can be empty but never null
     */
    Set<String> getRequiredModules();

    /**
     * <p>Gets the current state of the module, including any runtime data needed for operation.</p>
     *
     * <pre><code>
     * Map&lt;String, Object&gt; state = groupModule.getModuleState();
     * </code></pre>
     *
     * @return a map of state data, can be empty but never null
     */
    Map<String, Object> getModuleState();
}
