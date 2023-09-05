package de.happybavarian07.coolstufflib.commandmanagement;/*
 * @Author HappyBavarian07
 * @Date 09.11.2021 | 14:43
 */

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.languagemanager.Placeholder;
import de.happybavarian07.coolstufflib.languagemanager.PlaceholderType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * CommandManager class.
 */
@CommandData
public abstract class CommandManager {
    protected final ArrayList<SubCommand> commands = new ArrayList<>();
    protected final CoolStuffLib coolStuffLib = CoolStuffLib.getLib();
    protected final LanguageManager lgm = coolStuffLib.getLanguageManager();
    protected List<String> commandArgs = new ArrayList<>();
    protected List<String> commandSubArgs = new ArrayList<>();

    /**
     * Gets the name of the command.
     * 
     * Returns a String representing the name of the command.
     */
    public abstract String getCommandName();

    /**
     * Returns a String containing the usage instructions for the command. This String
     * should provide information on how to use the command, including any parameters
     * or flags that can be used.
     */
    public abstract String getCommandUsage();

    /**
     * Returns a String containing information about the command.
     */
    public abstract String getCommandInfo();

    /**
     * Provides access to the JavaPlugin associated with the CommandManager.
     * 
     * Returns the JavaPlugin associated with the CommandManager.
     */
    public abstract JavaPlugin getJavaPlugin();

    /**
     * Returns a list of aliases for the command. Aliases are alternate names for the
     * command that can be used to invoke it.
     */
    public abstract List<String> getCommandAliases();

    /**
     * Returns the permission associated with the command as a Permission object.
     */
    public abstract Permission getCommandPermissionAsPermission();

    /**
     * Returns the command permission as a String. The command permission is used to
     * determine if a user has permission to execute a command.
     */
    public abstract String getCommandPermissionAsString();

    /**
     * Provides a boolean value indicating whether permission should be automatically
     * registered when a command is registered.
     */
    public abstract boolean autoRegisterPermission();

    /**
     * Executes a subcommand based on the given arguments.
     *
     * @param sender The sender of the command.
     * @param args The arguments of the command.
     * @return Whether the command was successful.
     */
    public boolean onCommand(CommandSender sender, String[] args) {
        SubCommand target = this.getSub(args[0]);

        if (target == null) {
            sender.sendMessage(lgm.getMessage("Player.Commands.InvalidSubCommand", getPlayerForSender(sender), true));
            return true;
        }

        if (!hasPermission(sender, target)) {
            sender.sendMessage(format(lgm.getMessage("Player.General.NoPermissions", getPlayerForSender(sender), true), target));
            return true;
        }

        String[] updatedArgs = removeFirstArgument(args);

        if (target.isPlayerRequired() && !(sender instanceof Player)) {
            sender.sendMessage(lgm.getMessage("Console.ExecutesPlayerCommand", null, true));
            return true;
        }

        if (target.allowOnlySubCommandArgsThatFitToSubArgs()) {
            Map<Integer, String> invalidArgs = findInvalidArgs(updatedArgs, target);
            if (!invalidArgs.isEmpty()) {
                lgm.addPlaceholder(PlaceholderType.MESSAGE, "%invalidArgs%", invalidArgs.toString(), false);
                sender.sendMessage(format(lgm.getMessage("Player.Commands.CommandContainsInvalidArgs", getPlayerForSender(sender), true), target));
                return false;
            }
        }

        try {
            boolean callResult;
            if (sender instanceof Player) {
                callResult = target.onPlayerCommand((Player) sender, updatedArgs);
            } else {
                callResult = target.onConsoleCommand((ConsoleCommandSender) sender, updatedArgs);
            }
            if (!callResult) {
                sender.sendMessage(format(lgm.getMessage("Player.Commands.UsageMessage", getPlayerForSender(sender), true), target));
            }
        } catch (Exception e) {
            lgm.addPlaceholder(PlaceholderType.MESSAGE, "%error%", e + ": " + e.getMessage(), false);
            lgm.addPlaceholder(PlaceholderType.MESSAGE, "%stacktrace%", Arrays.toString(e.getStackTrace()), false);
            sender.sendMessage(format(lgm.getMessage("Player.Commands.ErrorPerformingSubCommand", getPlayerForSender(sender), true), target));
        }
        return true;
    }

