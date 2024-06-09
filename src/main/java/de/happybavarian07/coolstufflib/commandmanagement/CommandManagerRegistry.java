package de.happybavarian07.coolstufflib.commandmanagement;/*
 * @Author HappyBavarian07
 * @Date 09.11.2021 | 14:52
 */

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.utils.LogPrefix;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;

/**
 * CommandManagerRegistry class.
 */
public class CommandManagerRegistry implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final Map<CommandManager, CommandData> commandManagers;
    private LanguageManager lgm;
    private boolean commandManagerRegistryReady = false;

    /**
     * Provides a constructor for the CommandManagerRegistry class, initializing the
     * plugin and commandManagers fields. The commandManagers field is a HashMap that
     * stores CommandManager objects.
     *
     * @param plugin The JavaPlugin instance to associate with this registry.
     */
    public CommandManagerRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.commandManagers = new HashMap<>();
    }

    /**
     * Retrieves the value of a private field from an object.
     *
     * @param object The object from which to retrieve the field.
     * @param field  The name of the private field to retrieve.
     * @return The value of the private field, or null if an error occurs.
     */
    private static Object getPrivateField(Object object, String field) {
        Object result;
        try {
            Field objectField = object.getClass().getDeclaredField(field);
            objectField.setAccessible(true);
            result = objectField.get(object);
            objectField.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

    /**
     * <p>CommandManagerRegistry class provides a method to unregister a command from a
     * JavaPlugin. This method takes two parameters, a Command and a JavaPlugin. It
     * uses reflection to access the private fields of the Bukkit Server PluginManager
     * and SimpleCommandMap. It then uses a HashMap to remove the command and its
     * aliases from the knownCommands map.</p>
     *
     * @param cmd        The Command object to unregister.
     */
    public static void unregisterCommand(Command cmd) {
        try {
            Object result = getPrivateField(Bukkit.getServer().getPluginManager(), "commandMap");
            SimpleCommandMap commandMap = (SimpleCommandMap) result;
            assert commandMap != null;
            /*Object map = getPrivateField(commandMap, "knownCommands");
            @SuppressWarnings("unchecked")
            HashMap<String, Command> knownCommands = (HashMap<String, Command>) map;
            assert knownCommands != null;
            knownCommands.remove(cmd.getName());
            for (String alias : cmd.getAliases()) {
                if (knownCommands.containsKey(alias) && knownCommands.get(alias).toString().contains(javaPlugin.getName())) {
                    knownCommands.remove(alias);
                }
            }*/
            cmd.unregister(commandMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>CommandManagerRegistry class provides the register() method which allows for
     * registering a CommandManager. This method takes a CommandManager as a parameter
     * and returns a boolean value. It checks if the CommandManager has already been
     * registered or if the parameter is null. If either of these conditions is true,
     * the method returns false. It then checks if the CommandManagerRegistry is ready
     * to use by checking if the Start Method has been called. If not, a
     * RuntimeException is thrown.</p>
     *
     * <p>The method then checks if the CommandManager has CommandData and registers the
     * Command on the Server. If the Command already exists, the Executor and
     * TabCompleter are set to the CommandManagerRegistry. If the Command does not
     * exist, a new DCommand is created and registered.</p>
     *
     * <p>The method then checks if the CommandManager has an autoRegisterPermission and
     * adds the permission to the PluginManager if it does not already exist. Finally,
     * the setup() method is called for adding Sub Commands and the CommandManager is
     * added to the CommandManagerRegistry with the CommandData. The method then
     * returns true.</p>
     *
     * @param cm The CommandManager to register.
     * @return True if the CommandManager was successfully registered, false otherwise.
     * @throws RuntimeException If the CommandManagerRegistry is not ready to use.
     */
    public boolean register(CommandManager cm) {
        if (commandManagers.containsKey(cm) || cm == null) return false;
        if (!commandManagerRegistryReady)
            throw new RuntimeException("CommandManagerRegistry (CMR) not ready to use yet. The Start Method has not been called yet.");

        // Pre Init SubCommands
        for (SubCommand subCommand : cm.getSubCommands()) {
            subCommand.preInit();
        }

        // Checking if the Command Manager has CommandData

        CommandData data = cm.getClass().getAnnotation(CommandData.class);

        JavaPlugin javaPlugin = cm.getJavaPlugin();
        // Registering the Command on the Server
        if (javaPlugin.getCommand(cm.getCommandName()) != null) {
            Objects.requireNonNull(javaPlugin.getCommand(cm.getCommandName())).setExecutor(this);
            Objects.requireNonNull(javaPlugin.getCommand(cm.getCommandName())).setTabCompleter(this);
        } else {
            DCommand pluginCommand = new DCommand(cm.getCommandName(), javaPlugin);
            pluginCommand.setProperty("label", javaPlugin.getName().toLowerCase());
            pluginCommand.setProperty("aliases", cm.getCommandAliases());
            pluginCommand.setProperty("usage", cm.getCommandUsage());
            pluginCommand.setProperty("description", cm.getCommandInfo());
            pluginCommand.setProperty("permission", cm.getCommandPermissionAsString());
            pluginCommand.setExecutor(this);
            pluginCommand.setTabCompleter(this);
            pluginCommand.register();
        }
        if (cm.autoRegisterPermission()) {
            if (cm.getCommandPermissionAsString().isEmpty()) {
                if (!Bukkit.getPluginManager().getPermissions().contains(cm.getCommandPermissionAsPermission())) {
                    Bukkit.getPluginManager().addPermission(cm.getCommandPermissionAsPermission());
                }
            } else {
                Permission tempPerm = new Permission(cm.getCommandPermissionAsString());
                if (!Bukkit.getPluginManager().getPermissions().contains(tempPerm)) {
                    Bukkit.getPluginManager().addPermission(tempPerm);
                }
            }
        }
        // Calling setup() for Adding Sub Commands
        cm.setup();

        for (SubCommand subCommand : cm.getSubCommands()) {
            if (subCommand.autoRegisterPermission()) {
                if (subCommand.permissionAsString().isEmpty()) {
                    if (!Bukkit.getPluginManager().getPermissions().contains(subCommand.permissionAsPermission())) {
                        Bukkit.getPluginManager().addPermission(subCommand.permissionAsPermission());
                    }
                } else {
                    Permission tempPerm = new Permission(subCommand.permissionAsString());
                    if (!Bukkit.getPluginManager().getPermissions().contains(tempPerm)) {
                        Bukkit.getPluginManager().addPermission(tempPerm);
                    }
                }
            }
        }

        commandManagers.put(cm, data);

        // Post Init SubCommands
        for (SubCommand subCommand : cm.getSubCommands()) {
            subCommand.postInit();
        }
        return true;
    }

    /**
     * Unregisters a CommandManager from the CommandManagerRegistry. This method will
     * unregister the CommandManager from the CommandManagerRegistry, unregister the
     * command from the server, and remove the associated permission from the server if
     * the CommandManager's autoRegisterPermission is set to true. If the CommandManager
     * is not registered or if it's null, this method does nothing.
     *
     * @param cm The CommandManager to unregister.
     * @throws RuntimeException If the CommandManagerRegistry (CMR) is not ready to use yet.
     */
    public void unregister(CommandManager cm) {
        if (!commandManagers.containsKey(cm) || cm == null) return;
        if (!commandManagerRegistryReady)
            throw new RuntimeException("CommandManagerRegistry (CMR) not ready to use yet. The Start Method has not been called yet.");

        JavaPlugin javaPlugin = cm.getJavaPlugin();

        // Unregistering the Command on the Server
        if (javaPlugin.getCommand(cm.getCommandName()) != null) {
            unregisterCommand(javaPlugin.getCommand(cm.getCommandName()));
        } else {
            DCommand pluginCommand = new DCommand(cm.getCommandName(), javaPlugin);
            pluginCommand.setProperty("label", javaPlugin.getName().toLowerCase());
            pluginCommand.setProperty("aliases", cm.getCommandAliases());
            pluginCommand.setProperty("usage", cm.getCommandUsage());
            pluginCommand.setProperty("description", cm.getCommandInfo());
            pluginCommand.setProperty("permission", cm.getCommandPermissionAsString());
            unregisterCommand(pluginCommand);
        }
        if (cm.autoRegisterPermission()) {
            if (cm.getCommandPermissionAsString().isEmpty()) {
                if (Bukkit.getPluginManager().getPermissions().contains(cm.getCommandPermissionAsPermission())) {
                    Bukkit.getPluginManager().removePermission(cm.getCommandPermissionAsPermission());
                }
            } else {
                Permission tempPerm = new Permission(cm.getCommandPermissionAsString());
                if (Bukkit.getPluginManager().getPermissions().contains(tempPerm)) {
                    Bukkit.getPluginManager().removePermission(tempPerm);
                }
            }
        }

        for (SubCommand subCommand : cm.getSubCommands()) {
            if (subCommand.autoRegisterPermission()) {
                if (subCommand.permissionAsString().isEmpty()) {
                    if (Bukkit.getPluginManager().getPermissions().contains(subCommand.permissionAsPermission())) {
                        Bukkit.getPluginManager().removePermission(subCommand.permissionAsPermission());
                    }
                } else {
                    Permission tempPerm = new Permission(subCommand.permissionAsString());
                    if (Bukkit.getPluginManager().getPermissions().contains(tempPerm)) {
                        Bukkit.getPluginManager().removePermission(tempPerm);
                    }
                }
            }
        }

        // Call SubCommands.clear to Sub Command List
        cm.getSubCommands().clear();
        commandManagers.remove(cm);
    }

    /**
     * Unregisters all CommandManagers from the CommandManagerRegistry. If the
     * CommandManagerRegistry has not been initialized, a RuntimeException will be
     * thrown.
     *
     * @throws RuntimeException If the CommandManagerRegistry (CMR) is not ready to use yet.
     */
    public void unregisterAll() {
        if (!commandManagerRegistryReady)
            throw new RuntimeException("CommandManagerRegistry (CMR) not ready to use yet. The Start Method has not been called yet.");
        for (CommandManager cm : commandManagers.keySet()) {
            unregister(cm);
        }
    }

    /**
     * Retrieves the map of CommandManager and CommandData objects.
     *
     * @return A map of CommandManager and CommandData objects.
     * @throws RuntimeException if the CommandManagerRegistry (CMR) is not ready to use yet.
     */
    public Map<CommandManager, CommandData> getCommandManagers() {
        if (!commandManagerRegistryReady)
            throw new RuntimeException("CommandManagerRegistry (CMR) not ready to use yet. The Start Method has not been called yet.");
        return commandManagers;
    }

    /**
     * Retrieves the CommandManager associated with the given command name.
     *
     * @param commandName The name of the command to retrieve the CommandManager for.
     * @return The CommandManager associated with the given command name, or null if none is found.
     */
    public CommandManager getCommandManager(String commandName) {
        for (CommandManager cm : commandManagers.keySet()) {
            if (cm.getCommandName().equals(commandName)) {
                return cm;
            } else if (cm.getCommandAliases().contains(commandName)) {
                return cm;
            }
        }
        return null;
    }

    /**
     * Checks if a player is required for the given CommandManager.
     *
     * @param commandManager The CommandManager to check.
     * @return True if a player is required, false otherwise.
     */
    public Boolean isPlayerRequired(CommandManager commandManager) {
        CommandData data = commandManagers.get(commandManager);
        if (data == null) return false;
        return data.playerRequired();
    }

    /**
     * Checks if a CommandManager requires an operator to execute.
     *
     * @param commandManager The CommandManager to check.
     * @return True if the CommandManager requires an operator to execute, false otherwise.
     */
    public Boolean isOpRequired(CommandManager commandManager) {
        CommandData data = commandManagers.get(commandManager);
        if (data == null) return false;
        return data.opRequired();
    }

    /**
     * Checks if the given {@link CommandManager} allows only sub-command arguments that fit to sub-arguments.
     *
     * @param commandManager The {@link CommandManager} to check.
     * @return {@code true} if the given {@link CommandManager} allows only sub-command arguments that fit to sub-arguments, {@code false} otherwise.
     */
    public Boolean allowOnlySubCommandArgsThatFitToSubArgs(CommandManager commandManager) {
        CommandData data = commandManagers.get(commandManager);
        if (data == null) return false;
        return data.allowOnlySubCommandArgsThatFitToSubArgs();
    }

    /**
     * Checks if the given {@link CommandManager} has sender type specific sub-arguments.
     * @param commandManager The {@link CommandManager} to check.
     * @return {@code true} if the given {@link CommandManager} has sender type-specific sub-arguments, {@code false} otherwise.
     */
    public boolean senderTypeSpecificSubArgs(CommandManager commandManager) {
        CommandData data = commandManagers.get(commandManager);
        if (data == null) return false;
        return data.senderTypeSpecificSubArgs();
    }

    /**
     * Retrieves a list of subcommands associated with the specified command name.
     *
     * @param commandName The name of the command to retrieve subcommands for.
     * @return A list of subcommands associated with the specified command name.
     */
    public List<SubCommand> getSubCommands(String commandName) {
        return getCommandManager(commandName).getSubCommands();
    }

    /**
     * Handles the execution of commands registered in the {@link CommandManagerRegistry}.
     * Logs the command execution and result (success or failure) to the log file.
     *
     * @param sender The {@link CommandSender} who executed the command.
     * @param cmd    The {@link Command} that was executed.
     * @param label  The command label.
     * @param args   The command arguments.
     * @return True if the command was successfully executed, false otherwise.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        for (CommandManager cm : commandManagers.keySet()) {
            try {
                if (cm.getCommandName().equalsIgnoreCase(cmd.getName())) {
                    if (args.length == 0) {
                        sender.sendMessage(lgm.getMessage("Player.Commands.TooFewArguments", (sender instanceof Player) ? (Player) sender : null, true));
                        return true;
                    }
                    if (!(sender instanceof Player)) {
                        if (isPlayerRequired(cm)) {
                            sender.sendMessage(lgm.getMessage("Console.ExecutesPlayerCommand", null, true));
                            return true;
                        }
                    }
                    boolean commandResult = cm.onCommand(sender, args);

                    // Logging the command execution
                    String logMessage = "Command execution for command: " + cmd.getName() + ", Args: " + Arrays.toString(args) + ", Sender: " + sender.getName();

                    // Log the command result (success or failure)
                    if (commandResult) {
                        logMessage += " - Success";
                    } else {
                        logMessage += " - Failure";
                    }

                    CoolStuffLib.getLib().writeToLog(Level.INFO, logMessage, LogPrefix.COOLSTUFFLIB_COMMANDS, false);
                    return commandResult;
                }
            } catch (Exception e) {
                // Error occurred during command execution, log it.
                String logMessage = "Error during command execution for command: " + cmd.getName() + ", Args: " + Arrays.toString(args)
                        + ", Error: " + e.getMessage() + ", Stacktrace: " + Arrays.toString(e.getStackTrace());
                CoolStuffLib.getLib().writeToLog(Level.SEVERE, logMessage, LogPrefix.ERROR, true);
            }
        }
        return true;
    }

    /**
     * CommandManagerRegistry class provides a method to complete tab for a command.
     * This method takes four parameters, CommandSender sender, Command cmd, String
     * label, and String[] args. It returns a list of strings. If the sender is not a
     * player and the command requires a player, an empty list is returned. If the
     * args length is 0, an empty list is returned. Otherwise, the onTabComplete
     * method of the CommandManager is called and the result is returned. If an error
     * occurs, it is logged, and an empty list is returned.
     *
     * @param sender The sender of the tab completion request.
     * @param cmd    The command that is being tab completed.
     * @param label  The label of the command.
     * @param args   The arguments provided for tab completion.
     * @return A list of tab completion options, or null if an error occurs.
     */
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        CommandManager cm = findCommandManager(cmd.getName());

        if (cm != null) {
            try {
                if (!(sender instanceof Player) && isPlayerRequired(cm)) {
                    return Collections.emptyList();
                }

                if (args.length == 0) {
                    return Collections.emptyList();
                }

                return cm.onTabComplete(sender, cmd, label, args);
            } catch (NullPointerException e) {
                String logMessage = "Error during tab completion for command: " + cmd.getName() + ", Args: " + Arrays.toString(args)
                        + ", Error: " + e.getMessage() + ", Stacktrace: " + Arrays.toString(e.getStackTrace());
                CoolStuffLib.getLib().writeToLog(Level.SEVERE, logMessage, LogPrefix.ERROR, true);
            }
        }

        return null;
    }

    /**
     * Finds the CommandManager associated with the given command name.
     *
     * @param commandName The name of the command to search for.
     * @return The CommandManager associated with the given command name, or null if none is found.
     */
    @Nullable
    private CommandManager findCommandManager(String commandName) {
        for (CommandManager cm : commandManagers.keySet()) {
            if (cm.getCommandName().equalsIgnoreCase(commandName)) {
                return cm;
            }
        }
        return null;
    }

    /**
     * Checks if the CommandManagerRegistry is ready.
     *
     * @return True if the CommandManagerRegistry is ready, false otherwise.
     */
    public boolean isCommandManagerRegistryReady() {
        return commandManagerRegistryReady;
    }

    /**
     * Sets a boolean value indicating whether the CommandManagerRegistry is ready.
     *
     * @param cmrReady Boolean value indicating whether the CommandManagerRegistry is ready.
     */
    public void setCommandManagerRegistryReady(boolean cmrReady) {
        this.commandManagerRegistryReady = cmrReady;
    }

    /**
     * Sets the LanguageManager for the CommandManagerRegistry.
     *
     * @param lgm The LanguageManager to be set.
     */
    public void setLanguageManager(LanguageManager lgm) {
        this.lgm = lgm;
    }

    /**
     * Provides access to the JavaPlugin associated with the CommandManagerRegistry.
     *
     * @return The JavaPlugin associated with the CommandManagerRegistry.
     */
    public JavaPlugin getPlugin() {
        return plugin;
    }
}
