package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class LanguageReportTest {
    private static final String JAR = "lang-fixtures/legacy";

    @TempDir
    Path folder;

    private void legacyFile() throws IOException {
        Files.writeString(folder.resolve("en.yml"), "Messages:\n  Plain: 'mine'\n");
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
}
