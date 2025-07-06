package de.happybavarian07.coolstufflib.backupmanager;

import org.junit.jupiter.api.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FileBackupTest {
    private final Path tempDir = Path.of("TestOutputs");

    @Test
    void testBackup() {
        File testsRoot = new File(tempDir.toFile() + File.separator + "FileBackupTest" + File.separator + "testBackup");
        testsRoot.mkdirs();
        System.out.println(testsRoot.getAbsolutePath());
        File file = new File(testsRoot, "test.txt");
        try (FileWriter writer = new FileWriter(file)) { writer.write("data"); } catch (IOException e) { fail(); }
        File backupDir = new File(testsRoot, "backupsBackup");
        backupDir.mkdir();
        FileBackup backup = new FileBackup("testBackup", new File[]{file}, backupDir, testsRoot);
        int result = backup.backup(3, true);
        assertEquals(0, result);
        assertNotNull(backup.getNewestBackupFile());
        assertTrue(backup.getNewestBackupFile().exists());
    }

    @Test
    void testRestore() {
        File testsRoot = new File(tempDir.toFile() + File.separator + "FileBackupTest" + File.separator + "testRestore");
        testsRoot.mkdirs();
        File file = new File(testsRoot, "test.txt");
        try (FileWriter writer = new FileWriter(file)) { writer.write("data"); } catch (IOException e) { fail(); }
        File backupDir = new File(testsRoot, "backupsRestore");
        backupDir.mkdir();
        FileBackup backup = new FileBackup("testRestore", new File[]{file}, backupDir, testsRoot);
        backup.backup(3, true);
        File backupFile = backup.getNewestBackupFile();
        try (FileWriter writer = new FileWriter(file)) { writer.write("changed"); } catch (IOException e) { fail(); }
        int result = backup.loadBackup(backupFile);
        assertEquals(0, result);
        assertTrue(file.exists());
    }

    @Test
    void testDelete() {
        File testsRoot = new File(tempDir.toFile() + File.separator + "FileBackupTest" + File.separator + "testDelete");
        testsRoot.mkdirs();
        File file = new File(testsRoot, "test.txt");
        try (FileWriter writer = new FileWriter(file)) { writer.write("data"); } catch (IOException e) { fail(); }
        File backupDir = new File(testsRoot, "backupsDelete");
        backupDir.mkdir();
        FileBackup backup = new FileBackup("testDelete", new File[]{file}, backupDir, testsRoot);
        backup.backup(3, true);
        File backupFile = backup.getNewestBackupFile();
        int result = backup.deleteZipBackup(-1);
        assertEquals(0, result);
        assertFalse(backupFile.exists());
    }

    @Test
    void testRegexFiltering() {
        File testsRoot = new File(tempDir.toFile() + File.separator + "FileBackupTest" + File.separator + "testRegexFiltering");
        testsRoot.mkdirs();
        File file1 = new File(testsRoot, "test1.txt");
        File file2 = new File(testsRoot, "test2.log");
        try (FileWriter w1 = new FileWriter(file1)) { w1.write("a"); } catch (IOException e) { fail(); }
        try (FileWriter w2 = new FileWriter(file2)) { w2.write("b"); } catch (IOException e) { fail(); }
        File backupDir = new File(testsRoot, "backupsRegex");
        backupDir.mkdir();
        List<RegexFileFilter> filters = Collections.singletonList(new RegexFileFilter(".*\\.txt"));
        FileBackup backup = new FileBackup("testRegex", filters, Collections.emptyList(), backupDir, testsRoot);
        int result = backup.backup(3, true);
        assertEquals(0, result);
        File zip = backup.getNewestBackupFile();
        assertNotNull(zip);
        assertTrue(zip.exists());
    }

    @Test
    void testErrorHandling() {
        File testsRoot = new File(tempDir.toFile() + File.separator + "FileBackupTest" + File.separator + "testErrorHandling");
        testsRoot.mkdirs();
        File backupDir = new File(testsRoot, "backupsError");
        backupDir.mkdir();
        FileBackup backup = new FileBackup("testError", new File[]{}, backupDir, testsRoot);
        int result = backup.backup(3, true);
        assertEquals(-1, result); // No files to backup
        File file = new File(testsRoot, "test.txt");
        try (FileWriter writer = new FileWriter(file)) { writer.write("data"); } catch (IOException e) { fail(); }
        backup = new FileBackup("testError", new File[]{file}, backupDir, testsRoot);
        result = backup.loadBackup(new File(backupDir, "doesnotexist.zip"));
        assertEquals(-3, result); // Zip file doesn't exist
    }
}