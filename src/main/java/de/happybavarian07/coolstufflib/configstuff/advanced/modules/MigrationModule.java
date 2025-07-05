package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigLifecycleEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.migration.ConfigMigrationUtility;
import de.happybavarian07.coolstufflib.configstuff.advanced.migration.MigrationContext;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MigrationModule extends AbstractBaseConfigModule {
    private final Map<Integer, Consumer<AdvancedConfig>> migrations = new LinkedHashMap<>();
    private int currentVersion = 0;
    private boolean autoMigrateOnLoad = false;
    private int targetVersion = Integer.MAX_VALUE;
    private MigrationContext migrationContext;

    public MigrationModule() {
        super("MigrationModule",
                "Manages configuration migrations between versions",
                "1.0.0");
        this.migrationContext = new MigrationContext();
    }

    @Override
    protected void onInitialize() {
        // Load current version if available
        if (config.containsKey("__migrationVersion")) {
            currentVersion = config.getInt("__migrationVersion", 0);
        }
    }

    @Override
    protected void onEnable() {
        // Register for config load events if auto-migration is enabled
        if (autoMigrateOnLoad) {
            registerEventListener(
                    config.getEventBus(),
                    ConfigLifecycleEvent.class,
                    this::onConfigLifecycleEvent
            );
        }
    }

    @Override
    protected void onDisable() {
        // Unregister from config load events
        if (autoMigrateOnLoad) {
            unregisterEventListener(
                    config.getEventBus(),
                    ConfigLifecycleEvent.class,
                    this::onConfigLifecycleEvent
            );
        }
    }

    @Override
    protected void onCleanup() {
        migrations.clear();
    }

    private void onConfigLifecycleEvent(ConfigLifecycleEvent event) {
        if (event.getType() == ConfigLifecycleEvent.Type.LOAD ||
                event.getType() == ConfigLifecycleEvent.Type.RELOAD) {
            migrateTo(targetVersion);
        }
    }

    public void addMigration(int version, Consumer<AdvancedConfig> migration) {
        migrations.put(version, migration);
    }

    public void migrateTo(int targetVersion) {
        if (config == null) return;

        for (Map.Entry<Integer, Consumer<AdvancedConfig>> entry : migrations.entrySet()) {
            int migrationVersion = entry.getKey();
            if (migrationVersion > currentVersion && migrationVersion <= targetVersion) {
                try {
                    entry.getValue().accept(config);
                    currentVersion = migrationVersion;

                    // Store the current version
                    config.set("__migrationVersion", currentVersion);
                } catch (Exception e) {
                    // Log error and stop migration
                    break;
                }
            }
        }
    }

    public void setAutoMigrateOnLoad(boolean autoMigrateOnLoad) {
        this.autoMigrateOnLoad = autoMigrateOnLoad;

        // Update event listeners based on new setting
        if (state == ModuleState.ENABLED) {
            if (autoMigrateOnLoad) {
                registerEventListener(
                        config.getEventBus(),
                        ConfigLifecycleEvent.class,
                        this::onConfigLifecycleEvent
                );
            } else {
                unregisterEventListener(
                        config.getEventBus(),
                        ConfigLifecycleEvent.class,
                        this::onConfigLifecycleEvent
                );
            }
        }
    }

    public void setTargetVersion(int targetVersion) {
        this.targetVersion = targetVersion;
    }

    public int getCurrentVersion() {
        return currentVersion;
    }

    public Map<Integer, Consumer<AdvancedConfig>> getMigrations() {
        return new LinkedHashMap<>(migrations);
    }

    public Map<String, Object> getAdditionalModuleState() {
        Map<String, Object> state = new HashMap<>();
        state.put("currentVersion", currentVersion);
        state.put("migrationCount", migrations.size());
        state.put("autoMigrateOnLoad", autoMigrateOnLoad);
        return state;
    }

    public void setMigrationContext(MigrationContext context) {
        this.migrationContext = context;
    }

    public MigrationContext getMigrationContext() {
        return migrationContext;
    }
}