    /**
     * Finds invalid arguments in a given array of arguments for a given subcommand.
     *
     * @param args the array of arguments to check
     * @param target the subcommand to check against
     * @return a map of invalid arguments, with the key being the index of the argument and the value being the argument itself
     */
    private Map<Integer, String> findInvalidArgs(String[] args, SubCommand target) {
        Map<Integer, String> invalidArgs = new HashMap<>();
        if(target.subArgs() == null || target.subArgs().isEmpty()) return invalidArgs;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (!Arrays.asList(target.subArgs().get(i)).contains(arg)) {
                invalidArgs.put(i, arg);
            }
        }
        return invalidArgs;
    }

    /**
     * Retrieves the {@link Player} object associated with the given {@link CommandSender}.
     *
     * @param sender the {@link CommandSender} to retrieve the {@link Player} object for
     * @return the {@link Player} object associated with the given {@link CommandSender}, or null if the sender is not a {@link Player}
     */
    private Player getPlayerForSender(CommandSender sender) {
        return (sender instanceof Player) ? (Player) sender : null;
    }

    /**
     * Removes the first argument from the given array of arguments.
     *
     * @param args the array of arguments
     * @return a new array of arguments with the first argument removed
     */
    private String[] removeFirstArgument(String[] args) {
        return Arrays.copyOfRange(args, 1, args.length);
    }

    /**
     * Checks if the given {@link CommandSender} has permission to execute the given {@link SubCommand}.
     * 
     * @param sender The {@link CommandSender} to check permission for.
     * @param target The {@link SubCommand} to check permission for.
     * @return {@code true} if the {@link CommandSender} has permission to execute the {@link SubCommand}, {@code false} otherwise.
     */
    public boolean hasPermission(CommandSender sender, SubCommand target) {
        return sender.hasPermission(target.permissionAsPermission()) || (target.isOpRequired() && sender.isOp());
    }

    /**
     * Handles a subcommand for the given sender.
     * 
     * @param sender The sender of the command.
     * @param target The subcommand to handle.
     * @param args The arguments for the subcommand.
     * @return Whether the command was successfully handled.
     */
    public boolean handleSubCommand(CommandSender sender, SubCommand target, String[] args) {
        if (sender instanceof Player) {
            return target.onPlayerCommand((Player) sender, args);
        }
        return target.onConsoleCommand((ConsoleCommandSender) sender, args);
    }

    /**
     * Provides tab completion for the command.
     *
     * @param sender The sender of the command
     * @param command The command being executed
     * @param label The label of the command
     * @param args The arguments of the command
     * @return A list of possible tab completions
     */
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> result = new ArrayList<>();
        List<String> subCommandOptions = new ArrayList<>();
        List<String> subCommandArgOptions = new ArrayList<>();

        for (SubCommand sub : this.getSubCommands()) {
            if (!sender.hasPermission(sub.permissionAsPermission()) && !(sub.isOpRequired() && sender.isOp())) {
                continue;
            }

            subCommandOptions.add(sub.name());
            subCommandOptions.addAll(Arrays.asList(sub.aliases()));

            if (args.length == 1) {
                for (String option : subCommandOptions) {
                    if (option.toLowerCase().startsWith(args[0].toLowerCase())) {
                        result.add(option);
                    }
                }
            } else if (args.length > 1) {
                if (sub.name().equals(args[0]) || Arrays.asList(sub.aliases()).contains(args[0])) {
                    Map<Integer, String[]> subArgs = sub.subArgs();
                    if (subArgs != null && subArgs.containsKey(args.length - 1)) {
                        subCommandArgOptions.addAll(Arrays.asList(subArgs.get(args.length - 1)));
                    }

                    for (String option : subCommandArgOptions) {
                        if (option.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
                            result.add(option);
                        }
                    }

                    return result;
                }
            }
        }

        return result;
    }

    /**
     * Provides a setup method for CommandManager class. This method is used to set up
     * the CommandManager class.
     */
    public abstract void setup();

    /**
     * Retrieves the list of subcommands associated with this CommandManager.
     * 
     * @return A list of SubCommand objects associated with this CommandManager.
     */
    public List<SubCommand> getSubCommands() {
        return commands;
    }

    /**
     * Gets a SubCommand from the list of SubCommands by its name or alias.
     * 
     * Parameters:
     * name - The name or alias of the SubCommand to get
     * 
     * Returns:
     * The SubCommand with the given name or alias, or null if none is found
     */
    protected SubCommand getSub(String name) {
        for (SubCommand sub : getSubCommands()) {
            if (sub.name().equalsIgnoreCase(name)) {
                return sub;
            }

            String[] aliases;
            int length = (aliases = sub.aliases()).length;

            for (int i = 0; i < length; i++) {
                String alias = aliases[i];
                if (name.equalsIgnoreCase(alias)) {
                    return sub;
                }
            }
        }
        return null;
    }

    /**
     * CommandManager.format() is a method used to format a string with placeholders.
     * It takes in a string and a SubCommand as parameters and returns a formatted
     * string. The method creates a HashMap of Placeholders and adds the following
     * Placeholders to it: "%usage%", "%description%", "%name%", "%permission%",
     * "%aliases%" and "%subArgs%". The Placeholders are then replaced in the string
     * and the formatted string is returned.
     */
    private String format(String in, SubCommand cmd) {
        Map<String, Placeholder> placeholders = new HashMap<>();
        placeholders.put("%usage%", new Placeholder("%usage%", cmd.syntax(), PlaceholderType.ALL));
        placeholders.put("%description%", new Placeholder("%description%", cmd.info(), PlaceholderType.ALL));
        placeholders.put("%name%", new Placeholder("%name%", cmd.name(), PlaceholderType.ALL));
        placeholders.put("%permission%", new Placeholder("%permission%", cmd.permissionAsPermission().getName(), PlaceholderType.ALL));
        placeholders.put("%aliases%", new Placeholder("%aliases%", cmd.aliases(), PlaceholderType.ALL));
        placeholders.put("%subArgs%", new Placeholder("%subArgs%", cmd.subArgs().toString(), PlaceholderType.ALL));

        return lgm.replacePlaceholders(in, placeholders);
    }
}
