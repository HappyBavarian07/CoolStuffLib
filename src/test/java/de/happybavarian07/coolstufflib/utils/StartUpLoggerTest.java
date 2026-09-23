package de.happybavarian07.coolstufflib.utils;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StartUpLoggerTest {
    @Test
    void queuedMessagesReachConsoleAndTogglingDoesNotThrow() {
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        StartUpLogger logger = newLogger(console);

        logger.addMessageToQueue("a", "b");
        verify(console, timeout(2000)).sendMessage("b");

        assertDoesNotThrow(logger::disableMessageSystem);
        assertFalse(logger.isMessageSystemEnabled());
        logger.addMessageToQueue("dropped", "x");

        assertDoesNotThrow(logger::enableMessageSystem);
        logger.addMessageToQueue("c", "d");
        verify(console, timeout(2000)).sendMessage("d");
        verify(console, never()).sendMessage("dropped");
    }

    @Test
    void workerThreadStopsWhenInterrupted() throws Exception {
        StartUpLogger logger = newLogger(mock(ConsoleCommandSender.class));
        Field field = StartUpLogger.class.getDeclaredField("messageQueueThread");
        field.setAccessible(true);
        Thread worker = (Thread) field.get(logger);

        worker.interrupt();
        worker.join(2000);

        assertFalse(worker.isAlive());
    }

    private static StartUpLogger newLogger(ConsoleCommandSender console) {
        CoolStuffLib lib = mock(CoolStuffLib.class);
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(lib.getJavaPluginUsingLib()).thenReturn(plugin);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getConsoleSender).thenReturn(console);
            return new StartUpLogger(lib);
        }
    }
}
