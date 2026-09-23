package de.happybavarian07.coolstufflib.commandmanagement;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommandArgsTest {
    private static CommandArgs args(String... raw) {
        return new CommandArgs(mock(CommandSender.class), raw, List.of(Argument.player("target"), Argument.integer("amount")));
    }

    private static CommandArgumentException failure(Runnable call) {
        return assertThrows(CommandArgumentException.class, call::run);
    }

    @Test
    void stringsAndDefaults() {
        CommandArgs args = args("Steve", "hello", "world");
        assertEquals(3, args.size());
        assertEquals("Steve", args.string(0));
        assertEquals("fallback", args.string(5, "fallback"));
        assertEquals("hello world", args.joined(1));
        assertTrue(args.has(2));
        assertFalse(args.has(3));
    }

    @Test
    void missingArgumentNamesTheDeclaredArgument() {
        CommandArgumentException ex = failure(() -> args("Steve").integer(1));
        assertEquals("Player.Commands.MissingArgument", ex.getMessagePath());
        assertEquals("amount", ex.getPlaceholders().get("%argument%"));
    }

    @Test
    void undeclaredMissingArgumentUsesItsPosition() {
        CommandArgumentException ex = failure(() -> args().string(4));
        assertEquals("#5", ex.getPlaceholders().get("%argument%"));
    }

    @Test
    void integers() {
        assertEquals(12, args("x", "12").integer(1));
        assertEquals(7, args("x").integer(1, 7));
        assertEquals(64, args("x", "64").integer(1, 1, 64));
        assertEquals(3, args("x").integer(1, 3, 1, 64));

        assertEquals("Player.Commands.NotANumber", failure(() -> args("x", "abc").integer(1)).getMessagePath());
        CommandArgumentException range = failure(() -> args("x", "65").integer(1, 1, 64));
        assertEquals("Player.Commands.NumberOutOfRange", range.getMessagePath());
        assertEquals("64", range.getPlaceholders().get("%max%"));
    }

    @Test
    void decimalsAndBooleans() {
        assertEquals(2.5, args("2.5").decimal(0));
        assertEquals(1.0, args().decimal(0, 1.0));
        assertTrue(args("YES").bool(0));
        assertFalse(args("off").bool(0));
        assertEquals("Player.Commands.InvalidChoice", failure(() -> args("maybe").bool(0)).getMessagePath());
    }

    @Test
    void enumLookupIsCaseInsensitiveAndListsOptions() {
        assertEquals(GameMode.CREATIVE, args("creative").enumOf(0, GameMode.class));
        CommandArgumentException ex = failure(() -> args("flying").enumOf(0, GameMode.class));
        assertTrue(ex.getPlaceholders().get("%options%").contains("survival"));
    }

    @Test
    void onlinePlayerAndWorld() {
        Player steve = mock(Player.class);
        World nether = mock(World.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Steve")).thenReturn(steve);
            bukkit.when(() -> Bukkit.getWorld("nether")).thenReturn(nether);
            assertSame(steve, args("Steve").onlinePlayer(0));
            assertSame(nether, args("nether").world(0));
            assertEquals("Player.Commands.PlayerNotFound", failure(() -> args("Alex").onlinePlayer(0)).getMessagePath());
            assertEquals("Player.Commands.WorldNotFound", failure(() -> args("end").world(0)).getMessagePath());
        }
    }

    @Test
    void playerRequiresAPlayerSender() {
        Player player = mock(Player.class);
        assertSame(player, new CommandArgs(player, new String[0], null).player());
        CommandArgs console = new CommandArgs(mock(ConsoleCommandSender.class), new String[0], null);
        assertEquals("Console.ExecutesPlayerCommand", failure(console::player).getMessagePath());
    }

    @Test
    void renderUsesLanguageFileWhenKeyExists() {
        LanguageManager lgm = mock(LanguageManager.class);
        when(lgm.getMessageOrDefault(eq("Player.Commands.NotANumber"), any(), isNull(), eq(true))).thenReturn("localized");
        CommandArgumentException ex = failure(() -> args("abc").integer(0));
        assertEquals("localized", ex.render(lgm, null));
        verify(lgm).removePlaceholder(any(), eq("%value%"));
    }

    @Test
    void renderFallsBackToEnglishWithPlaceholders() {
        LanguageManager lgm = mock(LanguageManager.class);
        when(lgm.getPrefix()).thenReturn("[P]");
        CommandArgumentException ex = failure(() -> args("x", "99").integer(1, 1, 64));
        String rendered;
        try (MockedStatic<CoolStuffLib> statics = mockStatic(CoolStuffLib.class)) {
            statics.when(CoolStuffLib::getLib).thenThrow(new RuntimeException("not initialized"));
            rendered = ex.render(lgm, null);
        }
        assertTrue(rendered.startsWith("[P]"), rendered);
        assertTrue(rendered.contains("99 must be between 1 and 64"), rendered);
    }

    @Test
    void managerSendsArgumentErrorInsteadOfGenericError() {
        @SubCommandInfo(name = "give")
        @CommandData
        class Give extends SubCommand {
            Give() {
                super("admin");
            }

            @Override
            public boolean execute(CommandSender sender, CommandArgs args) {
                args.integer(0);
                return true;
            }
        }
        LanguageManager lgm = mock(LanguageManager.class);
        when(lgm.getMessageOrDefault(eq("Player.Commands.NotANumber"), any(), isNull(), eq(true))).thenReturn("not a number");
        ArgumentTest.SingleSubManager manager = new ArgumentTest.SingleSubManager(new Give());
        manager.setDependencies(mock(CoolStuffLib.class), lgm);
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission(any(Permission.class))).thenReturn(true);

        manager.onCommand(sender, new String[]{"give", "abc"});

        verify(sender).sendMessage("not a number");
        verify(lgm, never()).getMessage(eq("Player.Commands.ErrorPerformingSubCommand"), any(), anyBoolean());
    }
}
