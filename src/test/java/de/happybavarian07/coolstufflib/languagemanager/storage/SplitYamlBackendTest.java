package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SplitYamlBackendTest {
    @TempDir
    Path folder;

    @Test
    void writesEntriesIntoTheirFilesAndReadsThemBack() {
        SplitYamlBackend backend = new SplitYamlBackend(folder.toFile());
        List<LanguageProblem> problems = backend.write("en", List.of(
                new LanguageEntry("LanguageFullName", "English", "header", null),
                new LanguageEntry("Messages.Player.Greeting", "Hi 'you'", "greets", null),
                new LanguageEntry("Messages.Plain", "plain", null, null),
                new LanguageEntry("Items.Heal.lore", List.of("a", "b"), null, null)),
                Map.of("Items.Heal", "the heal item"));

        assertTrue(problems.isEmpty(), problems.toString());
        assertTrue(Files.exists(folder.resolve("en/language.yml")));
        assertTrue(Files.exists(folder.resolve("en/messages/Player.yml")));
        ReadResult read = backend.read("en");
        assertEquals("Hi 'you'", read.entries().get("Messages.Player.Greeting").value());
        assertEquals("greets", read.entries().get("Messages.Player.Greeting").comment());
        assertEquals("en/messages/Player.yml:2", read.entries().get("Messages.Player.Greeting").origin().toString());
        assertEquals(List.of("a", "b"), read.entries().get("Items.Heal.lore").value());
        assertEquals("the heal item", read.sectionComments().get("Items.Heal"));
        assertEquals(java.util.Set.of("en"), backend.languages());
    }

    @Test
    void keepsTheOwnersLinesWhenAddingKeys() throws IOException {
        Path file = folder.resolve("en/messages/Player.yml");
        Files.createDirectories(file.getParent());
        String owner = "# my notes\r\nGreeting: \"Servus\"   # changed by me\r\n";
        Files.writeString(file, owner);

        new SplitYamlBackend(folder.toFile()).write("en", List.of(new LanguageEntry("Messages.Player.Bye", "Bye", null, null)));

        assertEquals(owner + "Bye: 'Bye'\r\n", Files.readString(file));
    }

    @Test
    void aBrokenFileOnlyLosesItsOwnKeys() throws IOException {
        Files.createDirectories(folder.resolve("en/messages"));
        Files.writeString(folder.resolve("en/messages/Good.yml"), "A: 'a'\n");
        Files.writeString(folder.resolve("en/messages/Bad.yml"), "B: 'unclosed\n");

        ReadResult read = new SplitYamlBackend(folder.toFile()).read("en");

        assertEquals("a", read.entries().get("Messages.Good.A").value());
        assertEquals(1, read.problems().size());
        assertEquals("en/messages/Bad.yml", read.problems().get(0).file());
        assertEquals(java.util.Set.of("en/messages/Bad.yml"), read.failedFiles());
    }

    @Test
    void neverWritesAnInvalidFile() throws IOException {
        Path file = folder.resolve("en/messages/Player.yml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "Greeting: 'hi'\n");

        List<LanguageProblem> problems = new SplitYamlBackend(folder.toFile())
                .write("en", List.of(new LanguageEntry("Messages.Player.Greeting.Sub", "x", null, null)));

        assertEquals(1, problems.size());
        assertEquals("Greeting: 'hi'\n", Files.readString(file));
    }
}
