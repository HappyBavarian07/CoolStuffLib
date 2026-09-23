package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CustomObjectTest {
    @TempDir
    Path tempDir;

    @Test
    void zeroIsAValidConfiguredValue() throws IOException {
        Files.writeString(tempDir.resolve("en_zero.yml"), """
                LanguageFullName: English (Test)
                LanguageVersion: 1.0
                Items:
                  First:
                    slot: 0
                  Second:
                    slot: 7
                """);
        LanguageManager lgm = new LanguageManager(mock(JavaPlugin.class), tempDir.toFile(), "resources", "[TEST]");
        LanguageFile lang = new LanguageFile(tempDir.toFile(), "resources", "en_zero");
        lgm.addLang(lang, "en_zero");
        lgm.setCurrentLang(lang, false);

        assertEquals(0, lgm.getCustomObject("Items.First.slot", null, 5, false));
        assertEquals(7, lgm.getCustomObject("Items.Second.slot", null, 5, false));
        assertEquals(5, lgm.getCustomObject("Items.Missing.slot", null, 5, false));
    }
}
