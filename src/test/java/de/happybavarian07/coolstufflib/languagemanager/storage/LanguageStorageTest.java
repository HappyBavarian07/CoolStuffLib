package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LanguageStorageTest {
    @TempDir
    Path folder;

    @Test
    void freshInstallWritesTheJarDefaultsWithComments() throws IOException {
        LanguageStorage storage = new LanguageStorage(folder.toFile(), "lang-fixtures/legacy");

        PrepareResult prepare = storage.prepare("en", "en");

        assertTrue(prepare.problems().isEmpty(), prepare.problems().toString());
        assertTrue(prepare.addedKeys().contains("Messages.Player.Greeting"));
        String player = Files.readString(folder.resolve("en/messages/Player.yml"));
        assertTrue(player.contains("# greeting"), player);
        assertTrue(Files.readString(folder.resolve("en/items/Heal.yml")).contains("material: 'GOLDEN_APPLE'"));
    }

    @Test
    void ownerValuesWinAndMissingKeysComeFromTheJar() throws IOException {
        Path file = folder.resolve("en/messages/Player.yml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "Greeting: 'Servus'\n");
        LanguageStorage storage = new LanguageStorage(folder.toFile(), "lang-fixtures/legacy");

        LoadResult loaded = storage.load("en", "en", null);

        assertEquals("Servus", loaded.merged().get("Messages.Player.Greeting").value());
        assertEquals("root message", loaded.merged().get("Messages.Plain").value());
        assertEquals("en/messages/Player.yml:1", loaded.merged().get("Messages.Player.Greeting").origin().toString());
    }

    @Test
    void aBrokenOwnerFileKeepsItsLastWorkingValues() throws IOException {
        Path file = folder.resolve("en/messages/Player.yml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "Greeting: 'Servus'\n");
        LanguageStorage storage = new LanguageStorage(folder.toFile(), "lang-fixtures/legacy");
        LoadResult first = storage.load("en", "en", null);

        Files.writeString(file, "Greeting: 'broken\n");
        LoadResult second = storage.load("en", "en", first);

        assertEquals("Servus", second.merged().get("Messages.Player.Greeting").value());
        assertEquals(1, second.problems().size());
    }

    @Test
    void anOwnerSectionReplacesAJarValue() throws IOException {
        Path file = folder.resolve("en/messages/_root.yml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "Plain:\n  text: 'rich'\n");
        LoadResult loaded = new LanguageStorage(folder.toFile(), "lang-fixtures/legacy").load("en", "en", null);

        assertNull(loaded.merged().get("Messages.Plain"));
        assertEquals("rich", loaded.merged().get("Messages.Plain.text").value());
    }

    @Test
    void aStrayLegacyFileNextToTheFolderIsReportedAndKept() throws IOException {
        Files.createDirectories(folder.resolve("en/messages"));
        Files.writeString(folder.resolve("en/messages/Player.yml"), "Greeting: 'Servus'\n");
        Files.writeString(folder.resolve("en.yml"), "LanguageFullName: 'English'\n");
        LanguageStorage storage = new LanguageStorage(folder.toFile(), "lang-fixtures/legacy");

        PrepareResult prepare = storage.prepare("en", "en");

        assertEquals(SplitYamlBackend.ID, storage.diskFor("en").id());
        assertEquals(1, prepare.problems().size(), prepare.problems().toString());
        assertEquals("en.yml", prepare.problems().get(0).file());
        assertTrue(prepare.problems().get(0).message().contains("en/"), prepare.problems().get(0).message());
        assertTrue(Files.exists(folder.resolve("en.yml")));
        assertTrue(Files.exists(folder.resolve("en/messages/Player.yml")));
    }

    @Test
    void theBackendChoiceIsRemembered() {
        LanguageStorage.useBackend(folder.toFile(), LegacyYamlBackend.ID);
        assertEquals(LegacyYamlBackend.ID, LanguageStorage.backendIdFor(folder.toFile()));
        assertEquals(LegacyYamlBackend.ID, new LanguageStorage(folder.toFile(), "x").diskFor("en").id());
        assertTrue(List.copyOf(LanguageStorage.backendIds()).containsAll(List.of(SplitYamlBackend.ID, LegacyYamlBackend.ID)));
    }
}
