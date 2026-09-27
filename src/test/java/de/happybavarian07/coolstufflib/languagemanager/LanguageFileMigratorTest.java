package de.happybavarian07.coolstufflib.languagemanager;

import de.happybavarian07.coolstufflib.languagemanager.storage.LanguageEntry;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LanguageFileMigratorTest {
    @Test
    void comparesValuesAndWritesTheSelectedOnes() {
        Map<String, Object> written = new HashMap<>();
        LanguageFileMigrator migrator = new LanguageFileMigrator(
                Map.of("A", "mine", "B", "same", "Old", "gone"),
                Map.of("A", "jar", "B", "same", "New", "fresh"),
                written::putAll);

        List<LanguageFileMigrator.MigrationEntry> entries = migrator.getMigrationEntries();
        assertEquals(LanguageFileMigrator.MigrationStatus.DIFFERENT_VALUE, status(entries, "A"));
        assertEquals(LanguageFileMigrator.MigrationStatus.UNCHANGED, status(entries, "B"));
        assertEquals(LanguageFileMigrator.MigrationStatus.MISSING_IN_USER, status(entries, "New"));
        assertEquals(LanguageFileMigrator.MigrationStatus.MISSING_IN_RESOURCE, status(entries, "Old"));
        assertTrue(migrator.filesDifferByHash());

        entries.stream().filter(e -> e.getKey().equals("New")).findFirst().orElseThrow().setSelectedForMigration(true);
        migrator.migrateSelected();
        assertEquals(Map.of("New", "fresh"), written);
    }

    private static LanguageFileMigrator.MigrationStatus status(List<LanguageFileMigrator.MigrationEntry> entries, String key) {
        return entries.stream().filter(e -> e.getKey().equals(key)).findFirst().orElseThrow().getStatus();
    }

    @Test
    void selectedMissingKeysAreWrittenToConsumer() {
        Map<String, Object> written = new HashMap<>();
        LanguageFileMigrator migrator = new LanguageFileMigrator(
                Map.of("A", "userValue"),
                Map.of("A", "jarValue", "Missing", "jarOnly"),
                written::putAll);

        List<LanguageFileMigrator.MigrationEntry> entries = migrator.getMigrationEntries();
        assertEquals(LanguageFileMigrator.MigrationStatus.MISSING_IN_USER, status(entries, "Missing"));
        entries.stream().filter(e -> e.getKey().equals("Missing")).findFirst().orElseThrow().setSelectedForMigration(true);
        migrator.migrateSelected();
        assertEquals(Map.of("Missing", "jarOnly"), written);
    }
}
