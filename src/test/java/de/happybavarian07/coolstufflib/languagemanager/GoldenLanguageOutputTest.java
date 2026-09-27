package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/** Run with -Dgolden.record=true once to write the expected files from the current implementation. */
class GoldenLanguageOutputTest {
    private static final Path FIXTURES = Path.of("src/test/resources/golden/adminpanel");

    @TempDir
    Path tempDir;

    @ParameterizedTest
    @ValueSource(strings = {"en", "de"})
    void everyMessageTitleAndValueRendersAsBefore(String language) throws IOException {
        Path source = FIXTURES.resolve(language + ".yml");
        Files.copy(source, tempDir.resolve(language + ".yml"));
        LanguageManager lgm = new LanguageManager(mock(JavaPlugin.class), tempDir.toFile(), "golden-none", "[P]");
        LanguageFile file = new LanguageFile(tempDir.toFile(), "golden-none", language);
        lgm.addLang(file, language);
        lgm.setCurrentLang(file, false);

        YamlConfiguration raw = YamlConfiguration.loadConfiguration(source.toFile());
        List<String> lines = new ArrayList<>();
        for (String key : raw.getKeys(true)) {
            if (raw.isConfigurationSection(key) || key.startsWith("Items.")) continue;
            String rendered;
            if (key.startsWith("Messages.") && raw.isString(key)) {
                rendered = lgm.getMessage(key.substring("Messages.".length()), null, false);
            } else if (key.startsWith("MenuTitles.") && raw.isString(key)) {
                rendered = lgm.getMenuTitle(key.substring("MenuTitles.".length()), null);
            } else {
                rendered = String.valueOf(lgm.getCustomObject(key, null, "<none>", false));
            }
            lines.add(key + " = " + rendered.replace("\r", "\\r").replace("\n", "\\n"));
        }

        Path expected = FIXTURES.resolve(language + ".expected.txt");
        if (Boolean.getBoolean("golden.record")) {
            Files.write(expected, lines, StandardCharsets.UTF_8);
            return;
        }
        assertEquals(Files.readAllLines(expected, StandardCharsets.UTF_8), lines);
    }
}
