package de.happybavarian07.coolstufflib.configstuff.advanced.event;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.BaseConfigModule;

public class ConfigModuleEvent extends ConfigEvent {
    private final BaseConfigModule module;
    private final Type eventType;

    public static ConfigModuleEvent moduleRegistered(AdvancedConfig config, BaseConfigModule module) {
        return new ConfigModuleEvent(Type.REGISTERED, config, module);
    }

    public static ConfigModuleEvent moduleUnregistered(AdvancedConfig config, BaseConfigModule module) {
        return new ConfigModuleEvent(Type.UNREGISTERED, config, module);
    }

    public static ConfigModuleEvent moduleEnabled(AdvancedConfig config, BaseConfigModule module) {
        return new ConfigModuleEvent(Type.ENABLED, config, module);
    }

    public static ConfigModuleEvent moduleDisabled(AdvancedConfig config, BaseConfigModule module) {
        return new ConfigModuleEvent(Type.DISABLED, config, module);
    }

    private ConfigModuleEvent(Type eventType, AdvancedConfig config, BaseConfigModule module) {
        super(config);
        this.eventType = eventType;
        this.module = module;
    }

    public Type getType() {
        return eventType;
    }

    public BaseConfigModule getModule() {
        return module;
    }

    public enum Type {
        REGISTERED,
        UNREGISTERED,
        ENABLED,
        DISABLED
    }
}
