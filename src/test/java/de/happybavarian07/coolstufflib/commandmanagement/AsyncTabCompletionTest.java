package de.happybavarian07.coolstufflib.commandmanagement;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AsyncTabCompletionTest {
    @Test
    void asyncArgumentCompletionsAreFilteredByPrefix() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission(anyString())).thenReturn(true);
        when(sender.hasPermission(any(org.bukkit.permissions.Permission.class))).thenReturn(true);
        CommandManager manager = new ArgumentTest.SingleSubManager(
                new SubCommandBuilder("admin", "profile")
                        .arguments(Argument.asyncChoice("profile",
                                ignored -> CompletableFuture.completedFuture(List.of("Alice", "Bob"))))
                        .executes((s, a) -> true)
                        .build());

        List<String> result = manager.onTabCompleteAsync(sender, mock(Command.class), "admin",
                        new String[]{"profile", "a"})
                .toCompletableFuture().join();

        assertEquals(List.of("Alice"), result);
    }
}
