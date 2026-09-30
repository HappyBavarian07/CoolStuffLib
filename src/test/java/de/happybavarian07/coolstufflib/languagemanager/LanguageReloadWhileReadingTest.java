package de.happybavarian07.coolstufflib.languagemanager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class LanguageReloadWhileReadingTest {
    @TempDir
    Path folder;

    @Test
    void everyReadDuringReloadsIsOneOfTheTwoFileContents() throws Exception {
        Path file = folder.resolve("en/messages/_root.yml");
        Files.createDirectories(file.getParent());
        write(file, "one");
        LanguageConfig config = new LanguageConfig(folder.resolve("en.yml").toFile(), folder.toFile(), "none", "en");
        assertEquals("one", config.getConfig().getString("Messages.Plain"));

        AtomicBoolean running = new AtomicBoolean(true);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread reloader = new Thread(() -> {
            try {
                boolean flip = false;
                while (running.get()) {
                    write(file, (flip = !flip) ? "two" : "one");
                    config.reloadConfig();
                }
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            }
        }, "reloader");
        reloader.setDaemon(true);
        reloader.start();

        Set<String> allowed = Set.of("one", "two");
        long end = System.nanoTime() + 1_500_000_000L;
        int reads = 0;
        while (System.nanoTime() < end && failure.get() == null) {
            String value = config.getConfig().getString("Messages.Plain");
            assertTrue(allowed.contains(value), "read a half-loaded value: " + value);
            Object loaded = config.getLoaded().merged().get("Messages.Plain").value();
            assertTrue(allowed.contains(String.valueOf(loaded)), "read a half-loaded value: " + loaded);
            reads++;
        }
        running.set(false);
        reloader.join(10_000);

        assertNull(failure.get(), String.valueOf(failure.get()));
        assertFalse(reloader.isAlive());
        assertTrue(reads > 100, "the reader barely ran: " + reads);
    }

    private static void write(Path file, String value) throws IOException {
        Files.writeString(file, "Plain: '" + value + "'\n");
    }
}
