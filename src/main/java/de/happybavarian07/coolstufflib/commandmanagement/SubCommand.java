package de.happybavarian07.coolstufflib.commandmanagement;
/*
 * @Author HappyBavarian07
 * @Date 05.10.2021 | 17:28
 */

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.languagemanager.Placeholder;
import de.happybavarian07.coolstufflib.languagemanager.PlaceholderType;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;

import java.util.HashMap;
import java.util.Map;

@CommandData
public abstract class SubCommand {
    protected CoolStuffLib lib = CoolStuffLib.getLib();
    protected LanguageManager lgm = lib.getLanguageManager();
    protected CommandManagerRegistry registry = lib.getCommandManagerRegistry();
    protected String mainCommandName = "";
    /*
    /<command> <subcommand> args[0] args[1]
     */

    public SubCommand(String mainCommandName) {
        this.mainCommandName = mainCommandName;
    }

    /**
     * The isPlayerRequired function is used to determine whether or not the command requires a player.
     *
     *
     *
     * @return A boolean value
     */
    public boolean isPlayerRequired() {
        if (!this.getClass().isAnnotationPresent(CommandData.class)) {
            return registry.isPlayerRequired(registry.getCommandManager(mainCommandName));
        }
        return this.getClass().getAnnotation(CommandData.class).playerRequired();
    }

    /**
     * The isOpRequired function is used to determine whether or not the command requires OP status.
     *
     *
     *
     * @return The value of the oprequired variable in the commanddata annotation
     */
    public boolean isOpRequired() {
        if (!this.getClass().isAnnotationPresent(CommandData.class)) {
            return registry.isOpRequired(registry.getCommandManager(mainCommandName));
          }
        return this.getClass().getAnnotation(CommandData.class).opRequired();
    }

    /**
     * The allowOnlySubCommandArgsThatFitToSubArgs function is used to determine whether or not the command should only allow sub-command arguments that fit into the sub-command's argument list.
     * For example, if a command has two sub-commands: &quot;sub&quot; and &quot;sub2&quot;, and each of those commands have their own argument lists, then this function will determine whether or not it is possible for a user to enter an invalid set of arguments.
     * If this function returns true (which it does by default), then if a user enters &quot;/mainCommandName sub arg0 arg2&quot;, where arg0 and arg2 are valid arguments for the mainCommandName command
     *
     *
     * @return The value of the allowonlysubcommandargsthatfittosubargs function in the commandregistry class
     */
    public boolean allowOnlySubCommandArgsThatFitToSubArgs() {
        if (!this.getClass().isAnnotationPresent(CommandData.class)) {
            return registry.allowOnlySubCommandArgsThatFitToSubArgs(registry.getCommandManager(mainCommandName));
        }
        return this.getClass().getAnnotation(CommandData.class).allowOnlySubCommandArgsThatFitToSubArgs();
    }

    /**
     * The onPlayerCommand function is called when a player executes a command.
     *
     *
     * @param player Get the player who sent the command
     * @param args Get the arguments that are passed to the command
     *
     * @return A boolean value
     *
     * @docauthor Trelent
     */
    public boolean onPlayerCommand(Player player, String[] args) {
        return handleCommand(player, player, args);
    }

    /**
     * The onConsoleCommand function is called when a command is entered into the console.
     *
     *
     * @param sender Send messages to the console
     * @param args Get the arguments that were passed to the command
     *
     * @return A boolean, which is used to determine whether the command was successful
     *
     * @docauthor Trelent
     */
    public boolean onConsoleCommand(ConsoleCommandSender sender, String[] args) {
        return handleCommand(sender, null, args);
    }

    /**
     * The handleCommand function is the main function of a CommandHandler.
     * It takes in a CommandSender, which can be either a Player or ConsoleCommandSender,
     * and an array of Strings containing all arguments passed to the command.
     * The handleCommand function should return true if it handled the command successfully, false otherwise.

     *
     * @param sender Send messages to the player who executed the command
     * @param playerOrNull Get the player's location, inventory, and more
     * @param args Get the arguments that were passed to the command
     *
     * @return A boolean, which is a true or false value
     *
     * @docauthor Trelent
     */
    public boolean handleCommand(CommandSender sender, Player playerOrNull, String[] args) {
        return false;
    }

    public abstract String name();

    public abstract String info();

    public abstract String[] aliases();

    public abstract Map<Integer, String[]> subArgs();

    public abstract String syntax();

    public Permission permissionAsPermission() {
        return new Permission(permissionAsString(), permissionAsString());
    }
    public abstract String permissionAsString();
    public abstract boolean autoRegisterPermission();

    /**
     * The format function is used to format the help message for a subcommand.
     * It replaces placeholders in the string with their respective values.
     * The following placeholders are available:
     * &lt;ul&gt;
     *     &lt;li&gt;%usage% - Replaced with the usage of this command&lt;/li&gt;
     *     &lt;li&gt;%description% - Replaced with a description of this command&lt;/li&gt;
     *     &lt;li&gt;%name% - Replaced with the name of this command&lt;/li&gt;

     *
     * @param in Format the message
     * @param cmd Get the information about the command
     *
     * @return The string with the placeholders replaced
     *
     * @docauthor Trelent
     */
    protected String format(String in, SubCommand cmd) {
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
