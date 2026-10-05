package de.happybavarian07.coolstufflib.commandmanagement;

import org.bukkit.command.CommandSender;

public record ArgumentCompletionContext(
        CommandSender sender,
        SubCommand command,
        String[] rawArguments,
        int argumentIndex,
        String currentInput
) {
    public ArgumentCompletionContext {
        rawArguments = rawArguments == null ? new String[0] : rawArguments.clone();
        currentInput = currentInput == null ? "" : currentInput;
    }
}
