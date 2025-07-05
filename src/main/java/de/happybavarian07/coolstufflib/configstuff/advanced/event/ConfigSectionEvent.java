package de.happybavarian07.coolstufflib.configstuff.advanced.event;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;

public class ConfigSectionEvent extends ConfigEvent {
    private final ConfigSection parentSection;
    private final String path;
    private final ConfigSection section;
    private final Type eventType;

    public static ConfigSectionEvent sectionCreated(AdvancedConfig config, ConfigSection parentSection, String path, ConfigSection section) {
        return new ConfigSectionEvent(Type.CREATED, config, parentSection, path, section);
    }

    public static ConfigSectionEvent sectionRemoved(AdvancedConfig config, ConfigSection parentSection, String path, ConfigSection section) {
        return new ConfigSectionEvent(Type.REMOVED, config, parentSection, path, section);
    }

    private ConfigSectionEvent(Type eventType, AdvancedConfig config, ConfigSection parentSection, String path, ConfigSection section) {
        super(config);
        this.eventType = eventType;
        this.parentSection = parentSection;
        this.path = path;
        this.section = section;
    }

    public Type getType() {
        return eventType;
    }

    @Override
    public Object getSource() {
        return parentSection != null ? parentSection : getConfig();
    }

    public ConfigSection getParentSection() {
        return parentSection;
    }

    public String getPath() {
        return path;
    }

    public ConfigSection getSection() {
        return section;
    }

    public String getFullPath() {
        return parentSection != null && !parentSection.getFullPath().isEmpty()
               ? parentSection.getFullPath() + "." + path
               : path;
    }

    @Override
    public boolean isCancellable() {
        return true; // Section events can be cancelled
    }

    public enum Type {
        CREATED,
        REMOVED
    }
}
