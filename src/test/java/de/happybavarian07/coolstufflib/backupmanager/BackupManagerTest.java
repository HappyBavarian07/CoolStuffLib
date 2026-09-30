package de.happybavarian07.coolstufflib.backupmanager;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    @Test
    void startBackupWithUnknownIdReturnsMinus100() {
        assertEquals(-100, new BackupManager(3, 10000).startBackup("does-not-exist"));
    }

    @Test
    void schedulerRunsOnlyBetweenInitAndShutdown() throws InterruptedException {
        long before = schedulerThreads();
        BackupManager manager = new BackupManager(3, 10000);
        assertEquals(before, schedulerThreads());

        manager.init().join();
        assertEquals(before + 1, schedulerThreads());

        manager.shutdown().join();
        for (int i = 0; i < 50 && schedulerThreads() > before; i++) Thread.sleep(20);
        assertEquals(before, schedulerThreads());
    }

    @Test
    void swappingInAPlainMapStillLeavesAConcurrentOneBehind() throws IOException {
        File testsRoot = testRoot("testSwappingInAPlainMapStillLeavesAConcurrentOneBehind");
        File backupDir = new File(testsRoot, "backups");
        backupDir.mkdir();
        BackupManager manager = new BackupManager(3, 10000);
        Map<String, FileBackup> plain = new HashMap<>();
        plain.put("swapped", new FileBackup("swapped", new File[0], backupDir, testsRoot));

        manager.setFileBackupList(plain);

        assertInstanceOf(ConcurrentHashMap.class, manager.getFileBackupList());
        assertNotNull(manager.getFileBackup("swapped"));
    }

    @Test
    void schedulerIterationSurvivesConcurrentSwapsAndChanges() throws InterruptedException {
        File testsRoot = testRoot("schedulerIterationSurvivesConcurrentSwapsAndChanges");
        File backupDir = new File(testsRoot, "backups");
        backupDir.mkdir();
        BackupManager manager = new BackupManager(3, 10000);
        FileBackup backup = new FileBackup("raced", new File[0], backupDir, testsRoot);
        Map<String, FileBackup> entries = new HashMap<>();
        for (int i = 0; i < 8; i++) entries.put("raced" + i, backup);
        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();
        boolean[] running = {true};

        Thread scheduler = new Thread(() -> {
            while (running[0]) {
                try {
                    manager.backupAllFileBackups();
                } catch (Throwable t) {
                    failures.add(t);
                }
            }
        });
        Thread swaps = new Thread(() -> {
            while (running[0]) {
                try {
                    manager.setFileBackupList(new HashMap<>(entries));
                    manager.addFileBackup(backup);
                    manager.removeFileBackup(backup);
                } catch (Throwable t) {
                    failures.add(t);
                }
            }
        });

        scheduler.start();
        swaps.start();
        Thread.sleep(300);
        running[0] = false;
        scheduler.join();
        swaps.join();

        assertEquals(List.of(), failures.stream().map(Throwable::toString).toList());
    }

    @Test
    void addFileBackupToleratesFilesThatVanishedWhileListing() throws IOException {
        File testsRoot = testRoot("addFileBackupToleratesFilesThatVanishedWhileListing");
        File backupDir = new File(testsRoot, "backups");
        backupDir.mkdir();
        File file = new File(testsRoot, "test.txt");
        Files.writeString(file.toPath(), "data");
        File vanished = new File(backupDir, "vanishing_2.zip");
        // a listing that races the folder: one entry lost its parent, one is already gone
        File listing = new File(backupDir.getPath()) {
            @Override
            public File[] listFiles() {
                return new File[]{new File("vanishing_1.zip"), vanished};
            }
        };
        BackupManager manager = new BackupManager(3, 10000);
        FileBackup backup = new RacingFileBackup("vanishing", new File[]{file}, backupDir, testsRoot, listing);

        assertDoesNotThrow(() -> manager.addFileBackup(backup));
        assertNotNull(manager.getFileBackup("vanishing"));
        assertEquals(0, backup.getBackupsDone().size());
    }

    private File testRoot(String name) {
        File testsRoot = new File(tempDir.toFile() + File.separator + "BackupManagerTest" + File.separator + name);
        testsRoot.mkdirs();
        return testsRoot;
    }

    /** Reports a backup folder listing that no longer matches the file system. */
    private static final class RacingFileBackup extends FileBackup {
        private final File listing;

        private RacingFileBackup(String identifier, File[] filesToBackup, File destinationPathToBackupToo, File rootDirectory, File listing) {
            super(identifier, filesToBackup, destinationPathToBackupToo, rootDirectory);
            this.listing = listing;
        }

        @Override
        public File getDestinationPathToBackupToo() {
            return listing;
        }
    }

    private static long schedulerThreads() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.isAlive() && "CoolStuffLib-BackupScheduler".equals(t.getName()))
                .count();
    }
}
