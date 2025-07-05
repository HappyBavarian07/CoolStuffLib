package de.happybavarian07.coolstufflib.configstuff.advanced.event;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;

public class ConfigValueEvent extends ConfigEvent {
    private final String key;
    private final Object oldValue;
    private Object newValue;
    private final ConfigSection section;
    private final Type eventType;

    public static ConfigValueEvent valueSet(AdvancedConfig config, ConfigSection section, String key, Object oldValue, Object newValue) {
        return new ConfigValueEvent(Type.SET, config, section, key, oldValue, newValue);
    }

    public static ConfigValueEvent valueGet(AdvancedConfig config, ConfigSection section, String key, Object value) {
        return new ConfigValueEvent(Type.GET, config, section, key, null, value);
    }

    public static ConfigValueEvent valueRemove(AdvancedConfig config, ConfigSection section, String key, Object oldValue) {
        return new ConfigValueEvent(Type.REMOVE, config, section, key, oldValue, null);
    }

    private ConfigValueEvent(Type eventType, AdvancedConfig config, ConfigSection section, String key, Object oldValue, Object newValue) {
        super(config);
        this.eventType = eventType;
        this.section = section;
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Type getType() {
        return eventType;
    }

    @Override
    public Object getSource() {
        return section != null ? section : getConfig();
    }

    public String getKey() {
        return key;
    }

    public String getFullPath() {
        return section != null ?
            (section.getFullPath().isEmpty() ? key : section.getFullPath() + "." + key) :
            key;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }

    public ConfigSection getSection() {
        return section;
    }

    @Override
    public boolean isCancellable() {
        // Only SET and REMOVE operations can be cancelled
        return eventType == Type.SET || eventType == Type.REMOVE;
    }

    public enum Type {
        SET,
        GET,
        REMOVE
    }
}
