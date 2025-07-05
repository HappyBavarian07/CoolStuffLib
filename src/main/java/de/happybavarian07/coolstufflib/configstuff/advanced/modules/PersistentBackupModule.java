package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.backupmanager.FileBackup;
import de.happybavarian07.coolstufflib.backupmanager.RegexFileFilter;
import de.happybavarian07.coolstufflib.logging.ConfigLogger;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigLifecycleEvent;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PersistentBackupModule extends AbstractBaseConfigModule {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private FileBackup backup;
    private final int maxBackups;
    private File backupDir;
    private final boolean runBackupAsync;

    public PersistentBackupModule(int maxBackups) {
        this(maxBackups, true);
    }

    public PersistentBackupModule(int maxBackups, boolean runBackupAsync) {
        super("PersistentBackupModule",
                "Creates persistent backups of config files on disk",
                "1.0.0");
        this.maxBackups = maxBackups > 0 ? maxBackups : 1;
        this.runBackupAsync = runBackupAsync;
    }

    @Override
    protected void onInitialize() {
        if (config.getFile() == null) {
            throw new IllegalArgumentException("Config file cannot be null for PersistentBackupModule");
        }

        backupDir = new File(config.getFile().getParentFile(), "backups" + File.separator + config.getName() + "_backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        if (!backupDir.isDirectory()) {
            throw new IllegalArgumentException("Backup directory must be a directory: " + backupDir.getAbsolutePath());
        }

        this.backup = new FileBackup(config.getName() + "_backup",
                Collections.singletonList(new RegexFileFilter(config.getFile().getName())),
                Collections.emptyList(),
                backupDir, config.getFile().getParentFile());
    }

    @Override
    protected void onEnable() {
        registerEventListener(config.getEventBus(),
                ConfigLifecycleEvent.class,
                this::onConfigLifecycle);

        ConfigLogger.info("PersistentBackupModule enabled for config: " + config.getName(),
                "PersistentBackupModule", true);
    }

    @Override
    protected void onDisable() {
        unregisterEventListener(config.getEventBus(),
                ConfigLifecycleEvent.class,
                this::onConfigLifecycle);

        ConfigLogger.info("PersistentBackupModule disabled for config: " + config.getName(),
                "PersistentBackupModule", true);
    }

    @Override
    protected void onCleanup() {
        this.backup = null;
        this.backupDir = null;
    }

    private void onConfigLifecycle(ConfigLifecycleEvent event) {
        if (event.getType() == ConfigLifecycleEvent.Type.SAVE) {
            createBackup();
        }
    }

    public void createBackup() {
        if (backup == null) {
            return;
        }
        Runnable backupTask = () -> {
            try {
                int result = backup.backup(maxBackups, true);
                if (result == 0) {
                    ConfigLogger.info("Created backup for config " + config.getName(),
                            "PersistentBackupModule", false);
                } else {
                    String errorMessage = getErrorMessage(result);
                    ConfigLogger.error("Failed to create backup for config " + config.getName() + ": " + errorMessage,
                            "PersistentBackupModule", true);
                }
            } catch (Exception e) {
                ConfigLogger.error("Error creating backup for config " + config.getName() + ": " + e.getMessage(),
                        "PersistentBackupModule", true);
            }
        };
        if (runBackupAsync) {
            new Thread(backupTask).start();
        } else {
            backupTask.run();
        }
    }

    private String getErrorMessage(int errorCode) {
        return switch (errorCode) {
            case -1 -> "No files to backup";
            case -2 -> "I/O exception occurred";
            case -3 -> "No valid destination directory";
            case -4 -> "Backup limit reached";
            default -> "Unknown error (code: " + errorCode + ")";
        };
    }

    private void cleanupOldBackups() {
        while (backup.getBackupsDone().size() > maxBackups) {
            backup.removeOldestBackup();
        }
    }

    public static class BackupResult {
        private final boolean success;
        private final String errorMessage;
        private final File backupFile;

        public BackupResult(boolean success, String errorMessage, File backupFile) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.backupFile = backupFile;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public File getBackupFile() {
            return backupFile;
        }
    }

    @Override
    protected Map<String, Object> getAdditionalModuleState() {
        return Map.of();
    }
}
