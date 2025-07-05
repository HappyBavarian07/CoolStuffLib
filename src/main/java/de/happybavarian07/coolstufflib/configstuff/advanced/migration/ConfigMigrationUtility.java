package de.happybavarian07.coolstufflib.configstuff.advanced.migration;

import de.happybavarian07.coolstufflib.configstuff.Config;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ConfigMigrationUtility {
    public static void migrateFromLegacy(Config oldConfig, AdvancedConfig newConfig, MigrationContext context) {
        if (oldConfig == null || newConfig == null) {
            throw new IllegalArgumentException("Both old and new configs must be non-null");
        }
        newConfig.setMigrationContext(context);
        Map<String, Object> oldValues = new HashMap<>();
        Set<String> keys = oldConfig.getFileConfiguration().getKeys(true);
        for (String key : keys) {
            oldValues.put(key, oldConfig.get(key, Object.class));
        }
        Map<String, Object> nestedMap = buildNestedMap(oldValues);
        newConfig.migrate(context, nestedMap, true);
    }

    public static void migrateFromLegacyAdvanced(
            AdvancedConfig oldConfig,
            AdvancedConfig newConfig,
            MigrationContext context) {
        if (oldConfig == null || newConfig == null) {
            throw new IllegalArgumentException("Both old and new configs must be non-null");
        }
        newConfig.migrate(context, oldConfig.getRootSection(), true);
    }

    private static Map<String, Object> buildNestedMap(Map<String, Object> flatMap) {
        Map<String, Object> root = new HashMap<>();
        for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
            String[] parts = entry.getKey().split("\\.");
            Map<String, Object> current = root;
            for (int i = 0; i < parts.length - 1; i++) {
                current = (Map<String, Object>) current.computeIfAbsent(parts[i], k -> new HashMap<>());
            }
            current.put(parts[parts.length - 1], entry.getValue());
        }
        return root;
    }

    public static void detectAndConvertCollections(AdvancedConfig config) {
        if (config == null || config.getRootSection() == null) {
            throw new IllegalArgumentException("Config and its root section must be non-null");
        }
        config.detectAndConvertCollections();
    }
}
