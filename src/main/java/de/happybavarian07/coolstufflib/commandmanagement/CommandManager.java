package de.happybavarian07.coolstufflib.commandmanagement;/*
 * @Author HappyBavarian07
 * @Date 09.11.2021 | 14:43
 */

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.languagemanager.Placeholder;
import de.happybavarian07.coolstufflib.languagemanager.PlaceholderType;
import de.happybavarian07.coolstufflib.utils.CooldownTracker;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * The base class for creating command managers with subcommands.
 */
@CommandData
public abstract class CommandManager {
    private final ArrayList<SubCommand> commands = new ArrayList<>();
    private SubCommand rootCommand;
    protected CoolStuffLib coolStuffLib;
    protected LanguageManager lgm;
    protected List<String> commandArgs = new ArrayList<>();
    protected List<String> commandSubArgs = new ArrayList<>();

    /**
     * <p>Injects dependencies.</p>
     *
     * @param lib The library instance
     * @param lgm The language manager instance
     */
    public void setDependencies(CoolStuffLib lib, LanguageManager lgm) {
        this.coolStuffLib = lib;
        this.lgm = lgm;
    }

    /**
     * Gets the name of the main command.
     *
     * @return The name of the main command.
     */
    public abstract String getCommandName();

    /**
     * Gets the usage instructions for the main command.
     *
     * @return The usage instructions for the main command.
     */
    public abstract String getCommandUsage();

    /**
     * Gets a brief description of what the main command does.
     *
     * @return A brief description of the main command.
     */
    public abstract String getCommandInfo();

    /**
     * Gets the JavaPlugin associated with this command manager.
     *
     * @return The JavaPlugin associated with this command manager.
     */
    public abstract JavaPlugin getJavaPlugin();

    /**
     * Gets a list of aliases for the main command.
     *
     * @return A list of aliases for the main command.
     */
    public abstract List<String> getCommandAliases();

    /**
     * Gets the permission associated with the main command as a Permission object.
     *
     * @return The permission associated with the main command as a Permission object.
     */
    public Permission getCommandPermissionAsPermission() {
        return new Permission(getCommandPermissionAsString(), getCommandInfo());
    }

    /**
     * Gets the permission associated with the main command as a string.
     *
     * @return The permission associated with the main command as a string.
     */
    public abstract String getCommandPermissionAsString();

    /**
     * Determines whether permission should be automatically registered for the main command.
     *
     * @return True if permission should be automatically registered, false otherwise.
     */
    public abstract boolean autoRegisterPermission();

    /**
     * <p>Executes the command or subcommand.</p>
     *
     * @param sender The command sender
     * @param args   The command arguments
     * @return {@code true} if handled, {@code false} otherwise
     */
    public boolean onCommand(CommandSender sender, String[] args) {
        SubCommand target = args.length == 0 ? null : this.getSubCommand(args[0]);
        String[] updatedArgs;
        if (target != null) {
            updatedArgs = removeFirstArgument(args);
            while (updatedArgs.length > 0 && target.getChild(updatedArgs[0]) != null) {
                target = target.getChild(updatedArgs[0]);
                updatedArgs = removeFirstArgument(updatedArgs);
            }
        } else if (rootCommand != null) {
            target = rootCommand;
            updatedArgs = args;
        } else {
            sender.sendMessage(lgm.getMessage("Player.Commands.InvalidSubCommand", getPlayerForSender(sender), true));
            return true;
        }

        if (!hasPermission(sender, target)) {
            sender.sendMessage(format(lgm.getMessage("Player.General.NoPermissions", getPlayerForSender(sender), true), target));
            return true;
        }

        if (target.isPlayerRequired() && !(sender instanceof Player)) {
            sender.sendMessage(lgm.getMessage("Console.ExecutesPlayerCommand", null, true));
            return true;
        }
        if (updatedArgs.length < target.minArgs() || updatedArgs.length > target.maxArgs()) {
            String key = updatedArgs.length < target.minArgs() ? "Player.Commands.TooFewArguments" : "Player.Commands.TooManyArguments";
            sender.sendMessage(lgm.getMessage(key, getPlayerForSender(sender), true));
            sender.sendMessage(format(lgm.getMessage("Player.Commands.UsageMessage", getPlayerForSender(sender), true), target));
            return true;
        }
        if (target.allowOnlySubCommandArgsThatFitToSubArgs()) {
            Map<Integer, String> invalidArgs = findInvalidArgs(sender, updatedArgs, target, (sender instanceof Player) ? 1 : 0);
            if (!invalidArgs.isEmpty()) {
                lgm.addPlaceholder(PlaceholderType.MESSAGE, "%invalidArgs%", invalidArgs.toString(), false);
                sender.sendMessage(format(lgm.getMessage("Player.Commands.CommandContainsInvalidArgs", getPlayerForSender(sender), true), target));
                return false;
            }
        }

        if (!passesConfirmationAndCooldown(sender, target, updatedArgs)) {
            return true;
        }

        if (target.isAsync()) {
            SubCommand asyncTarget = target;
            String[] asyncArgs = updatedArgs;
            Bukkit.getScheduler().runTaskAsynchronously(getJavaPlugin(), () -> runSubCommand(sender, asyncTarget, asyncArgs));
        } else {
            runSubCommand(sender, target, updatedArgs);
        }
        return true;
    }

