package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LanguageMigrationTest {
    @TempDir
    Path folder;

    @Test
    void movesALegacyFileIntoTheSplitLayoutWithABackup() throws IOException {
        Files.copy(Path.of("src/test/resources/golden/adminpanel/en.yml"), folder.resolve("en.yml"));
        ReadResult before = new LegacyYamlBackend(folder.toFile()).read("en");

        MigrationReport report = LanguageMigration.migrateLegacyFile(folder.toFile(), "en");

        assertNotNull(report);
        assertTrue(report.ok(), report.summary());
        assertFalse(Files.exists(folder.resolve("en.yml")));
        assertTrue(Files.exists(report.backup().toPath()));
        ReadResult after = new SplitYamlBackend(folder.toFile()).read("en");
        assertEquals(before.entries().keySet(), after.entries().keySet());
        for (String key : before.entries().keySet()) {
            assertEquals(before.entries().get(key).value(), after.entries().get(key).value(), key);
        }
        assertNull(LanguageMigration.migrateLegacyFile(folder.toFile(), "en"));
    }

    @Test
    void aBrokenLegacyFileStaysWhereItIs() throws IOException {
        Files.writeString(folder.resolve("en.yml"), "Messages:\n  A: 'unclosed\n");

        MigrationReport report = LanguageMigration.migrateLegacyFile(folder.toFile(), "en");

        assertNotNull(report);
        assertFalse(report.ok());
        assertTrue(Files.exists(folder.resolve("en.yml")));
        assertFalse(Files.exists(folder.resolve("en")));
    }
}
