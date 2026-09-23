package de.happybavarian07.coolstufflib.commandmanagement;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommandBuilderTest {
    private CoolStuffLib lib;
    private LanguageManager lgm;
    private JavaPlugin plugin;
    private CommandManagerRegistry registry;

    @BeforeEach
    void setUp() {
        lib = mock(CoolStuffLib.class);
        lgm = mock(LanguageManager.class);
        plugin = mock(JavaPlugin.class);
        when(lib.getJavaPluginUsingLib()).thenReturn(plugin);
        when(lgm.getMessage(anyString(), any(), anyBoolean())).thenAnswer(invocation -> invocation.getArgument(0));
        when(lgm.replacePlaceholders(anyString(), anyMap())).thenAnswer(invocation -> invocation.getArgument(0));
        when(plugin.getCommand(anyString())).thenReturn(mock(PluginCommand.class));
        registry = new CommandManagerRegistry(plugin);
        registry.setLanguageManager(lgm);
        registry.setCommandManagerRegistryReady(true);
    }

    private <T> T withServer(Supplier<T> action) {
        try (MockedStatic<CoolStuffLib> statics = mockStatic(CoolStuffLib.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            statics.when(CoolStuffLib::getLib).thenReturn(lib);
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            bukkit.when(Bukkit::getOnlinePlayers).thenAnswer(invocation -> List.of());
            return action.get();
        }
    }

    private static <T extends CommandSender> T sender(Class<T> type) {
        T sender = mock(type);
        when(sender.hasPermission(any(Permission.class))).thenReturn(true);
        return sender;
    }

    private static Command command(String name) {
        Command command = mock(Command.class);
        when(command.getName()).thenReturn(name);
        return command;
    }

    @Test
    void subCommandBuilderNeedsAHandler() {
        assertThrows(IllegalStateException.class, () -> new SubCommandBuilder("main", "x").build());
    }

    @Test
    void subCommandBuilderMetadata() {
        SubCommand sub = new SubCommandBuilder("Main", "give")
                .aliases("g")
                .info("Gives items")
                .arguments(Argument.player("target"), Argument.integer("amount").optional())
                .executes((sender, args) -> true)
                .build();

        assertEquals("give", sub.name());
        assertArrayEquals(new String[]{"g"}, sub.aliases());
        assertEquals("Gives items", sub.info());
        assertEquals("/Main give <target> [amount]", sub.syntax());
        assertEquals("main.give", sub.permissionAsString());
        assertEquals(1, sub.minArgs());
        assertEquals(2, sub.maxArgs());
        assertFalse(sub.isPlayerRequired());
    }

    @Test
    void rootCommandRunsWithAndWithoutArguments() {
        List<String> calls = new ArrayList<>();
        withServer(() -> registry.command("heal")
                .arguments(Argument.player("target").optional())
                .executes((sender, args) -> calls.add("root:" + args.string(0, "self")))
                .register());
        CommandSender sender = sender(CommandSender.class);

        withServer(() -> registry.onCommand(sender, command("heal"), "heal", new String[0]));
        withServer(() -> registry.onCommand(sender, command("heal"), "heal", new String[]{"Steve"}));

        assertEquals(List.of("root:self", "root:Steve"), calls);
    }

    @Test
    void subCommandsTakePrecedenceOverRoot() {
        List<String> calls = new ArrayList<>();
        SubCommand all = new SubCommandBuilder("heal", "all").executes((sender, args) -> calls.add("all")).build();
        CommandManager manager = withServer(() -> registry.command("heal")
                .executes((sender, args) -> calls.add("root"))
                .sub(all)
                .register());

        withServer(() -> manager.onCommand(sender(CommandSender.class), new String[]{"all"}));

        assertEquals(List.of("all"), calls);
        assertEquals("/heal [all]", manager.getCommandUsage());
        assertEquals("heal", manager.getCommandPermissionAsString());
    }

    @Test
    void playerOnlyRootRejectsConsole() {
        List<String> calls = new ArrayList<>();
        CommandManager manager = withServer(() -> registry.command("fly")
                .player((player, args) -> calls.add(player.getName()))
                .register());
        ConsoleCommandSender console = sender(ConsoleCommandSender.class);

        withServer(() -> manager.onCommand(console, new String[0]));

        assertTrue(calls.isEmpty());
        verify(console).sendMessage("Console.ExecutesPlayerCommand");
    }

    @Test
    void tabCompletionMergesSubCommandsAndRootArguments() {
        SubCommand all = new SubCommandBuilder("mode", "all").executes((sender, args) -> true).build();
        CommandManager manager = withServer(() -> registry.command("mode")
                .arguments(Argument.choice("mode", "adventure", "survival"))
                .executes((sender, args) -> true)
                .sub(all)
                .register());

        List<String> result = withServer(() -> manager.onTabComplete(sender(CommandSender.class), command("mode"), "mode", new String[]{"a"}));

        assertEquals(List.of("adventure", "all"), result);
    }

    @Test
    void rootCommandGetsDependenciesAndDuplicateNamesAreRejected() {
        withServer(() -> registry.command("once").executes((sender, args) -> true).register());
        CommandManager registered = registry.getCommandManager("once");
        assertNotNull(registered.getRootCommand());
        assertSame(lgm, registered.getRootCommand().lgm);

        assertThrows(IllegalStateException.class,
                () -> withServer(() -> registry.command("once").executes((sender, args) -> true).register()));
    }

    @Test
    void managerSubCommandHelperUsesCommandName() {
        CommandManager manager = new ArgumentTest.SingleSubManager(new SubCommandBuilder("admin", "x").executes((s, a) -> true).build()) {
            @Override
            public void setup() {
                registerSubCommand(subCommand("ping").executes((s, a) -> true).build());
            }
        };
        manager.setup();
        assertEquals("/admin ping", manager.getSubCommand("ping").syntax());
    }

    @Test
    void builderExposesAllBehaviourFlags() {
        SubCommand sub = new SubCommandBuilder("main", "strict")
                .opRequired()
                .strictArguments()
                .senderTypeSpecificSubArgs()
                .minArgs(0)
                .maxArgs(4)
                .arguments(Argument.choice("mode", "a", "b"))
                .executes((sender, args) -> true)
                .build();

        assertTrue(sub.isOpRequired());
        assertTrue(sub.allowOnlySubCommandArgsThatFitToSubArgs());
        assertTrue(sub.senderTypeSpecificSubArgs());
        assertEquals(0, sub.minArgs());
        assertEquals(4, sub.maxArgs());
    }

    @Test
    void opRequiredLetsOpsRunWithoutPermission() {
        List<String> calls = new ArrayList<>();
        SubCommand sub = new SubCommandBuilder("admin", "op").opRequired().executes((s, a) -> calls.add("ran")).build();
        CommandManager manager = new ArgumentTest.SingleSubManager(sub);
        manager.setDependencies(lib, lgm);
        CommandSender op = mock(CommandSender.class);
        when(op.isOp()).thenReturn(true);

        manager.onCommand(op, new String[]{"op"});

        assertEquals(List.of("ran"), calls);
    }

    @Test
    void strictArgumentsRejectUnknownValues() {
        List<String> calls = new ArrayList<>();
        SubCommand sub = new SubCommandBuilder("admin", "mode").strictArguments()
                .arguments(Argument.choice("mode", "a", "b"))
                .executes((s, a) -> calls.add(a.string(0))).build();
        CommandManager manager = new ArgumentTest.SingleSubManager(sub);
        manager.setDependencies(lib, lgm);
        CommandSender sender = sender(CommandSender.class);

        manager.onCommand(sender, new String[]{"mode", "c"});
        manager.onCommand(sender, new String[]{"mode", "b"});

        assertEquals(List.of("b"), calls);
    }
}
