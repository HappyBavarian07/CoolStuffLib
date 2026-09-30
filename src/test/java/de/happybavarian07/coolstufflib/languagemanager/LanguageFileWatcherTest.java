package de.happybavarian07.coolstufflib.languagemanager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LanguageFileWatcherTest {
    @TempDir
    Path folder;

    @Test
    void reportsTheChangedLanguageOnceAfterTheDebounce() throws Exception {
        Files.createDirectories(folder.resolve("en/messages"));
        BlockingQueue<Set<String>> reported = new LinkedBlockingQueue<>();
        try (LanguageFileWatcher watcher = new LanguageFileWatcher(folder, 200, reported::add)) {
            watcher.start();
            Thread.sleep(200);
            Files.writeString(folder.resolve("en/messages/Player.yml"), "A: 'a'\n");
            Files.writeString(folder.resolve("en/messages/Player.yml"), "A: 'b'\n");
            assertEquals(Set.of("en"), reported.poll(10, TimeUnit.SECONDS));
            assertNull(reported.poll(800, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    void aFailingReloadDoesNotStopTheWatcher() throws Exception {
        Files.createDirectories(folder.resolve("en"));
        BlockingQueue<Set<String>> reported = new LinkedBlockingQueue<>();
        AtomicBoolean failed = new AtomicBoolean();
        try (LanguageFileWatcher watcher = new LanguageFileWatcher(folder, 200, languages -> {
            reported.add(languages);
            if (failed.compareAndSet(false, true)) throw new IllegalStateException("reload failed");
        })) {
            watcher.start();
            Files.writeString(folder.resolve("en/language.yml"), "A: 'a'\n");
            assertEquals(Set.of("en"), reported.poll(10, TimeUnit.SECONDS));
            Files.writeString(folder.resolve("en/language.yml"), "A: 'b'\n");
            assertEquals(Set.of("en"), reported.poll(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void reportsAChangeBelowTheFirstLevelOfANewTree() throws Exception {
        BlockingQueue<Set<String>> reported = new LinkedBlockingQueue<>();
        try (LanguageFileWatcher watcher = new LanguageFileWatcher(folder, 200, reported::add)) {
            watcher.start();
            Files.createDirectories(folder.resolve("de/a/b/c"));
            Files.writeString(folder.resolve("de/a/b/c/Deep.yml"), "A: 'a'\n");
            drain(reported);
            Files.writeString(folder.resolve("de/a/b/c/Deep.yml"), "A: 'b'\n");
            assertEquals(Set.of("de"), reported.poll(10, TimeUnit.SECONDS));
        }
    }

    /** Waits until the watcher has been quiet for one debounce, so the next report is the one under test. */
    private static void drain(BlockingQueue<Set<String>> reported) throws InterruptedException {
        while (reported.poll(400, TimeUnit.MILLISECONDS) != null) {
        }
    }
}
