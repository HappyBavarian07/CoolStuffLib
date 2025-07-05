package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.logging.ConfigLogger;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigLifecycleEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.AutoGenModule;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.templates.AutoGenTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CorruptionCheckModule extends AbstractBaseConfigModule {
    private AutoGenModule autoGenModule;
    private boolean autoRepair = true;
    private int backupCount = 0;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public CorruptionCheckModule() {
        super("CorruptionCheckModule",
              "Detects and repairs corrupted configuration files",
              "1.0.0");
    }

    @Override
    protected void onInitialize() {
        // Look for autogen module to use for repairs
        if (config != null) {
            autoGenModule = (AutoGenModule) config.getModuleByName("AutoGenModule");
        }
    }

    @Override
    protected void onEnable() {
        // Register for config reload events to check for corruption
        registerEventListener(
            config.getEventBus(),
            ConfigLifecycleEvent.class,
            this::onConfigLifecycleEvent
        );
    }

    @Override
    protected void onDisable() {
        // Unregister from config reload events
        unregisterEventListener(
            config.getEventBus(),
            ConfigLifecycleEvent.class,
            this::onConfigLifecycleEvent
        );
    }

    @Override
    protected void onCleanup() {
        autoGenModule = null;
    }

    private void onConfigLifecycleEvent(ConfigLifecycleEvent event) {
        if (event.getType() == ConfigLifecycleEvent.Type.RELOAD) {
            checkCorruption();
        }
    }

    public boolean checkCorruption() {
        if (config == null || config.getFile() == null) {
            return false;
        }

        File configFile = config.getFile();

        try {
            // Check if file exists and is readable
            if (!configFile.exists() || !configFile.canRead()) {
                repairCorruption("File does not exist or cannot be read");
                return true;
            }

            // Check if file size is 0
            if (configFile.length() == 0) {
                repairCorruption("File is empty");
                return true;
            }

            // Attempt to validate structure
            try {
                Map<String, Object> configData = config.getRootSection().toSerializableMap();
                if (configData == null || configData.isEmpty()) {
                    repairCorruption("Configuration appears to be empty");
                    return true;
                }
            } catch (Exception e) {
                repairCorruption("Exception while reading configuration: " + e.getMessage());
                return true;
            }

            return false;

        } catch (Exception e) {
            ConfigLogger.error("Error during corruption check: " + e.getMessage(), getName(), true);
            return false;
        }
    }

    private void repairCorruption(String reason) {
        if (!autoRepair) {
            ConfigLogger.warn("Corruption detected but auto-repair is disabled: " + reason, getName(), true);
            return;
        }

        try {
            File configFile = config.getFile();

            File backupDir = new File(configFile.getParentFile(), "corrupted_backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
            File backupFile = new File(backupDir, configFile.getName() + "." + timestamp + ".corrupted");

            if (configFile.exists() && configFile.length() > 0) {
                Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                backupCount++;
            }

            if (autoGenModule != null && autoGenModule.getState() == ModuleState.ENABLED) {
                AutoGenTemplate template = autoGenModule.getTemplateForFile(configFile.getName());
                if (template != null) {
                    template.writeToFile(configFile);
                    ConfigLogger.info("Repaired corrupted config using template", getName(), true);
                    config.reload();
                    return;
                }
            }

            // Create empty file as fallback
            Files.writeString(configFile.toPath(), "# Auto-generated after corruption was detected\n");
            ConfigLogger.info("Created new empty config file after corruption: " + reason, getName(), true);
            config.reload();

        } catch (IOException e) {
            ConfigLogger.error("Failed to repair corruption: " + e.getMessage(), getName(), true);
        }
    }

    public void setAutoRepair(boolean autoRepair) {
        this.autoRepair = autoRepair;
    }

    public boolean isAutoRepair() {
        return autoRepair;
    }

    public int getBackupCount() {
        return backupCount;
    }

    public Map<String, Object> getAdditionalModuleState() {
        Map<String, Object> state = new HashMap<>();
        state.put("autoRepair", autoRepair);
        state.put("backupCount", backupCount);
        state.put("autoGenModuleAvailable", autoGenModule != null && autoGenModule.getState() == ModuleState.ENABLED);
        return state;
    }
}
