package de.happybavarian07.coolstufflib.commandmanagement;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NestedCommandTest {
    private final List<String> calls = new ArrayList<>();
    private LanguageManager lgm;
    private CommandSender sender;
    private SubCommand player;
    private SubCommand info;
    private CommandManager manager;

    @BeforeEach
    void setUp() {
        lgm = mock(LanguageManager.class);
        when(lgm.getMessage(anyString(), any(), anyBoolean())).thenAnswer(invocation -> invocation.getArgument(0));
        when(lgm.replacePlaceholders(anyString(), anyMap())).thenAnswer(invocation -> invocation.getArgument(0));
        sender = mock(CommandSender.class);
        when(sender.hasPermission(any(Permission.class))).thenReturn(true);

        info = new SubCommandBuilder("admin", "info").aliases("i")
                .arguments(Argument.choice("target", "Steve", "Alex"))
                .executes((s, a) -> calls.add("info " + a.string(0))).build();
        SubCommand kick = new SubCommandBuilder("admin", "kick").executes((s, a) -> calls.add("kick")).build();
        player = new SubCommandBuilder("admin", "player").sub(info, kick).build();
        manager = new ArgumentTest.SingleSubManager(player);
        manager.setDependencies(mock(CoolStuffLib.class), lgm);
    }

    @Test
    void routesToNestedChildWithRemainingArguments() {
        manager.onCommand(sender, new String[]{"player", "I", "Steve"});
        assertEquals(List.of("info Steve"), calls);
    }

    @Test
    void pathSyntaxAndPermissionIncludeParents() {
        assertEquals("player info", info.path());
        assertEquals("/admin player info <Steve|Alex>", info.syntax());
        assertEquals("admin.player.info", info.permissionAsString());
        assertEquals("/admin player <info|kick>", player.syntax());
        assertSame(player, info.getParent());
    }

    @Test
    void parentWithoutHandlerShowsUsage() {
        manager.onCommand(sender, new String[]{"player"});
        assertTrue(calls.isEmpty());
        verify(sender).sendMessage("Player.Commands.UsageMessage");
    }

    @Test
    void allSubCommandsIncludeNestedChildrenAndBuiltInHelp() {
        List<String> names = manager.getAllSubCommands().stream().map(SubCommand::path).toList();
        assertEquals(List.of("player", "player info", "player kick", "help"), names);
    }

    @Test
    void tabCompletionWalksTheTree() {
        Command command = mock(Command.class);
        assertEquals(List.of("i", "info", "kick"), manager.onTabComplete(sender, command, "admin", new String[]{"player", ""}));
        assertEquals(List.of("Steve"), manager.onTabComplete(sender, command, "admin", new String[]{"player", "info", "St"}));
    }

    @Test
    void builderWithoutHandlerOrChildrenFails() {
        assertThrows(IllegalStateException.class, () -> new SubCommandBuilder("admin", "empty").build());
    }

    @Test
    void asyncCommandRunsOnScheduler() {
        SubCommand slow = new SubCommandBuilder("admin", "slow").async().executes((s, a) -> calls.add("slow")).build();
        CommandManager asyncManager = new ArgumentTest.SingleSubManager(slow);
        asyncManager.setDependencies(mock(CoolStuffLib.class), lgm);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            asyncManager.onCommand(sender, new String[]{"slow"});
        }

        assertTrue(calls.isEmpty());
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTaskAsynchronously(isNull(), task.capture());
        task.getValue().run();
        assertEquals(List.of("slow"), calls);
    }

    @Test
    void annotationEnablesAsync() {
        @SubCommandInfo(name = "report", async = true)
        class Report extends SubCommand {
            Report() {
                super("admin");
            }
        }
        assertTrue(new Report().isAsync());
    }
}
