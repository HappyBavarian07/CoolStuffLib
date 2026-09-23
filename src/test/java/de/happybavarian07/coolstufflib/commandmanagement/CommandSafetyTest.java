package de.happybavarian07.coolstufflib.commandmanagement;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommandSafetyTest {
    private LanguageManager lgm;
    private final List<String> calls = new ArrayList<>();

    @BeforeEach
    void setUp() {
        lgm = mock(LanguageManager.class);
        when(lgm.getMessageOrDefault(anyString(), any(), isNull(), anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CommandManager manager(SubCommand sub) {
        CommandManager manager = new ArgumentTest.SingleSubManager(sub);
        manager.setDependencies(mock(CoolStuffLib.class), lgm);
        return manager;
    }

    private static Player player() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.hasPermission(any(Permission.class))).thenReturn(true);
        return player;
    }

    @Test
    void confirmationTrackerNeedsSameSignatureWithinWindow() {
        AtomicLong now = new AtomicLong();
        ConfirmationTracker tracker = new ConfirmationTracker(1000, now::get);
        assertFalse(tracker.confirm("p", "reset all"));
        assertFalse(tracker.confirm("p", "reset other"));
        assertTrue(tracker.confirm("p", "reset other"));
        assertFalse(tracker.confirm("p", "reset all"));
        now.addAndGet(1001);
        assertFalse(tracker.confirm("p", "reset all"));
    }

    @Test
    void confirmedCommandRunsOnSecondCall() {
        CommandManager manager = manager(new SubCommandBuilder("admin", "reset").confirm()
                .executes((s, a) -> calls.add("reset")).build());
        Player player = player();

        manager.onCommand(player, new String[]{"reset"});
        assertTrue(calls.isEmpty());
        verify(player).sendMessage("Player.Commands.ConfirmCommand");

        manager.onCommand(player, new String[]{"reset"});
        assertEquals(List.of("reset"), calls);
    }

    @Test
    void cooldownBlocksRepeatedUsePerSender() {
        CommandManager manager = manager(new SubCommandBuilder("admin", "heal").cooldown(Duration.ofMinutes(1))
                .executes((s, a) -> calls.add("heal")).build());
        Player first = player();
        Player second = player();

        manager.onCommand(first, new String[]{"heal"});
        manager.onCommand(first, new String[]{"heal"});
        manager.onCommand(second, new String[]{"heal"});

        assertEquals(List.of("heal", "heal"), calls);
        verify(first).sendMessage("Player.Commands.OnCooldown");
    }

    @Test
    void annotationConfiguresCooldownAndConfirmation() {
        @SubCommandInfo(name = "wipe", cooldownMillis = 5000, confirm = true)
        class Wipe extends SubCommand {
            Wipe() {
                super("admin");
            }
        }
        Wipe wipe = new Wipe();
        assertEquals(5000, wipe.cooldownMillis());
        assertTrue(wipe.requiresConfirmation());
    }

    @Test
    void consoleIsKeyedByName() {
        CommandSender console = mock(CommandSender.class);
        when(console.getName()).thenReturn("CONSOLE");
        assertEquals("CONSOLE", SubCommand.senderKey(console));
    }
}
