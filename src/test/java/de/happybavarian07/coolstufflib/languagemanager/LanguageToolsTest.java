package de.happybavarian07.coolstufflib.languagemanager;

import de.happybavarian07.coolstufflib.languagemanager.storage.LegacyYamlBackend;
import de.happybavarian07.coolstufflib.languagemanager.storage.MigrationReport;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class LanguageToolsTest {
    @TempDir
    Path folder;
    private LanguageManager lgm;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(folder.resolve("en.yml"), "Messages:\n  Player:\n    Greeting: '&aHello there'\n    Bye: 'Bye'\n");
        lgm = new LanguageManager(mock(JavaPlugin.class), folder.toFile(), "lang-fixtures/legacy", "[P]");
        lgm.addLanguagesToList(false);
        lgm.setCurrentLang(lgm.getLang("en", true), false);
    }

    @Test
    void debugShowsTheKeyAndItsOrigin() {
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        assertTrue(lgm.toggleDebug(console));
        assertTrue(lgm.getMessage("Player.Greeting", null, false).endsWith("[Messages.Player.Greeting @ en/messages/Player.yml:1]"));
        assertFalse(lgm.toggleDebug(console));
        assertFalse(lgm.getMessage("Player.Greeting", null, false).contains("["));
    }

    @Test
    void findsKeysByVisibleText() {
        assertEquals(List.of("Messages.Player.Greeting @ en/messages/Player.yml:1"), lgm.findKeys("en", "hello THERE", 10));
    }

    @Test
    void reportsKeysTheFilesDoNotKnow() {
        LanguageManager.MissingKeys missing = lgm.missingKeys("en");
        assertTrue(missing.unknownToPlugin().contains("Messages.Player.Bye"));
        assertTrue(missing.missingInFiles().isEmpty());
    }

    @Test
    void migratesAllLanguagesToAnotherBackend() {
        List<MigrationReport> reports = lgm.migrateAll(LegacyYamlBackend.ID);
        assertTrue(reports.stream().allMatch(MigrationReport::ok), reports.toString());
        assertTrue(Files.exists(folder.resolve("en.yml")));
        assertEquals("§aHello there", lgm.getMessage("Player.Greeting", null, false));
    }
}
