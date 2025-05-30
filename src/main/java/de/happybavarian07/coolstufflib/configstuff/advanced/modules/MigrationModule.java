package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.ConfigModule;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MigrationModule extends ConfigModule {
    private final Map<Integer, Consumer<AdvancedConfig>> migrations = new LinkedHashMap<>();
    private int currentVersion = 0;

    public void addMigration(int version, Consumer<AdvancedConfig> migration) {
        migrations.put(version, migration);
    }

    public void migrateTo(int targetVersion) {
        if (getConfig() == null) return;
        for (Map.Entry<Integer, Consumer<AdvancedConfig>> entry : migrations.entrySet()) {
            if (entry.getKey() > currentVersion && entry.getKey() <= targetVersion) {
                entry.getValue().accept(getConfig());
                currentVersion = entry.getKey();
            }
        }
    }

    @Override
    public String getName() {
        return "MigrationModule";
    }

    @Override
    public void enable() { /* Do nothing */ }

    @Override
    public void disable() { /* Do nothing */ }

    @Override
    public void reload() {
    }

    @Override
    public void save() {
    }

    @Override
    public void onConfigChange(String key, Object oldValue, Object newValue) {
    }

    @Override
    public boolean supportsConfig(AdvancedConfig config) {
        return true;
    }

    @Override
    public Map<String, Object> getModuleState() {
        return Map.of("currentVersion", currentVersion);
    }

    @Override
    public Object onGetValue(String key, Object value) {
        return value;
    }
}
