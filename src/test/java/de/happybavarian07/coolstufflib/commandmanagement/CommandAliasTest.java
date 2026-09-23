package de.happybavarian07.coolstufflib.commandmanagement;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.menusystem.actions.MenuAction;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommandAliasTest {
    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<CoolStuffLib> statics;
    private CommandManagerRegistry registry;
    private ConsoleCommandSender console;

    @BeforeEach
    void setUp() {
        CoolStuffLib lib = mock(CoolStuffLib.class);
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(lib.getJavaPluginUsingLib()).thenReturn(plugin);
        when(plugin.getCommand(anyString())).thenReturn(mock(PluginCommand.class));
        LanguageManager lgm = mock(LanguageManager.class);
        when(lgm.getMessage(anyString(), any(), anyBoolean())).thenAnswer(invocation -> invocation.getArgument(0));
        when(lgm.getMessageOrDefault(anyString(), any(), isNull(), anyBoolean())).thenAnswer(invocation -> invocation.getArgument(0));
        console = mock(ConsoleCommandSender.class);

        statics = mockStatic(CoolStuffLib.class);
        statics.when(CoolStuffLib::getLib).thenReturn(lib);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
        bukkit.when(Bukkit::getConsoleSender).thenReturn(console);
        bukkit.when(() -> Bukkit.dispatchCommand(any(), anyString())).thenReturn(true);

        registry = new CommandManagerRegistry(plugin);
        registry.setLanguageManager(lgm);
        registry.setCommandManagerRegistryReady(true);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
        statics.close();
    }

    private static <T extends CommandSender> T allowed(Class<T> type) {
        T sender = mock(type);
        when(sender.hasPermission(any(Permission.class))).thenReturn(true);
        when(sender.getName()).thenReturn("Steve");
        return sender;
    }

    @Test
    void templateResolvesPlaceholders() {
        Player player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getName()).thenReturn("Steve");
        when(player.getUniqueId()).thenReturn(id);

        assertEquals("tp Steve Alex", new CommandTemplate("/tp {player} {0}").resolve(player, new String[]{"Alex"}));
        assertEquals("msg Alex hello there", new CommandTemplate("msg {0} {args:1}").resolve(player, new String[]{"Alex", "hello", "there"}));
        assertEquals("say", new CommandTemplate("say {args}").resolve(player, new String[0]));
        assertEquals("id " + id, new CommandTemplate("id {uuid}").resolve(player, new String[0]));
        assertEquals("$1 costs", new CommandTemplate("{0} costs").resolve(player, new String[]{"$1"}));
    }

    @Test
    void templateMetadata() {
        CommandTemplate template = new CommandTemplate("give {0} {1} {args:2}");
        assertEquals("give", template.commandName());
        assertEquals(2, template.requiredArgs());
        assertTrue(template.acceptsExtraArgs());
        assertEquals("<arg1> <arg2> [args...]", template.usage());
        assertThrows(IllegalArgumentException.class, () -> new CommandTemplate(" "));
    }

    @Test
    void missingPositionalArgumentIsAnArgumentError() {
        CommandArgumentException ex = assertThrows(CommandArgumentException.class,
                () -> new CommandTemplate("tp {0} {1}").resolve(null, new String[]{"a"}));
        assertEquals("#2", ex.getPlaceholders().get("%argument%"));
    }

    @Test
    void dispatchStopsRunawayAliasChains() {
        int[] calls = {0};
        bukkit.when(() -> Bukkit.dispatchCommand(any(), eq("loop"))).thenAnswer(invocation -> {
            calls[0]++;
            return CommandTemplate.dispatch(console, "loop");
        });
        assertFalse(CommandTemplate.dispatch(console, "loop"));
        assertEquals(CommandTemplate.MAX_DISPATCH_DEPTH, calls[0]);
    }

    @Test
    void aliasRunsTargetAsSender() {
        CommandManager gmc = registry.command("gmc").runs("gamemode creative {args}").register();
        CommandSender sender = allowed(CommandSender.class);

        gmc.onCommand(sender, new String[]{"Alex"});

        bukkit.verify(() -> Bukkit.dispatchCommand(sender, "gamemode creative Alex"));
        assertEquals("/gmc [args...]", gmc.getCommandUsage());
        assertEquals("Runs /gamemode creative {args}", gmc.getCommandInfo());
    }

    @Test
    void consoleAliasRunsAsConsoleButNeedsPermission() {
        assertThrows(IllegalStateException.class,
                () -> registry.command("gift").runsAsConsole("give {player} diamond").register());

        CommandManager gift = registry.command("gift").permission("shop.gift").runsAsConsole("give {player} diamond").register();
        gift.onCommand(allowed(CommandSender.class), new String[0]);

        bukkit.verify(() -> Bukkit.dispatchCommand(console, "give Steve diamond"));
    }

    @Test
    void aliasThatRunsItselfIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> registry.command("spawn").runs("spawn {args}").register());
        assertThrows(IllegalArgumentException.class, () -> registry.command("home").aliases("h").runs("/h").register());
    }

    @Test
    void failedTargetIsReported() {
        bukkit.when(() -> Bukkit.dispatchCommand(any(), eq("nope"))).thenReturn(false);
        CommandManager broken = registry.command("broken").runs("nope").register();
        CommandSender sender = allowed(CommandSender.class);

        broken.onCommand(sender, new String[0]);

        verify(sender).sendMessage("Player.Commands.AliasFailed");
    }

    @Test
    void positionalArgumentsSetLimits() {
        CommandManager tp = registry.command("tpto").runs("tp {0}").register();
        CommandSender sender = allowed(CommandSender.class);

        tp.onCommand(sender, new String[0]);
        tp.onCommand(sender, new String[]{"a", "b"});

        bukkit.verify(() -> Bukkit.dispatchCommand(any(), anyString()), never());
        verify(sender).sendMessage("Player.Commands.TooFewArguments");
        verify(sender).sendMessage("Player.Commands.TooManyArguments");
    }

    @Test
    void aliasesLoadFromConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("Aliases.gmc", "gamemode creative {args}");
        config.set("Aliases.heal.command", "effect give {0} instant_health");
        config.set("Aliases.heal.permission", "alias.heal");
        config.set("Aliases.heal.console", true);
        config.set("Aliases.heal.aliases", List.of("h"));
        config.set("Aliases.bad.console", true);
        config.set("Aliases.loop", "loop");

        List<String> registered = registry.loadAliases(config.getConfigurationSection("Aliases"));

        assertEquals(List.of("gmc", "heal"), registered);
        assertEquals("alias.heal", registry.getCommandManager("heal").getCommandPermissionAsString());
        assertEquals(List.of("h"), registry.getCommandManager("heal").getCommandAliases());
        assertNull(registry.getCommandManager("bad"));
        assertNull(registry.getCommandManager("loop"));
    }

    @Test
    void menuCommandActions() {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Steve");

        MenuAction.command("/spawn").execute(player, null);
        MenuAction.consoleCommand("give {player} diamond").execute(player, null);

        bukkit.verify(() -> Bukkit.dispatchCommand(player, "spawn"));
        bukkit.verify(() -> Bukkit.dispatchCommand(console, "give Steve diamond"));
    }
}
