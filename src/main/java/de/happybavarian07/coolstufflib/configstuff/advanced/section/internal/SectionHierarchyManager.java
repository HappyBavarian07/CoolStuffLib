package de.happybavarian07.coolstufflib.configstuff.advanced.section.internal;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.BaseConfigSection;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SectionHierarchyManager {
    private final ConfigSection owner;
    private final Map<String, ConfigSection> sections;

    public SectionHierarchyManager(ConfigSection owner) {
        this.owner = owner;
        this.sections = new ConcurrentHashMap<>();
    }

    public Map<String, ConfigSection> getSubSections() {
        return Collections.unmodifiableMap(sections);
    }

    public Map<String, ConfigSection> getMutableSubSections() {
        return sections;
    }

    public ConfigSection getSection(String path) {
        if (path == null || path.isEmpty()) {
            return owner;
        }
        String[] parts = path.split("\\.", 2);
        String current = parts[0];
        ConfigSection section = sections.get(current);
        if (section == null) {
            return null;
        }
        if (parts.length == 1) {
            return section;
        } else {
            return section.getSection(parts[1]);
        }
    }

    public ConfigSection createSection(String path) {
        if (path == null || path.isEmpty()) {
            return owner;
        }
        String[] parts = path.split("\\.", 2);
        String current = parts[0];
        ConfigSection section = sections.get(current);
        if (section == null) {
            section = owner.createCustomSection(current, owner.getClass());
            if (section == null) {
                throw new IllegalStateException("Could not create section: " + current);
            }
            sections.put(current, section);
        }
        if (parts.length == 1) {
            return section;
        } else {
            return section.createSection(parts[1]);
        }
    }

    public boolean hasSection(String path) {
        return getSection(path) != null;
    }

    public void removeSection(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        String[] parts = path.split("\\.", 2);
        String current = parts[0];
        if (parts.length == 1) {
            sections.remove(current);
        } else {
            ConfigSection section = sections.get(current);
            if (section != null) {
                section.removeSection(parts[1]);
            }
        }
    }

    public void copyFrom(SectionHierarchyManager other) {
        if (other == null) return;
        sections.clear();
        for (Map.Entry<String, ConfigSection> entry : other.sections.entrySet()) {
            ConfigSection section = entry.getValue();
            if (section instanceof BaseConfigSection bcs) {
                BaseConfigSection sectionClone = bcs.clone();
                sectionClone.setParent(this.owner);
                sections.put(entry.getKey(), sectionClone);
            } else {
                sections.put(entry.getKey(), section);
            }
        }
    }

    public SectionHierarchyManager deepClone(ConfigSection newOwner) {
        SectionHierarchyManager clone = new SectionHierarchyManager(newOwner);
        for (Map.Entry<String, ConfigSection> entry : sections.entrySet()) {
            ConfigSection section = entry.getValue();
            if (section instanceof BaseConfigSection bcs) {
                BaseConfigSection sectionClone = bcs.clone();
                sectionClone.setParent(newOwner);
                clone.sections.put(entry.getKey(), sectionClone);
            } else {
                clone.sections.put(entry.getKey(), section);
            }
        }
        return clone;
    }
}
