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

    public boolean isPlayerRequired() {
        if (!this.getClass().isAnnotationPresent(CommandData.class)) {
            return registry.isPlayerRequired(registry.getCommandManager(mainCommandName));
        }
        return this.getClass().getAnnotation(CommandData.class).playerRequired();
    }

    public boolean isOpRequired() {
        if (!this.getClass().isAnnotationPresent(CommandData.class)) {
            return registry.isOpRequired(registry.getCommandManager(mainCommandName));
        }
        return this.getClass().getAnnotation(CommandData.class).opRequired();
    }

    /**
     * Checks if the Sub Command can only be executed if the Player gives the Args given in the {subArgs()} Method
     *
     * @return boolean
     */
    public boolean allowOnlySubCommandArgsThatFitToSubArgs() {
        if (!this.getClass().isAnnotationPresent(CommandData.class)) {
            return registry.allowOnlySubCommandArgsThatFitToSubArgs(registry.getCommandManager(mainCommandName));
        }
        return this.getClass().getAnnotation(CommandData.class).allowOnlySubCommandArgsThatFitToSubArgs();
    }

    public boolean onPlayerCommand(Player player, String[] args) {
        return handleCommand(player, player, args);
    }

    public boolean onConsoleCommand(ConsoleCommandSender sender, String[] args) {
        return handleCommand(sender, null, args);
    }

    public boolean handleCommand(CommandSender sender, Player playerOrNull, String[] args) {
        return false;
    }

    public abstract String name();

    public abstract String info();

    public abstract String[] aliases();

    public abstract Map<Integer, String[]> subArgs();

    public abstract String syntax();

    public abstract String permission();

    protected String format(String in, SubCommand cmd) {
        Map<String, Placeholder> placeholders = new HashMap<>();
        placeholders.put("%usage%", new Placeholder("%usage%", cmd.syntax(), PlaceholderType.ALL));
        placeholders.put("%description%", new Placeholder("%description%", cmd.info(), PlaceholderType.ALL));
        placeholders.put("%name%", new Placeholder("%name%", cmd.name(), PlaceholderType.ALL));
        placeholders.put("%permission%", new Placeholder("%permission%", cmd.permission(), PlaceholderType.ALL));
        placeholders.put("%aliases%", new Placeholder("%aliases%", cmd.aliases(), PlaceholderType.ALL));
        placeholders.put("%subArgs%", new Placeholder("%subArgs%", cmd.subArgs().toString(), PlaceholderType.ALL));

        return lgm.replacePlaceholders(in, placeholders);
    }
}
