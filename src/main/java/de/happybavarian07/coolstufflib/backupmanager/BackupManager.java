package de.happybavarian07.coolstufflib.backupmanager;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import de.happybavarian07.coolstufflib.service.api.Service;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BackupManager implements Service {
    private final UUID serviceId = UUID.randomUUID();

    @Override
    public UUID id() { return serviceId; }
    @Override
    public String serviceName() { return "backup-manager"; }
    @Override
    public CompletableFuture<Void> init() { return CompletableFuture.completedFuture(null); }
    @Override
    public CompletableFuture<Void> shutdown() { return CompletableFuture.completedFuture(null); }

    private Map<String, FileBackup> fileBackupList;
    private int numberOfBackUpsBeforeDeleting;
    private volatile boolean backupSchedulerEnabled = true;
    private final long backupRepeatTimeInSeconds;
    private Thread backupSchedulerThread;

    /**
     * Constructs a BackupManager instance.
     *
     * @param numberOfBackUpsBeforeDeleting The maximum number of backups to retain.
     * @param backupRepeatTimeInSeconds The interval for automatic backups.
     */
    public BackupManager(int numberOfBackUpsBeforeDeleting, long backupRepeatTimeInSeconds) {
        this.fileBackupList = new HashMap<>();
        this.numberOfBackUpsBeforeDeleting = numberOfBackUpsBeforeDeleting;
        this.backupRepeatTimeInSeconds = backupRepeatTimeInSeconds;

        startBackupScheduler();
    }

    private void startBackupScheduler() {
        backupSchedulerEnabled = true;
        if (backupSchedulerThread != null && backupSchedulerThread.isAlive()) {
            backupSchedulerThread.interrupt();
        }
        backupSchedulerThread = new Thread(() -> {
            while (backupSchedulerEnabled) {
                try {
                    for (long waited = 0; waited < this.backupRepeatTimeInSeconds * 1000 && backupSchedulerEnabled; ) {
                        long sleepTime = Math.min(1000, this.backupRepeatTimeInSeconds * 1000 - waited);
                        Thread.sleep(sleepTime);
                        waited += sleepTime;
                    }
                    if (backupSchedulerEnabled) backupAllFileBackups();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        backupSchedulerThread.setDaemon(true);
        backupSchedulerThread.start();
    }

    public void stopBackupScheduler() {
        backupSchedulerEnabled = false;
        if (backupSchedulerThread != null && backupSchedulerThread.isAlive()) {
            backupSchedulerThread.interrupt();
        }
    }

    public void addFileBackup(FileBackup backup) {
        File[] filesInBackupFolder = backup.getDestinationPathToBackupToo().listFiles();
        if (filesInBackupFolder != null) {
            for (File f : filesInBackupFolder) {
                if ((f.getParentFile().getName() + "/" + f.getName()).contains(backup.getIdentifier() + "_"))
                    backup.addBackupDone(f);
            }
        }

        fileBackupList.put(backup.getIdentifier(), backup);
    }

    public void removeFileBackup(FileBackup backup) {
        fileBackupList.remove(backup.getIdentifier());
    }

    /**
     * Executes a manual backup.
     *
     * @param identifier The unique identifier for the backup.
     * @return 0 on success, or -100 if the backup identifier is invalid.
     */
    public int startBackup(String identifier) {
        FileBackup backup = fileBackupList.get(identifier);
        if (numberOfBackUpsBeforeDeleting <= backup.getBackupsDone().size()) {
            backup.removeOldestBackup();
        }
        return backup.backup(numberOfBackUpsBeforeDeleting, false);
    }

    /**
     * Restores a backup.
     *
     * @param identifier The unique identifier for the backup.
     * @param backupNumber The backup index, or -1 for the latest.
     * @return 0 on success, or -100 if the backup is not found.
     */
    public int loadBackup(String identifier, int backupNumber) {
        FileBackup backup = fileBackupList.get(identifier);
        if (backup == null) return -100;
        return backup.loadBackup(backupNumber == -1 ? backup.getNewestBackupFile() : backup.getBackupFileFromNumber(backupNumber));
    }

    public FileBackup getFileBackup(String identifier) {
        return fileBackupList.get(identifier);
    }

    public void backupAllFileBackups() {
        fileBackupList.keySet().forEach(this::startBackup);
    }

    public Map<String, FileBackup> getFileBackupList() {
        return fileBackupList;
    }

    public void setFileBackupList(Map<String, FileBackup> fileBackupList) {
        this.fileBackupList = fileBackupList;
    }

    public int getNumberOfBackUpsBeforeDeleting() {
        return numberOfBackUpsBeforeDeleting;
    }

    public void setNumberOfBackUpsBeforeDeleting(int numberOfBackUpsBeforeDeleting) {
        this.numberOfBackUpsBeforeDeleting = numberOfBackUpsBeforeDeleting;
    }

    /**
     * Deletes a specific backup file.
     *
     * @param identifier The unique identifier for the backup.
     * @param backupFile The file identifier or index.
     * @return 0 on success, or -100 if the backup identifier is invalid.
     */
    public int deleteBackupFile(String identifier, String backupFile) {
        FileBackup backup = fileBackupList.get(identifier);
        if (backup == null) return -100;
        try {
            int backupFileInt = Integer.parseInt(backupFile) - 1;
            return backup.deleteZipBackup(backupFileInt);
        } catch (NumberFormatException e) {
            return backup.deleteZipBackup(backup.getBackupFromFileName(backupFile));
        }
    }
}