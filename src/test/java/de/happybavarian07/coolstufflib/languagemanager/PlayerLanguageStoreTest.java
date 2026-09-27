package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PlayerLanguageStoreTest {
    @TempDir
    Path folder;

    @Test
    void emptyDataFileHasNoPlayerLanguages() {
        YamlPlayerLanguageStore store = new YamlPlayerLanguageStore(folder.resolve("data.yml").toFile(), new YamlConfiguration());
        assertTrue(store.all().isEmpty());
    }

    @Test
    void keepsTheDataYmlLayout() throws IOException {
        File dataFile = folder.resolve("data.yml").toFile();
        YamlPlayerLanguageStore store = new YamlPlayerLanguageStore(dataFile, new YamlConfiguration());
        UUID uuid = UUID.randomUUID();
        store.set(uuid, "de");
        assertTrue(Files.readString(dataFile.toPath()).contains("playerdata:"));
        assertEquals("de", YamlConfiguration.loadConfiguration(dataFile).getString("playerdata." + uuid + ".language"));
        assertEquals("de", store.all().get(uuid));
        store.remove(uuid);
        assertNull(store.get(uuid));
    }

    @Test
    void handlerWithAnEmptyStoreDoesNotThrow() throws IOException {
        Files.writeString(folder.resolve("en.yml"), "Messages:\n  A: 'a'\n");
        LanguageManager lgm = new LanguageManager(mock(JavaPlugin.class), folder.toFile(), "none", "[P]");
        lgm.addLanguagesToList(false);
        lgm.setCurrentLang(lgm.getLang("en", true), false);
        PerPlayerLanguageHandler handler = new PerPlayerLanguageHandler(lgm, new YamlPlayerLanguageStore(folder.resolve("data.yml").toFile(), new YamlConfiguration()));
        assertTrue(handler.getPlayerLanguages().isEmpty());
    }
}
