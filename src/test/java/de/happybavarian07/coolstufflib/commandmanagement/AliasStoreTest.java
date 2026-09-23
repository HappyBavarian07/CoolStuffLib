package de.happybavarian07.coolstufflib.commandmanagement;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.jpa.RepositoryController;
import de.happybavarian07.coolstufflib.jpa.SQLExecutor;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AliasStoreTest {
    @TempDir
    Path tempDir;

    private static final CommandAlias FULL = new CommandAlias("heal", "effect give {0} instant_health", "alias.heal",
            true, "Heals a player", List.of("h", "hl"), true, 30, true);

    @Test
    void aliasValidation() {
        assertThrows(IllegalArgumentException.class, () -> CommandAlias.of("two words", "say hi"));
        assertThrows(IllegalArgumentException.class, () -> CommandAlias.of("x", " "));
        assertEquals(List.of(), new CommandAlias("x", "say", null, false, null, null, false, 0, false).aliases());
    }

    @Test
    void yamlStoreRoundTripInOwnSection() throws Exception {
        File dataFile = tempDir.resolve("data.yml").toFile();
        Files.writeString(dataFile.toPath(), "OtherFeature:\n  keep: true\n");
        YamlAliasStore store = new YamlAliasStore(dataFile);

        store.save(FULL);
        store.save(CommandAlias.of("gmc", "gamemode creative {args}"));

        YamlConfiguration reloaded = YamlConfiguration.loadConfiguration(dataFile);
        assertTrue(reloaded.getBoolean("OtherFeature.keep"));
        assertEquals("gamemode creative {args}", reloaded.getString("CommandAliases.gmc.command"));
        assertEquals(List.of(FULL, CommandAlias.of("gmc", "gamemode creative {args}")), new YamlAliasStore(dataFile).loadAll());

        store.delete("heal");
        assertFalse(YamlConfiguration.loadConfiguration(dataFile).contains("CommandAliases.heal"));
    }

    @Test
    void yamlStoreSharesTheCallersDataConfiguration() {
        File dataFile = tempDir.resolve("data.yml").toFile();
        YamlConfiguration shared = new YamlConfiguration();
        shared.set("Players.steve", "en");
        YamlAliasStore store = new YamlAliasStore(shared, dataFile, "Aliases");

        store.save(CommandAlias.of("gmc", "gamemode creative"));

        assertEquals("gamemode creative", shared.getString("Aliases.gmc.command"));
        assertEquals("en", YamlConfiguration.loadConfiguration(dataFile).getString("Players.steve"));
    }

    @Test
    void yamlStoreReadsShortFormAndSkipsBrokenEntries() throws Exception {
        File dataFile = tempDir.resolve("data.yml").toFile();
        Files.writeString(dataFile.toPath(), "CommandAliases:\n  gmc: gamemode creative\n  broken:\n    permission: x\n");
        List<CommandAlias> aliases;
        try (MockedStatic<CoolStuffLib> statics = mockStatic(CoolStuffLib.class)) {
            aliases = new YamlAliasStore(dataFile).loadAll();
        }
        assertEquals(List.of(CommandAlias.of("gmc", "gamemode creative")), aliases);
    }

    @Test
    void yamlStoreRefusesConfigYml() {
        assertThrows(IllegalArgumentException.class,
                () -> new YamlAliasStore(new YamlConfiguration(), tempDir.resolve("config.yml").toFile(), "Aliases"));
    }

    @Test
    void sqlStoreRoundTripInOwnTable() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            RepositoryController controller = mock(RepositoryController.class);
            when(controller.getConnection("default")).thenReturn(connection);
            SQLExecutor executor = new SQLExecutor(controller, null);
            executor.setDefaultConnection("default");
            SqlAliasStore store = new SqlAliasStore(executor, "ap_");

            assertEquals("ap_command_aliases", store.table());
            store.save(FULL);
            store.save(CommandAlias.of("gmc", "gamemode creative"));
            store.save(CommandAlias.of("gmc", "gamemode creative {args}"));

            List<CommandAlias> loaded = store.loadAll();
            assertEquals(2, loaded.size());
            assertTrue(loaded.contains(FULL));
            assertTrue(loaded.contains(CommandAlias.of("gmc", "gamemode creative {args}")));

            store.delete("heal");
            assertEquals(List.of(CommandAlias.of("gmc", "gamemode creative {args}")), store.loadAll());
        }
    }

    @Test
    void sqlStoreUsesTheControllersExecutor() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            RepositoryController controller = mock(RepositoryController.class);
            when(controller.getConnection("default")).thenReturn(connection);
            SQLExecutor executor = new SQLExecutor(controller, null);
            executor.setDefaultConnection("default");
            when(controller.getSqlExecutor()).thenReturn(executor);

            SqlAliasStore store = new SqlAliasStore(controller, "");
            store.save(CommandAlias.of("gmc", "gamemode creative"));

            assertEquals("command_aliases", store.table());
            assertEquals(List.of(CommandAlias.of("gmc", "gamemode creative")), store.loadAll());
        }
    }

    @Test
    void sqlStoreRejectsUnsafePrefix() {
        assertThrows(IllegalArgumentException.class, () -> new SqlAliasStore(mock(SQLExecutor.class), "x; DROP TABLE y"));
    }

    @Test
    void managerCreatesRestoresAndRemovesAliases() {
        File dataFile = tempDir.resolve("data.yml").toFile();
        CoolStuffLib lib = mock(CoolStuffLib.class);
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(lib.getJavaPluginUsingLib()).thenReturn(plugin);
        when(plugin.getCommand(anyString())).thenReturn(mock(PluginCommand.class));

        try (MockedStatic<CoolStuffLib> statics = mockStatic(CoolStuffLib.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            statics.when(CoolStuffLib::getLib).thenReturn(lib);
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));

            CommandManagerRegistry registry = new CommandManagerRegistry(plugin);
            registry.setLanguageManager(mock(LanguageManager.class));
            registry.setCommandManagerRegistryReady(true);
            CommandAliasManager manager = new CommandAliasManager(registry, new YamlAliasStore(dataFile));

            manager.create(CommandAlias.of("gmc", "gamemode creative {args}"));
            assertNotNull(registry.getCommandManager("gmc"));
            assertThrows(IllegalStateException.class, () -> manager.create(CommandAlias.of("GMC", "gamemode creative")));
            assertThrows(IllegalStateException.class,
                    () -> manager.create(new CommandAlias("gift", "give {player} diamond", null, true, null, List.of(), false, 0, false)));
            assertEquals(List.of(CommandAlias.of("gmc", "gamemode creative {args}")), new YamlAliasStore(dataFile).loadAll());

            CommandManagerRegistry restartedRegistry = new CommandManagerRegistry(plugin);
            restartedRegistry.setLanguageManager(mock(LanguageManager.class));
            restartedRegistry.setCommandManagerRegistryReady(true);
            CommandAliasManager restarted = new CommandAliasManager(restartedRegistry, new YamlAliasStore(dataFile));
            assertEquals(List.of("gmc"), restarted.loadAll());
            assertTrue(restarted.get("GMC").isPresent());

            assertTrue(restarted.remove("gmc"));
            assertFalse(restarted.remove("gmc"));
            assertNull(restartedRegistry.getCommandManager("gmc"));
            assertTrue(new YamlAliasStore(dataFile).loadAll().isEmpty());
        }
    }
}
