package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class MessageBuilderTest {
    @TempDir
    Path folder;
    private LanguageManager lgm;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(folder.resolve("en.yml"), """
                Messages:
                  Target: '%prefix% hello %target%'
                  Help:
                    - 'line one %target%'
                    - 'line two'
                  Apples:
                    one: '%count% apple'
                    other: '%count% apples'
                MenuTitles:
                  Menu: 'Menu of %target%'
                """);
        Files.writeString(folder.resolve("de.yml"), """
                Messages:
                  Target: 'hallo %target%'
                """);
        lgm = new LanguageManager(mock(JavaPlugin.class), folder.toFile(), "none", "[P]");
        lgm.addLanguagesToList(false);
        lgm.setCurrentLang(lgm.getLang("en", true), false);
    }

    @Test
    void rendersWithItsOwnPlaceholders() {
        assertEquals("[P] hello Alex", lgm.message("Target").with("%target%", "Alex").text(null));
        assertEquals("hallo Alex", lgm.message("Target").lang("de").with("%target%", "Alex").text(null));
        assertEquals(List.of("line one Alex", "line two"), lgm.message("Help").with("%target%", "Alex").lines(null));
        assertEquals("1 apple", lgm.message("Apples").count(1).text(null));
        assertEquals("Menu of Alex", lgm.title("Menu").with("%target%", "Alex").text(null));
        assertTrue(lgm.getPlaceholders().isEmpty());
    }

    @Test
    void globalPlaceholdersStillApplyAndStay() {
        lgm.addPlaceholder(PlaceholderType.MESSAGE, "%target%", "Global", false);
        assertEquals("[P] hello Global", lgm.message("Target").text(null));
        assertEquals("[P] hello Local", lgm.message("Target").with("%target%", "Local").text(null));
        assertTrue(lgm.getPlaceholders().containsKey("%target%"));
    }

    @Test
    void rendersSafelyFromManyThreads() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<Boolean>> results = new java.util.ArrayList<>();
            for (int t = 0; t < 4; t++) {
                int id = t;
                results.add(pool.submit(() -> {
                    for (int i = 0; i < 500; i++) {
                        String name = "P" + id + "-" + i;
                        if (!lgm.message("Target").with("%target%", name).text(null).endsWith(name)) return false;
                    }
                    return true;
                }));
            }
            for (int i = 0; i < 500; i++) {
                lgm.addPlaceholder(PlaceholderType.ITEM, "%other" + i + "%", i, false);
                lgm.removePlaceholder(PlaceholderType.ITEM, "%other" + i + "%");
            }
            for (Future<Boolean> result : results) assertTrue(result.get(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }
}
