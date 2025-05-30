package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.ConfigLogger;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.AutoGenModule;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.templates.AutoGenTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class CorruptionCheckModule extends ConfigModule {
    private AutoGenModule autoGenModule;

    @Override
    public String getName() {
        return "CorruptionCheckModule";
    }

    @Override
    public void enable() {
    }

    @Override
    public void disable() {
    }

    @Override
    public void reload() {}

    @Override
    public void save() {}
    
    @Override
    public void onConfigChange(String key, Object oldValue, Object newValue) {}

    @Override
    public Object onGetValue(String key, Object value) {
        return value;
    }

    @Override
    public Map<String, Object> getModuleState() {
        return Map.of("enabled", isEnabled());
    }

    @Override
    public void onAttach(AdvancedConfig config) {
        super.onAttach(config);
        if (config.hasModule("AutoGenModule")) {
            autoGenModule = (AutoGenModule) config.getModuleByName("AutoGenModule");
            if (autoGenModule == null) {
                throw new IllegalStateException("AutoGenModule is not properly initialized");
            }
        } else {
            throw new IllegalStateException(getName() + " requires AutoGenModule to be enabled");
        }
    }

    @Override
    public boolean supportsConfig(AdvancedConfig config) {
        return config.hasModule("AutoGenModule");
    }

    public boolean checkCorruption() {
        if (getConfig() == null || autoGenModule == null || !isEnabled()) {
            ConfigLogger.warning("Cannot check corruption: " + 
                (getConfig() == null ? "Config is null" : 
                autoGenModule == null ? "AutoGenModule is null" : "Module is disabled"), getName(), true);
            return false;
        }

        try {
            Map<String, Object> currentConfig = getConfig().getValueMap();
            Map<String, Object> templateMap = autoGenModule.getMergedTemplateMap();
            return !hasMissingOrNullValues(templateMap, currentConfig);
        } catch (Exception e) {
            ConfigLogger.severe("Error checking config corruption: " + e.getMessage(), getName(), true);
            return false;
        }
    }

    public boolean checkCorruptionAndRepair(File configFile) {
        if (getConfig() == null || autoGenModule == null || !isEnabled()) {
            ConfigLogger.warning("Cannot repair corruption: " + 
                (getConfig() == null ? "Config is null" : 
                autoGenModule == null ? "AutoGenModule is null" : "Module is disabled"), getName(), true);
            return false;
        }

        try {
            Map<String, Object> currentConfig = getConfig().getValueMap();
            Map<String, Object> templateMap = autoGenModule.getMergedTemplateMap();
            
            if (hasMissingOrNullValues(templateMap, currentConfig)) {
                backupConfigFile(configFile);
                repairConfig(templateMap, currentConfig);
                getConfig().save();
                return false;
            }
            return true;
        } catch (Exception e) {
            ConfigLogger.severe("Error during config repair: " + e.getMessage(), getName(), true);
            try {
                restoreFromTemplates(configFile);
            } catch (Exception ex) {
                ConfigLogger.severe("Failed to restore from templates: " + ex.getMessage(), getName(), true);
            }
            return false;
        }
    }

    private boolean hasMissingOrNullValues(Map<String, Object> template, Map<String, Object> actual) {
        for (Map.Entry<String, Object> entry : template.entrySet()) {
            String key = entry.getKey();
            Object templateValue = entry.getValue();
            Object actualValue = actual.get(key);

            if (!actual.containsKey(key) || actualValue == null) {
                return true;
            }

            if (templateValue instanceof Map) {
                if (!(actualValue instanceof Map) || 
                    hasMissingOrNullValues((Map<String, Object>) templateValue, (Map<String, Object>) actualValue)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void repairConfig(Map<String, Object> template, Map<String, Object> actual) {
        checkAgainstTemplateAndRepair(actual, template);
    }

    private void backupConfigFile(File configFile) throws IOException {
        if (configFile.exists()) {
            File backupFile = new File(
                configFile.getParentFile(), 
                configFile.getName().replaceFirst("(\\.[^.]*)$", "_backup_" + System.currentTimeMillis() + "$1")
            );
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            ConfigLogger.info("Created backup at: " + backupFile.getAbsolutePath(), getName(), false);
        }
    }

    private void restoreFromTemplates(File configFile) throws IOException {
        if (configFile.exists()) {
            backupConfigFile(configFile);
        }

        Map<String, Object> configMap = new LinkedHashMap<>(getConfig().getValueMap());
        for (AutoGenTemplate template : autoGenModule.getTemplates()) {
            Map<String, Object> templateMap = template.toMap();
            checkAgainstTemplateAndRepair(configMap, templateMap);
        }

        getConfig().setValueBulk(configMap);
        getConfig().save();
        ConfigLogger.info("Config restored from templates", getName(), false);
    }

    private void checkAgainstTemplateAndRepair(Map<String, Object> configMap, Map<String, Object> templateMap) {
        for (Map.Entry<String, Object> entry : templateMap.entrySet()) {
            String key = entry.getKey();
            Object templateValue = entry.getValue();
            Object actualValue = configMap.get(key);

            if (!configMap.containsKey(key) || actualValue == null) {
                configMap.put(key, deepCopy(templateValue));
                ConfigLogger.info("Repaired missing/null value for key: " + key, getName(), false);
                continue;
            }

            if (templateValue instanceof Map && actualValue instanceof Map) {
                repairConfig((Map<String, Object>) templateValue, (Map<String, Object>) actualValue);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T deepCopy(T object) {
        if (object instanceof Map) {
            Map<Object, Object> map = new LinkedHashMap<>();
            ((Map<?, ?>) object).forEach((k, v) -> map.put(k, deepCopy(v)));
            return (T) map;
        } else if (object instanceof List) {
            List<Object> list = new ArrayList<>();
            ((List<?>) object).forEach(item -> list.add(deepCopy(item)));
            return (T) list;
        } else if (object instanceof Set) {
            Set<Object> set = new LinkedHashSet<>();
            ((Set<?>) object).forEach(item -> set.add(deepCopy(item)));
            return (T) set;
        }
        return object;
    }
}
