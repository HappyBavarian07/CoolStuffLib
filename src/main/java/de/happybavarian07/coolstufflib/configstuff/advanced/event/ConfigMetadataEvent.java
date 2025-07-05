package de.happybavarian07.coolstufflib.configstuff.advanced.event;

import de.happybavarian07.coolstufflib.configstuff.advanced.BaseAdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;

/*
 * @Author HappyBavarian07
 * @Date Juni 27, 2025 | 17:49
 */
public class ConfigMetadataEvent extends ConfigEvent {
    private final MetadataOperation operation;
    private final String key;
    private final Object value;
    private final Object oldValue;

    public ConfigMetadataEvent(AdvancedConfig config, MetadataOperation operation, String key, Object value) {
        super(config);
        this.operation = operation;
        this.key = key;
        this.value = value;
        this.oldValue = null;
    }

    public ConfigMetadataEvent(AdvancedConfig config, MetadataOperation operation, String key, Object value, Object oldValue) {
        super(config);
        this.operation = operation;
        this.key = key;
        this.value = value;
        this.oldValue = oldValue;
    }

    public static <T> ConfigEvent metadataAdded(BaseAdvancedConfig baseAdvancedConfig, String name, T value) {
        return new ConfigMetadataEvent(baseAdvancedConfig, MetadataOperation.SET, name, value);
    }

    public MetadataOperation getOperation() {
        return operation;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public Object getOldValue() {
        return oldValue;
    }

    @Override
    public boolean isCancellable() {
        return true;
    }

    public enum MetadataOperation {
        SET,
        REMOVE,
        CLEAR
    }
}
