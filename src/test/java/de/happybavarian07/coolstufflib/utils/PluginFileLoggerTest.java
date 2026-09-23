package de.happybavarian07.coolstufflib.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.*;

class PluginFileLoggerTest {
    @TempDir
    Path tempDir;

    @Test
    void writesPrefixedLineForLogPrefixOverload() throws IOException {
        PluginFileLogger logger = new PluginFileLogger(tempDir.toFile(), "test.log");
        logger.writeToLog(Level.INFO, "hello world", LogPrefix.INFO, false);
        List<String> lines = Files.readAllLines(logger.getLogFile().toPath());
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).endsWith("INFO]: [Info] hello world"), lines.get(0));
    }

    @Test
    void appendsForStringPrefixOverload() throws IOException {
        PluginFileLogger logger = new PluginFileLogger(tempDir.toFile(), "test.log");
        logger.writeToLog(Level.WARNING, "first", "Custom", false);
        logger.writeToLog(Level.WARNING, "second", "Custom", false);
        List<String> lines = Files.readAllLines(logger.getLogFile().toPath());
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).endsWith("[Custom] second"));
    }

    @Test
    void unwritableLogFileDoesNotThrow() {
        File directoryAsLogFile = tempDir.resolve("dir.log").toFile();
        assertTrue(directoryAsLogFile.mkdir());
        PluginFileLogger logger = new PluginFileLogger(tempDir.toFile(), "dir.log");
        assertDoesNotThrow(() -> logger.writeToLog(Level.SEVERE, "lost", LogPrefix.ERROR, true));
    }

    @Test
    void warningPrefixIsLabelledWarning() {
        assertEquals("Warning", LogPrefix.WARNING.getLogPrefix());
    }
}
