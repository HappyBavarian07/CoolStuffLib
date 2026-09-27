package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LegacyAndClasspathTest {
    @TempDir
    Path folder;

    @Test
    void legacyBackendReadsAndWritesTheSingleFile() throws IOException {
        Files.copy(Path.of("src/test/resources/lang-fixtures/legacy/en.yml"), folder.resolve("en.yml"));
        LegacyYamlBackend backend = new LegacyYamlBackend(folder.toFile());

        ReadResult read = backend.read("en");
        assertEquals("%prefix% Hello", read.entries().get("Messages.Player.Greeting").value());
        assertEquals("greeting", read.entries().get("Messages.Player.Greeting").comment());
        assertEquals("Header comment", read.entries().get("LanguageFullName").comment());
        assertEquals("the heal item", read.sectionComments().get("Items.Heal"));
        assertEquals(Set.of("en"), backend.languages());
        assertEquals("en.yml", backend.fileOf("en", "Messages.Player.Greeting"));

        assertTrue(backend.write("en", List.of(new LanguageEntry("Messages.Player.Bye", "Bye", null, null))).isEmpty());
        assertEquals("Bye", backend.read("en").entries().get("Messages.Player.Bye").value());
    }

    @Test
    void classpathSourcePrefersTheSplitLayout() {
        ClasspathLanguageSource split = new ClasspathLanguageSource(getClass().getClassLoader(), "lang-fixtures/split");
        ReadResult read = split.read("en");
        assertEquals("Hello from the split layout", read.entries().get("Messages.Player.Greeting").value());
        assertEquals("the player's messages", read.sectionComments().get("Messages.Player"));
        assertEquals(Set.of("en"), split.languages());
        assertFalse(split.isWritable());
    }

    @Test
    void aSplitResourceIsFetchedOnlyOnce() {
        CountingLoader loader = new CountingLoader("lang-fixtures/split/en/messages/Player.yml");

        ReadResult read = new ClasspathLanguageSource(loader, "lang-fixtures/split").read("en");

        assertEquals("Hello from the split layout", read.entries().get("Messages.Player.Greeting").value());
        assertEquals("the player's messages", read.sectionComments().get("Messages.Player"));
        assertEquals(1, loader.reads(), "the resource was fetched " + loader.reads() + " times");
    }

    /** Counts how often one resource is fetched, so a doubled read is visible. */
    private static final class CountingLoader extends ClassLoader {
        private final String resource;
        private int reads;

        private CountingLoader(String resource) {
            super(LegacyAndClasspathTest.class.getClassLoader());
            this.resource = resource;
        }

        @Override
        public URL getResource(String name) {
            if (resource.equals(name)) reads++;
            return super.getResource(name);
        }

        private int reads() {
            return reads;
        }
    }

    @Test
    void classpathSourceReadsASingleFile() {
        ClasspathLanguageSource legacy = new ClasspathLanguageSource(getClass().getClassLoader(), "lang-fixtures/legacy");
        ReadResult read = legacy.read("en");
        assertEquals("%prefix% Hello", read.entries().get("Messages.Player.Greeting").value());
        assertEquals("jar:lang-fixtures/legacy/en.yml:6", read.entries().get("Messages.Player.Greeting").origin().toString());
        assertTrue(legacy.read("fr").entries().isEmpty());
    }
}
