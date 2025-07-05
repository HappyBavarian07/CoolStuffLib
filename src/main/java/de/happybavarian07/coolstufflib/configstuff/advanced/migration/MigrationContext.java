package de.happybavarian07.coolstufflib.configstuff.advanced.migration;

import java.util.HashMap;
import java.util.Map;

public class MigrationContext {
    private final Map<String, Object> metadata = new HashMap<>();
    private boolean enableLegacyCompatibility = true;

    public boolean isLegacyCompatibilityEnabled() {
        return enableLegacyCompatibility;
    }

    public void setLegacyCompatibilityEnabled(boolean enable) {
        this.enableLegacyCompatibility = enable;
    }

    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }
}
