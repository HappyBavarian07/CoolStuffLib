package de.happybavarian07.coolstufflib.backupmanager;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class BackupManagerTest {
    private final Path tempDir = Path.of("TestOutputs");
    @Test
    void testRegisterFileBackup() {
        File testsRoot = new File(tempDir.toFile() + File.separator + "BackupManagerTest" + File.separator + "testRegisterFileBackup");
        testsRoot.mkdirs();
        System.out.println(testsRoot.getAbsolutePath());
        File backupDir = new File(testsRoot, "backups");
        backupDir.mkdir();
        File file = new File(testsRoot, "test.txt");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("data");
        } catch (IOException e) {
            fail();
        }
        BackupManager manager = new BackupManager(3, 10000);
        FileBackup backup = new FileBackup("testRegister", new File[]{file}, backupDir, testsRoot);
        manager.addFileBackup(backup);
        assertNotNull(manager);
        assertEquals(manager.getFileBackup("testRegister").getFilesToBackup()[0], file);
    }

    @Test
    void testManageBackups() {
        File testsRoot = new File(tempDir.toFile() + File.separator + "BackupManagerTest" + File.separator + "testManageBackups");
        testsRoot.mkdirs();
        File backupDir = new File(testsRoot, "backups");
        backupDir.mkdir();
        File file = new File(testsRoot, "test.txt");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("data");
        } catch (IOException e) {
            fail();
        }
        BackupManager manager = new BackupManager(3, 10000);
        FileBackup backup = new FileBackup("testManage", new File[]{file}, backupDir, testsRoot);
        manager.addFileBackup(backup);
        assertDoesNotThrow(() -> manager.removeFileBackup(backup));
    }

    @Test
    void testIntegrationWithFileBackup() {
        File testsRoot = new File(tempDir.toFile() + File.separator + "BackupManagerTest" + File.separator + "testIntegrationWithFileBackup");
        testsRoot.mkdirs();
        File backupDir = new File(testsRoot, "backups");
        backupDir.mkdir();
        File file = new File(testsRoot, "test.txt");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("data");
        } catch (IOException e) {
            fail();
        }
        BackupManager manager = new BackupManager(3, 10000);
        FileBackup backup = new FileBackup("testIntegration", new File[]{file}, backupDir, testsRoot);
        manager.addFileBackup(backup);
        int result = backup.backup(3, true);
        assertEquals(0, result);
        File backupFile = backup.getNewestBackupFile();
        assertNotNull(backupFile);
        assertTrue(backupFile.exists());
        int restoreResult = backup.loadBackup(backupFile);
        assertEquals(0, restoreResult);
    }
}
