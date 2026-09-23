package de.happybavarian07.coolstufflib.service;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.service.impl.ChatInputService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatInputServiceTest {
    @Test
    void initRegistersListenerOnCallingThread() {
        ChatInputService service = new ChatInputService();
        PluginManager pluginManager = mock(PluginManager.class);
        JavaPlugin plugin = mock(JavaPlugin.class);
        CoolStuffLib lib = mock(CoolStuffLib.class);
        when(lib.getJavaPluginUsingLib()).thenReturn(plugin);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<CoolStuffLib> statics = mockStatic(CoolStuffLib.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            statics.when(CoolStuffLib::getLib).thenReturn(lib);

            var future = service.init();
            assertTrue(future.isDone());
            assertFalse(future.isCompletedExceptionally());
            verify(pluginManager).registerEvents(service, plugin);
        }
    }

    @Test
    void initFailureIsReportedAsFailedFuture() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenThrow(new IllegalStateException("no server"));
            assertTrue(new ChatInputService().init().isCompletedExceptionally());
        }
    }
}
