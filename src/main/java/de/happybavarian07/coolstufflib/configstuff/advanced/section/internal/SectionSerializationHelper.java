package de.happybavarian07.coolstufflib.configstuff.advanced.section.internal;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import java.util.*;

public class SectionSerializationHelper {
    private final ConfigSection owner;
    public SectionSerializationHelper(ConfigSection owner) {
        this.owner = owner;
    }
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>(owner.getValues(false));
        for (Map.Entry<String, ConfigSection> entry : owner.getSubSections().entrySet()) {
            result.put(entry.getKey(), entry.getValue().toMap());
        }
        return result;
    }
    public List<Object> toList() {
        List<Object> result = new ArrayList<>(owner.getValues(false).values());
        for (ConfigSection section : owner.getSubSections().values()) {
            result.add(section.toList());
        }
        return result;
    }
    public Set<Object> toSet() {
        Set<Object> result = new HashSet<>(owner.getValues(false).values());
        for (ConfigSection section : owner.getSubSections().values()) {
            result.add(section.toSet());
        }
        return result;
    }
    public Map<String, Object> toSerializableMap() {
        Map<String, Object> result = new HashMap<>(owner.getValues(false));
        for (Map.Entry<String, ConfigSection> entry : owner.getSubSections().entrySet()) {
            result.put(entry.getKey(), entry.getValue().toSerializableMap());
        }
        return result;
    }
}
