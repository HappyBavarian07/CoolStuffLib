package de.happybavarian07.coolstufflib.commandmanagement;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommandManagerTest {

    @Mock
    private JavaPlugin mockPlugin;

    @Mock
    private Player mockPlayer;

    @Mock
    private CommandSender mockSender;

    @Mock
    private CoolStuffLib mockCoolStuffLib;

    @Mock
    private LanguageManager mockLanguageManager;

    private TestCommandManager commandManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        try (MockedStatic<CoolStuffLib> mockedStatic = mockStatic(CoolStuffLib.class)) {
            mockedStatic.when(CoolStuffLib::getLib).thenReturn(mockCoolStuffLib);
            when(mockCoolStuffLib.getLanguageManager()).thenReturn(mockLanguageManager);

            commandManager = new TestCommandManager();
        }
    }

    @Test
    void testCommandBasicProperties() {
        assertEquals("testcommand", commandManager.getCommandName());
        assertEquals("/testcommand <subcommand>", commandManager.getCommandUsage());
        assertEquals("Test command for unit testing", commandManager.getCommandInfo());
        assertEquals("test.permission", commandManager.getCommandPermissionAsString());
        assertTrue(commandManager.autoRegisterPermission());
    }

    @Test
    void testCommandAliases() {
        List<String> aliases = commandManager.getCommandAliases();
        assertEquals(2, aliases.size());
        assertTrue(aliases.contains("tc"));
        assertTrue(aliases.contains("test"));
    }

    //@Test
    void testSubCommandRegistration() {
        TestSubCommand subCommand = new TestSubCommand();

        assertEquals(1, commandManager.getSubCommands().size());
        assertTrue(commandManager.getSubCommands().contains(subCommand));
    }

    //@Test
    void testSubCommandRetrieval() {
        TestSubCommand subCommand = new TestSubCommand();

        SubCommand retrieved = commandManager.getSubCommand("testsub");
        assertNotNull(retrieved);
        assertEquals(subCommand, retrieved);

        SubCommand notFound = commandManager.getSubCommand("nonexistent");
        assertNull(notFound);
    }

    //@Test
    void testPermissionHandling() {
        TestSubCommand subCommand = new TestSubCommand();

        when(mockPlayer.hasPermission("test.sub.permission")).thenReturn(true);
        assertTrue(commandManager.hasPermission(mockPlayer, subCommand));

        when(mockPlayer.hasPermission("test.sub.permission")).thenReturn(false);
        assertFalse(commandManager.hasPermission(mockPlayer, subCommand));
    }

    static class TestCommandManager extends CommandManager {
        @Override
        public String getCommandName() {
            return "testcommand";
        }

        @Override
        public String getCommandUsage() {
            return "/testcommand <subcommand>";
        }

        @Override
        public String getCommandInfo() {
            return "Test command for unit testing";
        }

        @Override
        public JavaPlugin getJavaPlugin() {
            return mock(JavaPlugin.class);
        }

        @Override
        public List<String> getCommandAliases() {
            return Arrays.asList("tc", "test");
        }

        @Override
        public String getCommandPermissionAsString() {
            return "test.permission";
        }

        @Override
        public boolean autoRegisterPermission() {
            return true;
        }

        @Override
        public void setup() {
            registerSubCommand(new TestSubCommand());
        }
    }

    static class TestSubCommand extends SubCommand {
        public TestSubCommand() {
            super("testcommand");
        }

        @Override
        public String name() {
            return "testsub";
        }

        @Override
        public String info() {
            return "Test subcommand";
        }

        @Override
        public String[] aliases() {
            return new String[0];
        }

        @Override
        public Map<Integer, String[]> subArgs(CommandSender sender, int isPlayer, String[] args) {
            return Map.of();
        }

        @Override
        public String syntax() {
            return "<arg>";
        }

        @Override
        public String permissionAsString() {
            return "test.sub.permission";
        }

        @Override
        public boolean autoRegisterPermission() {
            return true;
        }
    }
}
