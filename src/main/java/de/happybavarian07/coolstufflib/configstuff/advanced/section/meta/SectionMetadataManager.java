package de.happybavarian07.coolstufflib.configstuff.advanced.section.meta;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SectionMetadataManager {
    private final Map<String, Object> metadata;
    public SectionMetadataManager() {
        this.metadata = new ConcurrentHashMap<>();
    }
    public void addMetadata(String key, Object value) {
        if (key == null || value == null) return;
        metadata.put(key, value);
    }
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (!type.isInstance(value)) return null;
        return type.cast(value);
    }
    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }
    public void removeMetadata(String key) {
        metadata.remove(key);
    }
    public void clearMetadata() {
        metadata.clear();
    }
    public void copyFrom(SectionMetadataManager other) {
        if (other == null) return;
        metadata.clear();
        metadata.putAll(other.metadata);
    }
    public SectionMetadataManager deepClone() {
        SectionMetadataManager clone = new SectionMetadataManager();
        clone.metadata.putAll(this.metadata);
        return clone;
    }
}
