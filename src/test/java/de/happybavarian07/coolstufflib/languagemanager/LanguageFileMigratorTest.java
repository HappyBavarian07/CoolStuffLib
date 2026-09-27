package de.happybavarian07.coolstufflib.languagemanager;

import de.happybavarian07.coolstufflib.languagemanager.storage.LanguageEntry;
import de.happybavarian07.coolstufflib.languagemanager.storage.LanguageStorage;
import de.happybavarian07.coolstufflib.languagemanager.storage.LegacyYamlBackend;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

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
    void migratedMissingKeysKeepTheJarComment(@TempDir Path folder) throws IOException {
        LanguageStorage.useBackend(folder.toFile(), LegacyYamlBackend.ID);
        Path ownerFile = folder.resolve("en.yml");
        Files.writeString(ownerFile, "Messages:\n  Plain: 'mine'\n");
        LanguageManager lgm = new LanguageManager(mock(JavaPlugin.class), folder.toFile(), "lang-fixtures/legacy", "[P]");
        lgm.addLanguagesToList(false);
        Files.writeString(ownerFile, "Messages:\n  Plain: 'mine'\n");
        lgm.getLang("en", true).getLangConfig().reloadConfig();

        LanguageFileMigrator migrator = lgm.createMigratorForLanguage("en");
        assertEquals(LanguageFileMigrator.MigrationStatus.MISSING_IN_USER, status(migrator.getMigrationEntries(), "Messages.Player.Greeting"));
        migrator.migrateSelected();

        LanguageEntry greeting = new LegacyYamlBackend(folder.toFile()).read("en").entries().get("Messages.Player.Greeting");
        assertEquals("%prefix% Hello", greeting.value());
        assertEquals("greeting", greeting.comment());
    }
}
