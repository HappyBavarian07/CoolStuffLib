package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LanguageReportTest {
    private static final String JAR = "lang-fixtures/legacy";

    @TempDir
    Path folder;

    private void legacyFile() throws IOException {
        Files.writeString(folder.resolve("en.yml"), "Messages:\n  Plain: 'mine'\n");
    }

    @Test
    void theStartReportNamesTheMigrationAndTheAddedKeysOnceAndProblemsOncePerChange() throws IOException {
        legacyFile();
        LanguageConfig config = new LanguageConfig(folder.resolve("en.yml").toFile(), folder.toFile(), JAR, "en");

        List<String> start = config.drainReport();
        assertTrue(start.stream().anyMatch(line -> line.startsWith("Moved en.yml into the folder en/")), start.toString());
        assertTrue(start.stream().anyMatch(line -> line.startsWith("Added ") && line.contains("Messages.Player.Greeting")), start.toString());
        assertEquals(List.of(), config.drainReport());

        Path player = folder.resolve("en/messages/Player.yml");
        Files.writeString(player, "Greeting: 'broken\n");
        config.reloadConfig();
        List<String> problem = config.drainReport();
        assertEquals(1, problem.size(), problem.toString());
        assertTrue(problem.get(0).startsWith("Problem in "), problem.get(0));
        config.reloadConfig();
        assertEquals(List.of(), config.drainReport());

        Files.writeString(player, "Greeting: 'fixed'\n");
        config.reloadConfig();
        assertEquals(List.of(), config.drainReport());
        assertEquals("fixed", config.getConfig().getString("Messages.Player.Greeting"));
    }

    @Test
    void addLangDrainsTheReportOfAFileBuiltBeforeAddLanguagesToList() throws IOException {
        legacyFile();
        LanguageManager lgm = new LanguageManager(mock(JavaPlugin.class), folder.toFile(), JAR, "[P]");
        LanguageFile en = new LanguageFile(folder.toFile(), JAR, "en");

        lgm.addLanguagesToList(false);
        assertTrue(lgm.getLastReport().isEmpty(), lgm.getLastReport().toString());
        lgm.addLang(en, "en");

        assertTrue(lgm.getLastReport().stream().anyMatch(line -> line.startsWith("Moved en.yml")), lgm.getLastReport().toString());
        assertEquals(List.of(), en.getLangConfig().drainReport());
    }

    @Test
    void updateLangFilesAddsMissingKeysAndReportsThem() throws IOException {
        legacyFile();
        LanguageManager lgm = new LanguageManager(mock(JavaPlugin.class), folder.toFile(), JAR, "[P]");
        lgm.addLanguagesToList(false);
        Path heal = folder.resolve("en/items/Heal.yml");
        assertTrue(Files.deleteIfExists(heal));

        lgm.updateLangFiles();

        assertTrue(Files.exists(heal));
        assertTrue(lgm.getLastReport().stream().anyMatch(line -> line.startsWith("Added ") && line.contains("Items.Heal")), lgm.getLastReport().toString());
    }

    @Test
    void reloadLanguagesPicksUpEditsAndNewLanguages() throws IOException {
        legacyFile();
        JavaPlugin plugin = mock(JavaPlugin.class);
        YamlConfiguration pluginConfig = new YamlConfiguration();
        pluginConfig.set("Plugin.language", "en");
        when(plugin.getConfig()).thenReturn(pluginConfig);
        LanguageManager lgm = new LanguageManager(plugin, folder.toFile(), JAR, "[P]");
        lgm.addLanguagesToList(false);
        lgm.setCurrentLang(lgm.getLang("en", true), false);
        assertEquals("mine", lgm.getMessage("Plain", null, false));

        Path root = folder.resolve("en/messages/_root.yml");
        Files.writeString(root, Files.readString(root).replace("mine", "edited"));
        Files.createDirectories(folder.resolve("de/messages"));
        Files.writeString(folder.resolve("de/messages/Player.yml"), "Greeting: 'Hallo'\n");

        lgm.reloadLanguages(null, false);

        assertEquals("edited", lgm.getMessage("Plain", null, false));
        assertNotNull(lgm.getLang("de", false));
        assertEquals("Hallo", lgm.getLang("de", true).getLangConfig().getConfig().getString("Messages.Player.Greeting"));
    }
}
