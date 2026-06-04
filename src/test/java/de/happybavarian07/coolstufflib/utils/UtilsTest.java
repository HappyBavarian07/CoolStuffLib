package de.happybavarian07.coolstufflib.utils;

import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigTypeConverterRegistry;
import de.happybavarian07.coolstufflib.utils.Utils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    static final Path tempDir = Path.of("TestOutputs/UtilsTest");

    @Test
    void testFlattenMap() {
        Map<String, Object> nested = Map.of(
            "simple", "value",
            "nested", Map.of(
                "inner", "data",
                "deep", Map.of(
                    "value", "deepValue"
                )
            ),
            "list", List.of("item1", "item2")
        );

        Map<String, Object> flattened = ConfigUtils.flatten(ConfigTypeConverterRegistry.defaultRegistry(), "", nested);
        System.out.println(flattened);

        assertEquals("value", flattened.get("simple"));
        assertEquals("data", flattened.get("nested.inner"));
        assertEquals("deepValue", flattened.get("nested.deep.value"));
        assertNotNull(flattened.get("list.0"));
        assertNotNull(flattened.get("list.1"));
    }

    @Test
    void testUnflattenMap() {
        Map<String, String> flat = Map.of(
            "simple", "value",
            "nested.inner", "data",
            "nested.deep.value", "deepValue",
            "config.database.host", "localhost",
            "config.database.port", "5432"
        );

        Map<String, Object> unflattened = (Map<String, Object>) ConfigUtils.unflatten(ConfigTypeConverterRegistry.defaultRegistry(), flat);

        assertEquals("value", unflattened.get("simple"));

        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) unflattened.get("nested");
        assertEquals("data", nested.get("inner"));

        @SuppressWarnings("unchecked")
        Map<String, Object> deep = (Map<String, Object>) nested.get("deep");
        assertEquals("deepValue", deep.get("value"));
    }

    @Test
    void testIsValidPath() {
        assertTrue(FileUtils.isValidPath("simple.key"));
        assertTrue(FileUtils.isValidPath("nested.section.value"));
        assertTrue(FileUtils.isValidPath("a.b.c.d.e"));

        assertFalse(FileUtils.isValidPath(""));
        assertFalse(FileUtils.isValidPath(".invalid"));
        assertFalse(FileUtils.isValidPath("invalid."));
        assertFalse(FileUtils.isValidPath("double..dot"));
    }

    @Test
    void testSanitizeString() {
        assertEquals("hello_world", Utils.sanitize("hello world"));
        assertEquals("test_string", Utils.sanitize("test-string"));
        assertEquals("no_special_chars", Utils.sanitize("no!@#$%special*()chars"));
        assertEquals("", Utils.sanitize("!@#$%^&*()"));
    }

    @Test
    void testParseBoolean() {
        assertTrue(ConfigUtils.parseBoolean("true"));
        assertTrue(ConfigUtils.parseBoolean("TRUE"));
        assertTrue(ConfigUtils.parseBoolean("yes"));
        assertTrue(ConfigUtils.parseBoolean("1"));

        assertFalse(ConfigUtils.parseBoolean("false"));
        assertFalse(ConfigUtils.parseBoolean("FALSE"));
        assertFalse(ConfigUtils.parseBoolean("no"));
        assertFalse(ConfigUtils.parseBoolean("0"));
        assertFalse(ConfigUtils.parseBoolean("invalid"));
    }

    @Test
    void testParseNumber() {
        assertEquals(123, ConfigUtils.parseNumber("123"));
        assertEquals(123.45, ConfigUtils.parseNumber("123.45"));
        assertEquals(-456, ConfigUtils.parseNumber("-456"));
        assertEquals(0, ConfigUtils.parseNumber("0"));

        assertNull(ConfigUtils.parseNumber("not_a_number"));
        assertNull(ConfigUtils.parseNumber(""));
    }

    //@Test
    void testCreateDirectories() throws IOException {
        Path testPath = tempDir.resolve("nested").resolve("directory").resolve("structure");

        assertFalse(Files.exists(testPath));
        FileUtils.createDirectories(testPath.toFile());
        assertTrue(Files.exists(testPath));
        assertTrue(Files.isDirectory(testPath));
    }

    @Test
    void testCopyFile() throws IOException {
        Path source = tempDir.resolve("source.txt");
        Path target = tempDir.resolve("target.txt");

        Files.writeString(source, "Test content for copying");

        FileUtils.copyFile(source.toFile(), target.toFile());

        assertTrue(Files.exists(target));
        assertEquals("Test content for copying", Files.readString(target));
    }

    @Test
    void testDeleteDirectory() throws IOException {
        Path testDir = tempDir.resolve("to_delete");
        Path subDir = testDir.resolve("subdir");
        Path file = subDir.resolve("file.txt");

        Files.createDirectories(subDir);
        Files.writeString(file, "content");

        assertTrue(Files.exists(testDir));
        FileUtils.deleteDirectory(testDir.toFile());
        assertFalse(Files.exists(testDir));
    }

    @Test
    void testGetFileExtension() {
        assertEquals("txt", FileUtils.getFileExtension(new File("test.txt")));
        assertEquals("yaml", FileUtils.getFileExtension(new File("config.yaml")));
        assertEquals("", FileUtils.getFileExtension(new File("noextension")));
        assertEquals("json", FileUtils.getFileExtension(new File("path/to/file.json")));
    }

    @Test
    void testJoinPath() {
        assertEquals("a.b.c", FileUtils.joinPath(".", "a", "b", "c"));
        assertEquals("single", FileUtils.joinPath(".", "single"));
        assertEquals("", FileUtils.joinPath("."));
        assertEquals("a.b", FileUtils.joinPath(".", "a", "", "b"));
    }

    @Test
    void testSplitPath() {
        assertArrayEquals(new String[]{"a", "b", "c"}, FileUtils.splitPath(".", "a.b.c"));
        assertArrayEquals(new String[]{"single"}, FileUtils.splitPath(".", "single"));
        assertArrayEquals(new String[]{}, FileUtils.splitPath(".", ""));
    }

    @Test
    void testTimeFormatting() {
        assertEquals("1s", Utils.formatDuration(1000, TimeUnit.MILLISECONDS));
        assertEquals("1m 30s", Utils.formatDuration(90000, TimeUnit.MILLISECONDS));
        assertEquals("1h 5m", Utils.formatDuration(3900000, TimeUnit.MILLISECONDS));
        assertEquals("0s", Utils.formatDuration(0, TimeUnit.MILLISECONDS));
    }
}
