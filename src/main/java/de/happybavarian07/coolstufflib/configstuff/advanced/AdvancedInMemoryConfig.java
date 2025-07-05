package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigLifecycleEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigFileType;

import java.io.File;

public class AdvancedInMemoryConfig extends BaseAdvancedConfig {

    public AdvancedInMemoryConfig(String name) {
        super(name, null, ConfigFileType.MEMORY);
        getEventBus().publish(ConfigLifecycleEvent.configLoad(this));
    }

    @Override
    public void save() {
        lockValuesWrite();
        try {
            getEventBus().publish(ConfigLifecycleEvent.configSave(this));
        } finally {
            unlockValuesWrite();
        }
    }

    @Override
    public void reload() {
        lockValuesWrite();
        try {
            // In-memory config reload just means notifying listeners
            getEventBus().publish(ConfigLifecycleEvent.configReload(this));
        } finally {
            unlockValuesWrite();
        }
    }
}
