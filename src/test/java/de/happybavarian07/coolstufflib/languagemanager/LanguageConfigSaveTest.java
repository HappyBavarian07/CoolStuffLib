package de.happybavarian07.coolstufflib.languagemanager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LanguageConfigSaveTest {
    @TempDir
    Path folder;

    @Test
    void saveConfigWritesOnlyTheChangedKeyLine() throws IOException {
        Files.writeString(folder.resolve("en.yml"), "Messages:\n  Plain: 'mine'\n  Other: 'same'\n");
        LanguageConfig config = new LanguageConfig(folder.resolve("en.yml").toFile(), folder.toFile(), "lang-fixtures/legacy", "en");
        Path root = folder.resolve("en/messages/_root.yml");
        String before = Files.readString(root);
        List<String> filesBefore = snapshot();

        config.getConfig().set("Messages.Plain", "changed");
        config.saveConfig();

        List<String> after = Files.readAllLines(root);
        List<String> old = before.lines().toList();
        assertEquals(old.size(), after.size(), after.toString());
        List<Integer> differing = new ArrayList<>();
        for (int i = 0; i < old.size(); i++) if (!old.get(i).equals(after.get(i))) differing.add(i);
        assertEquals(1, differing.size(), after.toString());
        assertTrue(after.get(differing.get(0)).contains("Plain") && after.get(differing.get(0)).contains("changed"), after.toString());
        assertEquals("changed", config.getConfig().getString("Messages.Plain"));
        assertEquals("same", config.getConfig().getString("Messages.Other"));

        List<String> filesAfter = snapshot();
        assertEquals(filesBefore.size(), filesAfter.size());
        for (int i = 0; i < filesBefore.size(); i++) {
            if (filesBefore.get(i).startsWith(root.toString() + "\n")) continue;
            assertEquals(filesBefore.get(i), filesAfter.get(i), "another file changed");
        }
    }

    @Test
    void saveConfigWithoutChangesLeavesEveryFileAlone() throws IOException {
        Files.writeString(folder.resolve("en.yml"), "Messages:\n  Plain: 'mine'\n");
        LanguageConfig config = new LanguageConfig(folder.resolve("en.yml").toFile(), folder.toFile(), "lang-fixtures/legacy", "en");
        List<String> before = snapshot();

        config.saveConfig();

        assertEquals(before, snapshot());
    }

    private List<String> snapshot() throws IOException {
        try (Stream<Path> walk = Files.walk(folder.resolve("en"))) {
            List<Path> files = walk.filter(Files::isRegularFile).sorted().toList();
            List<String> texts = new ArrayList<>();
            for (Path file : files) texts.add(file + "\n" + Files.readString(file));
            return texts;
        }
    }
}
