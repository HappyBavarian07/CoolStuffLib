package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.backupmanager.FileBackup;
import de.happybavarian07.coolstufflib.backupmanager.RegexFileFilter;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.ConfigModule;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PersistentBackupModule extends ConfigModule {
    private FileBackup backup;
    private final int maxBackups;

    public PersistentBackupModule(int maxBackups) {
        if (maxBackups <= 0) {
            this.maxBackups = 1;
        } else {
            this.maxBackups = maxBackups;
        }
    }

    @Override
    public String getName() {
        return "PersistentBackupModule";
    }

    @Override
    public void enable() { /* Do nothing */ }

    @Override
    public void disable() { /* Do nothing */ }

    @Override
    public void onAttach(AdvancedConfig config) {
        super.onAttach(config);
        if (config.getFile() == null) {
            throw new IllegalArgumentException("Config file cannot be null for PersistentBackupModule");
        }
        File backupDir = new File(config.getFile().getParentFile(), "backups" + File.separator + config.getName() + "_backups");
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
    public void onDetach() {
        super.onDetach();
        this.backup = null;
    }

    @Override
    public void reload() {
    }

    @Override
    public void save() {
        if (backup != null) {
            createBackup();
        }
    }

    @Override
    public void onConfigChange(String key, Object oldValue, Object newValue) {
    }

    @Override
    public boolean supportsConfig(AdvancedConfig config) {
        return config.getFile() != null;
    }

    @Override
    public Map<String, Object> getModuleState() {
        Map<String, Object> state = new HashMap<>();
        state.put("maxBackups", maxBackups);
        state.put("hasBackup", backup != null);
        if (backup != null) {
            state.put("backupIdentifier", backup.getIdentifier());
            state.put("backupsDone", listBackups());
        }
        state.put("enabled", isEnabled());
        return state;
    }

    @Override
    public Object onGetValue(String key, Object value) {
        return value;
    }

    /**
     * Creates a backup of the current configuration.
     * The backup will be stored in the backup directory with a number.
     *
     * @return Error Code
     */
    public int createBackup() {
        return backup.backup(maxBackups, true);
    }

    /**
     * Restores a backup by its number.
     * Use -1 to restore the latest backup.
     *
     * @param backupNumber The number of the backup to restore.
     * @return true if the restore was successful, false otherwise.
     */
    public boolean restoreBackup(int backupNumber) {
        if (!isEnabled())
            return false;
        if (backup == null) return false;
        int result = backup.loadBackup(backup.getBackupFileFromNumber(backupNumber));
        if (result != 0) return false;
        getConfig().reload();
        return true;
    }

    public Map<String, File> listBackups() {
        if (!isEnabled()) return Collections.emptyMap();
        if (backup == null) return Collections.emptyMap();
        Map<String, File> backups = new HashMap<>();
        for (File file : backup.getBackupsDone()) {
            String name = file.getName().replace(".zip", "");
            backups.put(name, file);
        }
        return backups;
    }

    public boolean deleteBackup(int backupNumber) {
        if (!isEnabled()) return false;
        if (backup == null) return false;
        int result = backup.deleteZipBackup(backupNumber);
        return result == 0;
    }
}
