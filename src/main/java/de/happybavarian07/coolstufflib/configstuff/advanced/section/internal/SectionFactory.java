package de.happybavarian07.coolstufflib.configstuff.advanced.section.internal;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;

public class SectionFactory {
    private final ConfigSection owner;
    public SectionFactory(ConfigSection owner) {
        this.owner = owner;
    }
    public <T extends ConfigSection> T createCustomSection(String name, Class<T> clazz) {
        if (name == null || name.isEmpty() || clazz == null) return null;
        try {
            return clazz.getConstructor(String.class, ConfigSection.class).newInstance(name, owner);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate custom section: " + clazz.getName(), e);
        }
    }
}
