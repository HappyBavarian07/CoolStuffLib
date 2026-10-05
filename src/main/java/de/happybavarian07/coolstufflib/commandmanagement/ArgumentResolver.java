package de.happybavarian07.coolstufflib.commandmanagement;

import org.bukkit.command.CommandSender;

import java.util.Collection;

@FunctionalInterface
public interface ArgumentResolver<T> {
    Collection<ArgumentOption<T>> resolve(CommandSender sender);
}
