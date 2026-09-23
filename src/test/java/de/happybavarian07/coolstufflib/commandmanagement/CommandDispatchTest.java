package de.happybavarian07.coolstufflib.commandmanagement;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommandDispatchTest {
    private CoolStuffLib lib;
    private LanguageManager lgm;
    private JavaPlugin plugin;

    @BeforeEach
    void setUp() {
        lib = mock(CoolStuffLib.class);
        lgm = mock(LanguageManager.class);
        plugin = mock(JavaPlugin.class);
        when(lib.getJavaPluginUsingLib()).thenReturn(plugin);
        when(lgm.getMessage(anyString(), any(), anyBoolean())).thenAnswer(invocation -> invocation.getArgument(0));
        when(lgm.replacePlaceholders(anyString(), anyMap())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registerInjectsDependenciesIntoSubCommandsAddedInSetup() {
        RecordingSub sub = new RecordingSub();
        Manager manager = new Manager(sub);
        CommandManagerRegistry registry = new CommandManagerRegistry(plugin);
        registry.setLanguageManager(lgm);
        registry.setCommandManagerRegistryReady(true);
        when(plugin.getCommand("main")).thenReturn(mock(PluginCommand.class));

        try (MockedStatic<CoolStuffLib> statics = mockStatic(CoolStuffLib.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            statics.when(CoolStuffLib::getLib).thenReturn(lib);
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            assertTrue(registry.register(manager));
        }

        assertSame(lib, sub.lib);
        assertSame(lgm, sub.lgm);
        assertSame(registry, sub.registry);
        assertTrue(sub.preInitCalled);
    }

    @Test
    void strictArgumentCheckValidatesFirstArgument() {
        StrictSub sub = new StrictSub();
        Manager manager = ready(sub);
        CommandSender sender = allowedSender(CommandSender.class);

        manager.onCommand(sender, new String[]{"sub", "a"});
        manager.onCommand(sender, new String[]{"sub", "x"});

        assertEquals(List.of("a"), sub.calls);
        verify(sender).sendMessage("Player.Commands.CommandContainsInvalidArgs");
    }

    @Test
    void nonPlayerNonConsoleSenderIsHandled() {
        RecordingSub sub = new RecordingSub();
        Manager manager = ready(sub);
        BlockCommandSender block = allowedSender(BlockCommandSender.class);

        assertDoesNotThrow(() -> manager.onCommand(block, new String[]{"sub", "go"}));
        assertEquals(List.of("go"), sub.calls);
    }

    @Test
    void aliasesMatchCaseInsensitively() {
        Manager manager = ready(new RecordingSub());
        assertNotNull(manager.getSubCommand("ALIAS"));
        assertNotNull(manager.getSubCommand("SUB"));
    }

    @Test
    void subCommandMinArgsAreEnforced() {
        MinArgsSub sub = new MinArgsSub();
        Manager manager = ready(sub);
        Player player = allowedSender(Player.class);

        manager.onCommand(player, new String[]{"sub"});

        assertTrue(sub.calls.isEmpty());
        verify(player).sendMessage("Player.Commands.TooFewArguments");
    }

    @Test
    void emptyArgumentsReportInvalidSubCommand() {
        Manager manager = ready(new RecordingSub());
        CommandSender sender = allowedSender(CommandSender.class);

        assertDoesNotThrow(() -> manager.onCommand(sender, new String[0]));
        verify(sender).sendMessage("Player.Commands.InvalidSubCommand");
    }

    private Manager ready(SubCommand sub) {
        Manager manager = new Manager(sub);
        manager.setup();
        manager.setDependencies(lib, lgm);
        sub.setDependencies(lib, lgm, mock(CommandManagerRegistry.class));
        return manager;
    }

    private static <T extends CommandSender> T allowedSender(Class<T> type) {
        T sender = mock(type);
        when(sender.hasPermission(any(Permission.class))).thenReturn(true);
        return sender;
    }

    static class Manager extends CommandManager {
        private final SubCommand sub;

        Manager(SubCommand sub) {
            this.sub = sub;
        }

        @Override
        public String getCommandName() {
            return "main";
        }

        @Override
        public String getCommandUsage() {
            return "/main";
        }

        @Override
        public String getCommandInfo() {
            return "Main";
        }

        @Override
        public JavaPlugin getJavaPlugin() {
            return CoolStuffLib.getLib().getJavaPluginUsingLib();
        }

        @Override
        public List<String> getCommandAliases() {
            return List.of();
        }

        @Override
        public String getCommandPermissionAsString() {
            return "main.use";
        }

        @Override
        public boolean autoRegisterPermission() {
            return false;
        }

        @Override
        public void setup() {
            if (!getSubCommands().contains(sub)) registerSubCommand(sub);
        }
    }

    @CommandData
    static class RecordingSub extends SubCommand {
        final List<String> calls = new ArrayList<>();
        boolean preInitCalled;

        RecordingSub() {
            super("main");
        }

        @Override
        public void preInit() {
            preInitCalled = true;
        }

        @Override
        public boolean handleCommand(CommandSender sender, Player playerOrNull, String[] args) {
            calls.add(String.join(" ", args));
            return true;
        }

        @Override
        public String name() {
            return "sub";
        }

        @Override
        public String info() {
            return "";
        }

        @Override
        public String[] aliases() {
            return new String[]{"alias"};
        }

        @Override
        public Map<Integer, String[]> subArgs(CommandSender sender, int isPlayer, String[] args) {
            return Map.of(1, new String[]{"a", "b"});
        }

        @Override
        public String syntax() {
            return "/main sub";
        }

        @Override
        public String permissionAsString() {
            return "main.sub";
        }

        @Override
        public boolean autoRegisterPermission() {
            return false;
        }
    }

    @CommandData(allowOnlySubCommandArgsThatFitToSubArgs = true)
    static class StrictSub extends RecordingSub {
    }

    @CommandData(minArgs = 1)
    static class MinArgsSub extends RecordingSub {
    }
}
