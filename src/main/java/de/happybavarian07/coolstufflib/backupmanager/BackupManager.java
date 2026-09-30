package de.happybavarian07.coolstufflib.backupmanager;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

import de.happybavarian07.coolstufflib.service.api.Service;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class BackupManager implements Service {
    private final UUID serviceId = UUID.randomUUID();

    @Override
    public UUID id() { return serviceId; }
    @Override
    public String serviceName() { return "backup-manager"; }
    @Override
    public CompletableFuture<Void> init() {
        startBackupScheduler();
        return CompletableFuture.completedFuture(null);
    }
    @Override
    public CompletableFuture<Void> shutdown() {
        stopBackupScheduler();
        return CompletableFuture.completedFuture(null);
    }

    // always a ConcurrentHashMap: the scheduler thread iterates this while setFileBackupList may swap it
    private volatile Map<String, FileBackup> fileBackupList;
    private int numberOfBackUpsBeforeDeleting;
    private volatile boolean backupSchedulerEnabled = true;
    private final long backupRepeatTimeInSeconds;
    private Thread backupSchedulerThread;

    /**
     * Constructs a BackupManager instance. The automatic backup scheduler starts in {@link #init()}
     * (called by the service registry) or with {@link #startBackupScheduler()}.
     *
     * @param numberOfBackUpsBeforeDeleting The maximum number of backups to retain.
     * @param backupRepeatTimeInSeconds The interval for automatic backups.
     */
    public BackupManager(int numberOfBackUpsBeforeDeleting, long backupRepeatTimeInSeconds) {
        this.fileBackupList = new ConcurrentHashMap<>();
        this.numberOfBackUpsBeforeDeleting = numberOfBackUpsBeforeDeleting;
        this.backupRepeatTimeInSeconds = backupRepeatTimeInSeconds;
    }

    public synchronized void startBackupScheduler() {
        if (backupRepeatTimeInSeconds <= 0) return;
        if (backupSchedulerThread != null && backupSchedulerThread.isAlive()) {
            backupSchedulerThread.interrupt();
        }
        backupSchedulerEnabled = true;
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
        }, "CoolStuffLib-BackupScheduler");
        backupSchedulerThread.setDaemon(true);
        backupSchedulerThread.start();
    }

    public synchronized void stopBackupScheduler() {
        backupSchedulerEnabled = false;
        if (backupSchedulerThread != null && backupSchedulerThread.isAlive()) {
            backupSchedulerThread.interrupt();
        }
    }

    public void addFileBackup(FileBackup backup) {
        File[] filesInBackupFolder = backup.getDestinationPathToBackupToo().listFiles();
        if (filesInBackupFolder != null) {
            for (File f : filesInBackupFolder) {
                // the backup folder can be deleted while we walk the listing, so nothing in it can be trusted
                File parent = f.getParentFile();
                if (parent == null || !f.exists()) continue;
                if ((parent.getName() + "/" + f.getName()).contains(backup.getIdentifier() + "_"))
                    backup.addBackupDone(f);
            }
        }

        fileBackupList.put(backup.getIdentifier(), backup);
    }

    public void removeFileBackup(FileBackup backup) {
        fileBackupList.remove(backup.getIdentifier());
    }

    /**
     * <p>Executes a manual backup.</p>
     *
     * <pre><code>backupManager.startBackup("players_data");</code></pre>
     *
     * @param identifier The backup identifier
     * @return 0 on success, -100 if invalid identifier
     */
    public int startBackup(String identifier) {
        FileBackup backup = fileBackupList.get(identifier);
        if (backup == null) return -100;
        if (numberOfBackUpsBeforeDeleting <= backup.getBackupsDone().size()) {
            backup.removeOldestBackup();
        }
        return backup.backup(numberOfBackUpsBeforeDeleting, false);
    }

    /**
     * <p>Restores a backup.</p>
     *
     * <pre><code>backupManager.loadBackup("players_data", -1);</code></pre>
     *
     * @param identifier   The backup identifier
     * @param backupNumber The index, -1 for latest
     * @return 0 on success, -100 if not found
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
        // copied into a concurrent map, so the scheduler thread can keep iterating a weakly consistent view
        this.fileBackupList = fileBackupList == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(fileBackupList);
    }

    public int getNumberOfBackUpsBeforeDeleting() {
        return numberOfBackUpsBeforeDeleting;
    }

    public void setNumberOfBackUpsBeforeDeleting(int numberOfBackUpsBeforeDeleting) {
        this.numberOfBackUpsBeforeDeleting = numberOfBackUpsBeforeDeleting;
    }

    /**
     * <p>Deletes a specific backup file.</p>
     *
     * @param identifier The backup identifier
     * @param backupFile The file identifier or index
     * @return 0 on success, -100 if invalid
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