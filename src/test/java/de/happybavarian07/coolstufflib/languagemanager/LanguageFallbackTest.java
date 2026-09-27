package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LanguageFallbackTest {
    @TempDir
    Path folder;
    private LanguageManager lgm;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(folder.resolve("en.yml"), """
                LanguageFullName: English
                Messages:
                  OnlyEnglish: 'english only'
                  Shared: 'shared en'
                  Apples:
                    one: '%count% apple'
                    other: '%count% apples'
                  Rich:
                    text: 'rich text'
                    actionbar: 'bar'
                """);
        Files.writeString(folder.resolve("de.yml"), """
                LanguageFullName: Deutsch
                Messages:
                  OnlyGerman: 'nur deutsch'
                  Shared: 'geteilt de'
                """);
        Files.writeString(folder.resolve("at.yml"), """
                LanguageFullName: Oesterreichisch
                LanguageParent: de
                Messages:
                  Shared: 'geteilt at'
                """);
        lgm = new LanguageManager(mock(JavaPlugin.class), folder.toFile(), "none", "[P]");
        lgm.addLanguagesToList(false);
        lgm.setCurrentLang(lgm.getLang("en", true), false);
    }

    private Player playerWithLanguage(String language) {
        UUID uuid = UUID.randomUUID();
        YamlConfiguration data = new YamlConfiguration();
        data.set("playerdata." + uuid + ".language", language);
        lgm.setPLHandler(new PerPlayerLanguageHandler(lgm, new File(folder.toFile(), "data.yml"), data));
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        return player;
    }

    @Test
    void missingKeysComeFromTheParentThenTheServerLanguage() {
        Player austrian = playerWithLanguage("at");
        assertEquals("geteilt at", lgm.getMessage("Shared", austrian, false));
        assertEquals("nur deutsch", lgm.getMessage("OnlyGerman", austrian, false));
        assertEquals("english only", lgm.getMessage("OnlyEnglish", austrian, false));
        assertTrue(lgm.getMessage("Nowhere", austrian, false).startsWith("null path: Messages.Nowhere"));
    }

    @Test
    void theMessagesRootIsOptionalEverywhere() {
        assertEquals("shared en", lgm.getMessage("Messages.Shared", null, false));
        assertEquals("shared en", lgm.getCustomObject("Shared", null, "x", false));
        assertEquals("shared en", lgm.getCustomObject("Messages.Shared", null, "x", false));
    }

    @Test
    void richAndPluralValuesGiveTextToTheOldApi() {
        assertEquals("rich text", lgm.getMessage("Rich", null, false));
        assertEquals("%count% apples", lgm.getMessage("Apples", null, false));
    }

    @Test
    void pluralVariantAndLocalPlaceholders() {
        LanguageFile en = lgm.getLang("en", true);
        assertEquals("1 apple", lgm.renderMessage("Apples", null, en, Map.of(), 1L, false));
        assertEquals("3 apples", lgm.renderMessage("Apples", null, en, Map.of(), 3L, false));
        assertEquals(List.of(), List.copyOf(lgm.getPlaceholders().keySet()));
    }
}
