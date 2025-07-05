package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.BaseConfigModule;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.AbstractGroupConfigModule;

import java.util.Map;
import java.util.Set;

/**
 * <p>Default implementation of GroupConfigModule that manages individual BaseConfigModule
 * instances across all configurations in a group. Automatically handles module lifecycle
 * events when configurations are added or removed from the group.</p>
 *
 * <p>This module provides:</p>
 * <ul>
 * <li>Automatic module registration for new configurations</li>
 * <li>Module cloning to prevent shared state issues</li>
 * <li>Lifecycle synchronization between group and individual modules</li>
 * <li>Clean removal when configurations leave the group</li>
 * </ul>
 *
 * <pre><code>
 * BaseConfigModule validationModule = new ValidationModule();
 * DefaultGroupConfigModule groupModule = new DefaultGroupConfigModule(validationModule);
 * group.registerGroupModule(groupModule);
 * </code></pre>
 */
class DefaultGroupConfigModule extends AbstractGroupConfigModule {
    private final BaseConfigModule module;

    public DefaultGroupConfigModule(BaseConfigModule module) {
        super(module.getName(), module.getDescription(), module.getVersion());
        this.module = module;
    }

    @Override
    protected void onInitialize() {
        for (AdvancedConfig config : getGroup().getConfigs().values()) {
            onConfigAdded(config);
        }
    }

    @Override
    protected void onEnable() {
        for (AdvancedConfig config : getGroup().getConfigs().values()) {
            if (config.hasModule(module.getName())) {
                BaseConfigModule configModule = config.getModuleByName(module.getName());
                if (configModule != null && configModule.getState() == BaseConfigModule.ModuleState.INITIALIZED) {
                    configModule.enable();
                }
            }
        }
    }

    @Override
    protected void onDisable() {
        for (AdvancedConfig config : getGroup().getConfigs().values()) {
            if (config.hasModule(module.getName())) {
                BaseConfigModule configModule = config.getModuleByName(module.getName());
                if (configModule != null && configModule.getState() == BaseConfigModule.ModuleState.ENABLED) {
                    configModule.disable();
                }
            }
        }
    }

    @Override
    protected void onCleanup() {
        for (AdvancedConfig config : getGroup().getConfigs().values()) {
            if (config.hasModule(module.getName())) {
                config.getModuleByName(module.getName()).cleanup();
            }
        }
    }

    /**
     * <p>Handles the addition of a new configuration to the group by registering and
     * initializing the associated module. Attempts to clone the module to prevent
     * shared state issues between configurations.</p>
     *
     * <pre><code>
     * groupModule.onConfigAdded(newConfig);
     * // newConfig now has the module registered and initialized
     * </code></pre>
     *
     * @param config the configuration that was added to the group
     */
    public void onConfigAdded(AdvancedConfig config) {
        if (!config.hasModule(module.getName())) {
            try {
                // Clone the module for the config
                Class<? extends BaseConfigModule> moduleClass = module.getClass();
                BaseConfigModule clonedModule = moduleClass.getDeclaredConstructor().newInstance();
                config.registerModule(clonedModule);
                clonedModule.initialize(config);
                if (isEnabled()) {
                    clonedModule.enable();
                }
            } catch (Exception e) {
                // Fallback to using the original module if cloning fails
                if (!config.hasModule(module)) {
                    config.registerModule(module);
                    if (isEnabled()) {
                        module.enable();
                    }
                }
            }
        }
    }

    /**
     * <p>Handles the removal of a configuration from the group by properly disabling,
     * cleaning up, and unregistering the associated module to prevent resource leaks.</p>
     *
     * <pre><code>
     * groupModule.onConfigRemoved(oldConfig);
     * // oldConfig no longer has the module and resources are cleaned up
     * </code></pre>
     *
     * @param config the configuration that was removed from the group
     */
    @Override
    public void onConfigRemoved(AdvancedConfig config) {
        if (config.hasModule(module.getName())) {
            BaseConfigModule configModule = config.getModuleByName(module.getName());
            if (configModule != null) {
                configModule.disable();
                configModule.cleanup();
            }
            config.unregisterModule(module.getName());
        }
    }

    /**
     * <p>Determines whether this group module applies to a specific configuration
     * by checking if the configuration already has the associated module registered.</p>
     *
     * <pre><code>
     * if (groupModule.appliesTo(config)) {
     *     // Module is relevant to this configuration
     * }
     * </code></pre>
     *
     * @param config the configuration to check
     * @return true if the configuration has the associated module, false otherwise
     */
    @Override
    public boolean appliesTo(AdvancedConfig config) {
        return config.hasModule(module.getName());
    }

    @Override
    public Set<String> getRequiredModules() {
        return module.getDependencies();
    }

    @Override
    protected Map<String, Object> getAdditionalModuleState() {
        return Map.of(
                "moduleName", module.getName(),
                "moduleDescription", module.getDescription(),
                "moduleVersion", module.getVersion()
        );
    }
}
