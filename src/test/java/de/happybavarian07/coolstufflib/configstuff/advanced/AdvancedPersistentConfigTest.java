package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers.PropertiesConfigFileHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedPersistentConfigTest {
    Path tempDir = Path.of("");

    @Test
    void testCrudOperations() throws Exception {
        File file = tempDir.resolve("testCrud.properties").toFile();
        var handler = new PropertiesConfigFileHandler();
        AdvancedPersistentConfig config = new AdvancedPersistentConfig("test", file, handler);
        assertEquals("test", config.getName());
        assertNull(config.get("key1"));
        config.setValue("key1", "val1");
        assertEquals("val1", config.get("key1"));
        assertTrue(config.containsKey("key1"));
        config.remove("key1");
        assertFalse(config.containsKey("key1"));
    }

    @Test
    void testFileIO() throws Exception {
        File file = tempDir.resolve("testFileIO.properties").toFile();
        var handler = new PropertiesConfigFileHandler();
        AdvancedPersistentConfig config = new AdvancedPersistentConfig("test", file, handler);
        config.setValue("foo", "bar");
        config.save();
        AdvancedPersistentConfig loaded = new AdvancedPersistentConfig("test", file, handler);
        assertEquals("bar", loaded.get("foo"));
    }

    @Test
    void testPersistence() throws Exception {
        File file = tempDir.resolve("testPersistence.properties").toFile();
        var handler = new PropertiesConfigFileHandler();
        AdvancedPersistentConfig config = new AdvancedPersistentConfig("test", file, handler);
        config.setValue("persist", 42);
        config.save();
        AdvancedPersistentConfig loaded = new AdvancedPersistentConfig("test", file, handler);
        assertEquals(42, Integer.parseInt(loaded.get("persist").toString()));
    }

    @Test
    void testHandlerCorrectness() throws Exception {
        File file = tempDir.resolve("testHandler.properties").toFile();
        var handler = new PropertiesConfigFileHandler();
        AdvancedPersistentConfig config = new AdvancedPersistentConfig("test", file, handler);
        config.setValue("alpha", "a");
        config.setValue("beta", "b");
        config.save();
        var map = handler.load(file);
        System.out.println(map);
        assertEquals("a", map.get("alpha"));
        assertEquals("b", map.get("beta"));
    }
}
