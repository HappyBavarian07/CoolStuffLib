package de.happybavarian07.coolstufflib.configstuff.advanced.event;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;

public class ConfigChangeEvent extends ConfigEvent {
    private final ChangeType changeType;
    private final String path;
    private final Object oldValue;
    private final Object newValue;

    public ConfigChangeEvent(AdvancedConfig config, ChangeType changeType, String path, Object oldValue, Object newValue) {
        super(config);
        this.changeType = changeType;
        this.path = path;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public String getPath() {
        return path;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    @Override
    public boolean isCancellable() {
        return true;
    }

    public enum ChangeType {
        VALUE_CHANGE,
        SECTION_CHANGE,
        SECTION_CREATION,
        SECTION_REMOVAL
    }
}
