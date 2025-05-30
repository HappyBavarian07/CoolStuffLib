package de.happybavarian07.coolstufflib.backupmanager;

import de.happybavarian07.coolstufflib.CoolStuffLib;

/*
 * @Author HappyBavarian07
 * @Date September 12, 2024 | 15:41
 */
public class BackupExecuteRunnable implements Runnable {
    private final BackupManager backupManager;
    private final String identifier;

    public BackupExecuteRunnable(BackupManager backupManager, String identifier) {
        this.backupManager = backupManager;
        this.identifier = identifier;
    }

    @Override
    public void run() {
        backupManager.startBackup(identifier);
        CoolStuffLib.getLib().getJavaPluginUsingLib().getLogger().info("Backup of " + identifier + " started.");
    }
}