    private void runSubCommand(CommandSender sender, SubCommand target, String[] args) {
        try {
            if (!handleSubCommand(sender, target, args)) {
                sender.sendMessage(format(lgm.getMessage("Player.Commands.UsageMessage", getPlayerForSender(sender), true), target));
            }
        } catch (CommandArgumentException e) {
            sender.sendMessage(e.render(lgm, getPlayerForSender(sender)));
        } catch (Exception e) {
            lgm.addPlaceholder(PlaceholderType.MESSAGE, "%error%", e + ": " + e.getMessage(), false);
            lgm.addPlaceholder(PlaceholderType.MESSAGE, "%stacktrace%", Arrays.toString(e.getStackTrace()), false);
            sender.sendMessage(format(lgm.getMessage("Player.Commands.ErrorPerformingSubCommand", getPlayerForSender(sender), true), target));
            CoolStuffLib.logError("Error performing sub command '" + target.path() + "'", e);
        }
    }

    private boolean passesConfirmationAndCooldown(CommandSender sender, SubCommand target, String[] args) {
        Object key = SubCommand.senderKey(sender);
        Player player = getPlayerForSender(sender);
        if (target.requiresConfirmation() && !target.confirmations().confirm(key, String.join(" ", args))) {
            sender.sendMessage(CommandMessages.render(lgm, player, "Player.Commands.ConfirmCommand",
                    "%prefix% &9> &eRun the command again within %seconds%s to confirm.",
                    Map.of("%seconds%", String.valueOf(SubCommand.CONFIRMATION_WINDOW_MILLIS / 1000))));
            return false;
        }
        CooldownTracker cooldown = target.cooldowns();
        if (cooldown != null) {
            long remaining = cooldown.remainingMillis(key);
            if (remaining > 0 || !cooldown.tryUse(key)) {
                sender.sendMessage(CommandMessages.render(lgm, player, "Player.Commands.OnCooldown",
                        "%prefix% &9> &cPlease wait %seconds%s before using this again.",
                        Map.of("%seconds%", String.valueOf((remaining + 999) / 1000))));
                return false;
            }
        }
        return true;
    }

    /**
     * Finds invalid arguments in a given array of arguments for a given subcommand.
     *
     * @param args   The array of arguments to check.
     * @param target The subcommand to check against.
     * @return A map of invalid arguments, with the key being the index of the argument and the value being the argument itself.
     */
    private Map<Integer, String> findInvalidArgs(CommandSender sender, String[] args, SubCommand target, int isPlayer) {
        Map<Integer, String> invalidArgs = new HashMap<>();
        Map<Integer, String[]> subArgs = target.subArgs(sender, isPlayer, args);
        if (subArgs == null || subArgs.isEmpty()) return invalidArgs;
        for (int i = 0; i < args.length; i++) {
            String[] allowed = subArgs.get(i + 1);
            if (allowed != null && !Arrays.asList(allowed).contains(args[i])) {
                invalidArgs.put(i + 1, args[i]);
            }
        }
        return invalidArgs;
    }

    /**
     * Retrieves the {@link Player} object associated with the given {@link CommandSender}.
     *
     * @param sender The {@link CommandSender} to retrieve the {@link Player} object for.
     * @return The {@link Player} object associated with the given {@link CommandSender}, or null if the sender is not a {@link Player}.
     */
    private Player getPlayerForSender(CommandSender sender) {
        return (sender instanceof Player) ? (Player) sender : null;
    }

    /**
     * Removes the first argument from the given array of arguments.
     *
     * @param args The array of arguments.
     * @return A new array of arguments with the first argument removed.
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
        String permission = target.permissionAsString();
        if (permission == null || permission.isEmpty()) return true;
        return sender.hasPermission(target.permissionAsPermission()) || (target.isOpRequired() && sender.isOp());
    }

    /**
     * Handles a subcommand for the given sender.
     *
     * @param sender The sender of the command.
     * @param target The subcommand to handle.
     * @param args   The arguments for the subcommand.
     * @return Whether the command was successfully handled.
     */
    public boolean handleSubCommand(CommandSender sender, SubCommand target, String[] args) {
        if (sender instanceof Player player) {
            return target.onPlayerCommand(player, args);
        }
        if (sender instanceof ConsoleCommandSender console) {
            return target.onConsoleCommand(console, args);
        }
        return target.handleCommand(sender, null, args);
    }

