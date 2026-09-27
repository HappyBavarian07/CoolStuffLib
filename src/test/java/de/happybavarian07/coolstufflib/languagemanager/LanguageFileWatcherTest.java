package de.happybavarian07.coolstufflib.languagemanager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageFileWatcherTest {
    @TempDir
    Path folder;

    @Test
    void reportsTheChangedLanguageOnceAfterTheDebounce() throws Exception {
        Files.createDirectories(folder.resolve("en/messages"));
        CompletableFuture<Set<String>> changed = new CompletableFuture<>();
        try (LanguageFileWatcher watcher = new LanguageFileWatcher(folder, 200, changed::complete)) {
            watcher.start();
            Thread.sleep(200);
            Files.writeString(folder.resolve("en/messages/Player.yml"), "A: 'a'\n");
            Files.writeString(folder.resolve("en/messages/Player.yml"), "A: 'b'\n");
            assertEquals(Set.of("en"), changed.get(10, TimeUnit.SECONDS));
        }
    }
}
