package de.happybavarian07.coolstufflib.commandmanagement;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ArgumentTest {
    @SubCommandInfo(name = "give")
    static class Give extends SubCommand {
        Give() {
            super("admin");
        }

        @Override
        public List<Argument> arguments() {
            return List.of(Argument.player("target"), Argument.choice("item", "apple", "bread"),
                    Argument.integer("amount").optional());
        }
    }

    @SubCommandInfo(name = "say")
    static class Say extends SubCommand {
        Say() {
            super("admin");
        }

        @Override
        public List<Argument> arguments() {
            return List.of(Argument.text("message"));
        }
    }

    @SubCommandInfo(name = "limited")
    @CommandData(minArgs = 0, maxArgs = 5)
    static class Limited extends Give {
    }

    @Test
    void usageNotation() {
        assertEquals("<target>", Argument.player("target").usage());
        assertEquals("[amount]", Argument.integer("amount").optional().usage());
        assertEquals("<message...>", Argument.text("message").usage());
        assertTrue(Argument.integer("amount").required());
        assertFalse(Argument.integer("amount").optional().required());
    }

    @Test
    void builtInCompletions() {
        assertEquals(List.of("apple", "bread"), Argument.choice("item", "apple", "bread").complete(null));
        assertTrue(Argument.enumOf("mode", GameMode.class).complete(null).contains("creative"));
        assertTrue(Argument.integer("n").complete(null).isEmpty());
        assertEquals(List.of("x"), Argument.custom("c", sender -> List.of("x")).complete(null));
    }

    @Test
    void playerCompletionListsOnlinePlayers() {
        Player alex = mock(Player.class);
        when(alex.getName()).thenReturn("Alex");
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenAnswer(invocation -> List.of(alex));
            assertEquals(List.of("Alex"), Argument.player("target").complete(null));
        }
    }

    @Test
    void syntaxAndLimitsAreDerivedFromArguments() {
        Give give = new Give();
        assertEquals("/admin give <target> <item> [amount]", give.syntax());
        assertEquals(2, give.minArgs());
        assertEquals(3, give.maxArgs());

        Say say = new Say();
        assertEquals("/admin say <message...>", say.syntax());
        assertEquals(1, say.minArgs());
        assertEquals(Integer.MAX_VALUE, say.maxArgs());
    }

    @Test
    void commandDataOverridesDerivedLimits() {
        Limited limited = new Limited();
        assertEquals(0, limited.minArgs());
        assertEquals(5, limited.maxArgs());
    }

    @Test
    void subArgsSkipArgumentsWithoutCompletions() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenAnswer(invocation -> List.of());
            Map<Integer, String[]> map = new Give().subArgs(null, -1, new String[0]);
            assertFalse(map.containsKey(1));
            assertArrayEquals(new String[]{"apple", "bread"}, map.get(2));
            assertFalse(map.containsKey(3));
        }
    }

    @Test
    void tabCompletionUsesArgumentsAndFiltersByPrefix() {
        CommandManager manager = new SingleSubManager(new Give());
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission(any(Permission.class))).thenReturn(true);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenAnswer(invocation -> List.of());
            List<String> result = manager.onTabComplete(sender, mock(Command.class), "admin", new String[]{"give", "Steve", "a"});
            assertEquals(List.of("apple"), result);
        }
    }

    static class SingleSubManager extends CommandManager {
        SingleSubManager(SubCommand sub) {
            registerSubCommand(sub);
        }

        @Override
        public String getCommandName() {
            return "admin";
        }

        @Override
        public String getCommandUsage() {
            return "/admin";
        }

        @Override
        public String getCommandInfo() {
            return "";
        }

        @Override
        public JavaPlugin getJavaPlugin() {
            return null;
        }

        @Override
        public List<String> getCommandAliases() {
            return List.of();
        }

        @Override
        public String getCommandPermissionAsString() {
            return "admin";
        }

        @Override
        public boolean autoRegisterPermission() {
            return false;
        }

        @Override
        public void setup() {
        }
    }
}