    /**
     * Handles tab completion for the main command and its subcommands.
     *
     * @param sender  The {@link CommandSender} of the command.
     * @param command The main {@link Command}.
     * @param label   The label of the command.
     * @param args    The arguments provided for tab completion.
     * @return A list of possible tab completions.
     */
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return new ArrayList<>();
        Set<String> options = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (args.length == 1) {
            for (SubCommand sub : getSubCommands()) {
                if (!hasPermission(sender, sub)) continue;
                options.add(sub.name());
                options.addAll(Arrays.asList(sub.aliases()));
            }
        }

        SubCommand target = getSubCommand(args[0]);
        String[] targetArgs;
        if (target != null) {
            targetArgs = removeFirstArgument(args);
            while (targetArgs.length > 1 && target.getChild(targetArgs[0]) != null) {
                target = target.getChild(targetArgs[0]);
                targetArgs = removeFirstArgument(targetArgs);
            }
            if (targetArgs.length == 1) {
                for (SubCommand child : target.getChildren()) {
                    if (!hasPermission(sender, child)) continue;
                    options.add(child.name());
                    options.addAll(Arrays.asList(child.aliases()));
                }
            }
        } else {
            target = rootCommand;
            targetArgs = args;
        }
        if (target != null && targetArgs.length > 0 && hasPermission(sender, target)) {
            Map<Integer, String[]> subArgs = target.subArgs(sender, (sender instanceof Player) ? 1 : 0, targetArgs);
            if (subArgs != null && subArgs.containsKey(targetArgs.length)) {
                options.addAll(Arrays.asList(subArgs.get(targetArgs.length)));
            }
        }

