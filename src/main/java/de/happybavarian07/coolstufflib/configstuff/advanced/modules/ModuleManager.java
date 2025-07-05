package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEventBus;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigModuleEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.BaseConfigModule;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.core.ConfigLockManager;
import java.util.HashMap;
import java.util.Map;

public class ModuleManager {
    private final Map<String, BaseConfigModule> modules = new HashMap<>();
    private final AdvancedConfig config;
    private final ConfigEventBus eventBus;
    private final ConfigLockManager lockManager;
    public ModuleManager(AdvancedConfig config, ConfigEventBus eventBus, ConfigLockManager lockManager) {
        this.config = config;
        this.eventBus = eventBus;
        this.lockManager = lockManager;
    }
    public void registerModule(BaseConfigModule module) {
        if (module == null) return;
        lockManager.lockModuleWrite();
        try {
            String name = module.getName();
            if (!modules.containsKey(name)) {
                module.initialize(config);
                module.enable();
                modules.put(name, module);
                eventBus.publish(ConfigModuleEvent.moduleRegistered(config, module));
            }
        } finally { lockManager.unlockModuleWrite(); }
    }
    public void unregisterModule(String name) {
        if (name == null) return;
        lockManager.lockModuleWrite();
        try {
            BaseConfigModule m = modules.remove(name);
            if (m != null) {
                m.disable();
                m.cleanup();
                eventBus.publish(ConfigModuleEvent.moduleUnregistered(config, m));
            }
        } finally { lockManager.unlockModuleWrite(); }
    }
    public boolean hasModule(BaseConfigModule module) {
        return module != null && hasModule(module.getName());
    }
    public boolean hasModule(String name) {
        if (name == null) return false;
        lockManager.lockModuleRead();
        try { return modules.containsKey(name); }
        finally { lockManager.unlockModuleRead(); }
    }
    public BaseConfigModule getModuleByName(String name) {
        if (name == null) return null;
        lockManager.lockModuleRead();
        try { return modules.get(name); }
        finally { lockManager.unlockModuleRead(); }
    }
    public void enableModule(String name) {
        BaseConfigModule m = getModuleByName(name);
        if (m != null) {
            m.enable();
            eventBus.publish(ConfigModuleEvent.moduleEnabled(config, m));
        }
    }
    public void disableModule(String name) {
        BaseConfigModule m = getModuleByName(name);
        if (m != null) {
            m.disable();
            eventBus.publish(ConfigModuleEvent.moduleDisabled(config, m));
        }
    }
    public Map<String, BaseConfigModule> getModules() {
        lockManager.lockModuleRead();
        try { return new HashMap<>(modules); }
        finally { lockManager.unlockModuleRead(); }
    }
}
