package de.happybavarian07.coolstufflib.commandmanagement;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * <p>Command logic for builder-made commands. Return {@code false} to show the usage message.</p>
 */
@FunctionalInterface
public interface CommandHandler {
    boolean handle(CommandSender sender, CommandArgs args);

    /**
     * <p>Player-only command logic.</p>
     */
    @FunctionalInterface
    interface PlayerHandler {
        void handle(Player player, CommandArgs args);
    }
}