        String current = args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(current)) result.add(option);
        }
        return result;
    }

    public CompletionStage<List<String>> onTabCompleteAsync(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return CompletableFuture.completedFuture(List.of());

        Set<String> options = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (args.length == 1) {
            for (SubCommand sub : getSubCommands()) {
                if (!hasPermission(sender, sub)) continue;
                options.add(sub.name());
                options.addAll(Arrays.asList(sub.aliases()));
            }
        }

        SubCommand target = getSubCommand(args[0]);
        String[] targetArgs;
        if (target != null) {
            targetArgs = removeFirstArgument(args);
            while (targetArgs.length > 1 && target.getChild(targetArgs[0]) != null) {
                target = target.getChild(targetArgs[0]);
                targetArgs = removeFirstArgument(targetArgs);
            }
            if (targetArgs.length == 1) {
                for (SubCommand child : target.getChildren()) {
                    if (!hasPermission(sender, child)) continue;
                    options.add(child.name());
                    options.addAll(Arrays.asList(child.aliases()));
                }
            }
        } else {
            target = rootCommand;
            targetArgs = args;
        }

        if (target == null || targetArgs.length == 0 || !hasPermission(sender, target)) {
            return CompletableFuture.completedFuture(filterCompletions(options, args[args.length - 1]));
        }

        Map<Integer, String[]> subArgs = target.subArgs(sender, sender instanceof Player ? 1 : 0, targetArgs);
        if (subArgs != null && subArgs.containsKey(targetArgs.length)) {
            options.addAll(Arrays.asList(subArgs.get(targetArgs.length)));
        }

        int argumentIndex = targetArgs.length - 1;
        List<Argument> arguments = target.arguments();
        if (argumentIndex >= 0 && argumentIndex < arguments.size()) {
            Argument argument = arguments.get(argumentIndex);
            if (argument.hasAsyncCompleter()) {
                Set<String> baseOptions = options;
                return argument.completeAsync(sender).thenApply(values -> {
                    baseOptions.addAll(values);
                    return filterCompletions(baseOptions, args[args.length - 1]);
                });
            }
        }
        return CompletableFuture.completedFuture(filterCompletions(options, args[args.length - 1]));
    }

    private List<String> filterCompletions(Set<String> options, String current) {
        String prefix = current.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }

    /**
     * Sets up the main command and registers all subcommands.
     * <p> This will be called by the CMR on registration. </p>
     * <p> This is where you should register all subcommands using {@link #registerSubCommand(SubCommand)}. </p>
     * <p> The built-in {@link HelpCommand} does not have to be registered here; it is added afterwards
     * unless this method already registered a sub command called {@code help}. </p>
     * <p> This method should be overridden by the implementing class. </p>
     */
    public abstract void setup();

    /**
     * Retrieves the list of subcommands associated with the main command.
     *
     * @return A list of {@link SubCommand} objects associated with the main command.
     */
    public List<SubCommand> getSubCommands() {
        return commands;
    }

    /**
     * <p>All sub commands including the root command, for registration and help output.</p>
     * <p>This is the first thing the {@link CommandManagerRegistry} calls after {@link #setup()}, so the
     * built-in {@link HelpCommand} is added here and picked up by the usual registration (dependencies,
     * pre-init, permissions, post-init) without the consumer doing anything.</p>
     */
    public List<SubCommand> getAllSubCommands() {
        registerBuiltInSubCommands();
        List<SubCommand> all = new ArrayList<>();
        for (SubCommand command : commands) collectWithChildren(command, all);
        if (rootCommand != null) all.add(rootCommand);
        return all;
    }

    /**
     * <p>Adds the built-in {@link HelpCommand} unless a sub command called {@code help} (or aliased to it)
     * is already registered. Because this runs after {@link #setup()}, a consumer that registers its own
     * help sub command there keeps it and is never duplicated.</p>
     */
    private void registerBuiltInSubCommands() {
        if (getSubCommand("help") == null) registerSubCommand(new HelpCommand(getCommandName()));
    }

    private static void collectWithChildren(SubCommand command, List<SubCommand> into) {
        into.add(command);
        for (SubCommand child : command.getChildren()) collectWithChildren(child, into);
    }

    /**
     * <p>Sets the command that runs for {@code /<command>} without arguments and for first arguments that
     * are not a sub command.</p>
     */
    protected void setRootCommand(SubCommand rootCommand) {
        this.rootCommand = rootCommand;
    }

    public SubCommand getRootCommand() {
        return rootCommand;
    }

    /**
     * <p>Starts a {@link SubCommandBuilder} for this command; register the result in {@link #setup()}.</p>
     */
    protected SubCommandBuilder subCommand(String name) {
        return new SubCommandBuilder(getCommandName(), name);
    }

    /**
     * Gets a specific subcommand by its name or alias.
     *
     * @param name The name or alias of the subcommand to retrieve.
     * @return The {@link SubCommand} with the given name or alias, or null if not found.
     */
    public SubCommand getSubCommand(String name) {
        for (SubCommand subCommand : getSubCommands()) {
            if (matches(subCommand, name)) {
                return subCommand;
            }
        }
        return null;
    }

    private static boolean matches(SubCommand subCommand, String name) {
        if (subCommand.name().equalsIgnoreCase(name)) return true;
        for (String alias : subCommand.aliases()) {
            if (alias.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    /**
     * Registers a subcommand with the main command.
     * <p> This method should be called in the {@link #setup()} method to register all subcommands.</p>
     * <p> If the subcommand is successfully registered, this method will return true.</p>
     * <p> If the subcommand is not successfully registered, this method will return false.</p>
     * <p> If the subcommand is already registered, this method will return false.</p>
     *
     * @param subCommand The subcommand to register.
     * @return True if the subcommand was successfully registered, false otherwise.
     */
    protected boolean registerSubCommand(SubCommand subCommand) {
        return commands.add(subCommand);
    }

    /**
     * <p>Formats the help message for a subcommand, replacing placeholders with their respective values.</p>
     * <p>Available placeholders:</p>
     * <ul>
     *     <li>%usage% - Replaced with the usage of this command</li>
     *     <li>%description% - Replaced with a description of this command</li>
     *     <li>%name% - Replaced with the name of this command</li>
     *     <li>%permission% - Replaced with the permission required for this command</li>
     *     <li>%aliases% - Replaced with the command's aliases</li>
     *     <li>%subArgs% - Replaced with the command's sub-arguments</li>
     * </ul>
     *
     * @param in  The message to format.
     * @param cmd The {@link SubCommand} associated with the message.
     * @return The string with the placeholders replaced.
     */
    private String format(String in, SubCommand cmd) {
        Map<String, Placeholder> placeholders = new HashMap<>();
        placeholders.put("%usage%", new Placeholder("%usage%", cmd.syntax(), PlaceholderType.ALL));
        placeholders.put("%description%", new Placeholder("%description%", cmd.info(), PlaceholderType.ALL));
        placeholders.put("%name%", new Placeholder("%name%", cmd.name(), PlaceholderType.ALL));
        placeholders.put("%permission%", new Placeholder("%permission%", cmd.permissionAsPermission().getName(), PlaceholderType.ALL));
        placeholders.put("%aliases%", new Placeholder("%aliases%", cmd.aliases(), PlaceholderType.ALL));
        placeholders.put("%subArgs%", new Placeholder("%subArgs%", cmd.subArgs(null, -1, new String[0]).toString(), PlaceholderType.ALL));

        return lgm.replacePlaceholders(in, placeholders);
    }
}
