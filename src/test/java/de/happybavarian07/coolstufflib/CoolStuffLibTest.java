package de.happybavarian07.coolstufflib;

import de.happybavarian07.coolstufflib.service.api.Service;
import de.happybavarian07.coolstufflib.service.api.ServiceDescriptor;
import de.happybavarian07.coolstufflib.service.api.ServiceRegistry;
import de.happybavarian07.coolstufflib.service.api.ServiceState;
import de.happybavarian07.coolstufflib.utils.LogPrefix;
import de.happybavarian07.coolstufflib.utils.PluginFileLogger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CoolStuffLibTest {
    private JavaPlugin plugin;

    @BeforeEach
    void setUp() throws Exception {
        resetSingleton();
        plugin = mock(JavaPlugin.class);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(plugin.getName()).thenReturn("TestPlugin");
    }

    @AfterEach
    void tearDown() throws Exception {
        resetSingleton();
    }

    @Test
    void requireServiceInstantiatesRegistersAndStarts() {
        CoolStuffLib lib = newLib(null);
        NoArgService first = lib.requireService("no-arg", NoArgService.class);
        ServiceRegistry registry = lib.getServiceRegistry();
        assertEquals(ServiceState.RUNNING, registry.getStateByName("no-arg"));
        assertSame(first, lib.requireService("no-arg", NoArgService.class));
        assertEquals(1, first.initCalls);
    }

    @Test
    void requireServiceRestartsStoppedService() {
        CoolStuffLib lib = newLib(null);
        NoArgService svc = lib.requireService("restart", NoArgService.class);
        ServiceRegistry registry = lib.getServiceRegistry();
        registry.stop(registry.getIdByName("restart")).join();
        assertEquals(ServiceState.STOPPED, registry.getStateByName("restart"));
        lib.requireService("restart", NoArgService.class);
        assertEquals(ServiceState.RUNNING, registry.getStateByName("restart"));
        assertEquals(2, svc.initCalls);
    }

    @Test
    void requireServiceRejectsTypeMismatch() {
        CoolStuffLib lib = newLib(null);
        lib.getServiceRegistry().register(ServiceDescriptor.of("taken"), new NoArgService(), null);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> lib.requireService("taken", NoNoArgService.class));
        assertTrue(ex.getMessage().contains(NoArgService.class.getName()));
    }

    @Test
    void requireServiceExplainsMissingNoArgConstructor() {
        CoolStuffLib lib = newLib(null);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> lib.requireService("needs-args", NoNoArgService.class));
        assertTrue(ex.getMessage().contains("no-arg constructor"));
        assertEquals(ServiceState.UNREGISTERED, lib.getServiceRegistry().getStateByName("needs-args"));
    }

    @Test
    void setupRegistersCoreServicesAndReportsFailedOnes() {
        CoolStuffLib lib = newLib(null);
        lib.getServiceRegistry().register(ServiceDescriptor.of("failing"), new FailingService(), null);
        IllegalStateException ex = assertThrows(IllegalStateException.class, lib::setup);
        assertTrue(ex.getMessage().contains("failing"), ex.getMessage());
        assertNotEquals(ServiceState.UNREGISTERED, lib.getServiceRegistry().getStateByName("chat-input-service"));
    }

    @Test
    void logErrorWritesToFileLoggerWhenInitialized() {
        PluginFileLogger fileLogger = mock(PluginFileLogger.class);
        newLib(fileLogger);
        CoolStuffLib.logError("boom", new RuntimeException("cause"));
        verify(fileLogger).writeToLog(eq(Level.SEVERE), contains("boom"), eq(LogPrefix.ERROR), eq(false));
    }

    @Test
    void writeToLogWithoutFileLoggerOnlyUsesConsoleWhenAsked() {
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger logger = Logger.getLogger("CoolStuffLib");
        logger.addHandler(handler);
        try {
            CoolStuffLib lib = newLib(null);
            lib.writeToLog(Level.INFO, "quiet", LogPrefix.INFO, false);
            lib.writeToLog(Level.INFO, "loud", LogPrefix.INFO, true);
            lib.writeToLog(Level.WARNING, "warn", LogPrefix.WARNING, false);
            assertEquals(List.of("loud", "warn"), records.stream().map(LogRecord::getMessage).toList());
        } finally {
            logger.removeHandler(handler);
        }
    }

    @Test
    void logErrorIsSafeBeforeInitialization() {
        assertDoesNotThrow(() -> CoolStuffLib.logError("early", new RuntimeException()));
    }

    @Test
    void reinitializationLogsWarning() {
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger logger = Logger.getLogger("CoolStuffLib");
        logger.addHandler(handler);
        try {
            newLib(null);
            assertTrue(records.stream().noneMatch(r -> r.getLevel() == Level.WARNING));
            CoolStuffLib second = newLib(null);
            assertTrue(records.stream().anyMatch(r -> r.getLevel() == Level.WARNING && r.getMessage().contains("TestPlugin")));
            assertSame(second, CoolStuffLib.getLib());
        } finally {
            logger.removeHandler(handler);
        }
    }

    @Test
    void nullPluginIsRejectedWithoutReplacingSingleton() {
        CoolStuffLib valid = newLib(null);
        plugin = null;
        assertThrows(RuntimeException.class, () -> newLib(null));
        assertSame(valid, CoolStuffLib.getLib());
    }

    private CoolStuffLib newLib(PluginFileLogger fileLogger) {
        return new CoolStuffLib(plugin, null, null, null, null, null, null, fileLogger, false, false,
                args -> {}, args -> {}, args -> {}, args -> {}, args -> {}, args -> {}, null);
    }

    private static void resetSingleton() throws Exception {
        Field field = CoolStuffLib.class.getDeclaredField("lib");
        field.setAccessible(true);
        field.set(null, null);
    }

    public static class NoArgService implements Service {
        private UUID id;
        private String name;
        int initCalls;

        @Override
        public UUID id() {
            return id;
        }

        @Override
        public String serviceName() {
            return name;
        }

        @Override
        public CompletableFuture<Void> init() {
            initCalls++;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> shutdown() {
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class NoNoArgService extends NoArgService {
        public NoNoArgService(String required) {
        }
    }

    public static class FailingService extends NoArgService {
        @Override
        public CompletableFuture<Void> init() {
            return CompletableFuture.failedFuture(new IllegalStateException("init failed"));
        }
    }

    @Test
    void shutdownStopsServices() {
        CoolStuffLib lib = newLib(null);
        lib.requireService("stop-me", NoArgService.class);
        try (org.mockito.MockedStatic<org.bukkit.Bukkit> bukkit = mockStatic(org.bukkit.Bukkit.class)) {
            bukkit.when(org.bukkit.Bukkit::getOnlinePlayers).thenReturn(List.of());
            lib.shutdown();
        }
        assertEquals(ServiceState.STOPPED, lib.getServiceRegistry().getStateByName("stop-me"));
    }

    @Test
    void languageFolderDefaultsToLanguagesInDataFolder() {
        java.io.File dataFolder = new java.io.File("TestOutputs/BuilderDefaults");
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        CoolStuffLib lib = new CoolStuffLibBuilder(plugin).withLanguageManager().build().createCoolStuffLib();
        assertEquals(new java.io.File(dataFolder, "languages"), lib.getLanguageManager().getLangFolder());
    }
}
