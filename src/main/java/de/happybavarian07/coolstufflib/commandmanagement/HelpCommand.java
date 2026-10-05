package de.happybavarian07.coolstufflib.commandmanagement;
/*
 * @Author HappyBavarian07
 * @Date 05.10.2021 | 17:53
 */

import de.happybavarian07.coolstufflib.languagemanager.PlaceholderType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>Built-in help sub command.</p>
 * <ul>
 * <li>{@code /<main> help [page]} lists every command the sender may use, nested ones included.</li>
 * <li>{@code /<main> help <command> [sub command...]} shows details: syntax, description, aliases,
 * permission, arguments, sub commands and notes (player only, cooldown, confirmation, async).</li>
 * </ul>
 */
@CommandData
@SubCommandInfo(name = "help", info = "Shows all commands or details about one command", autoRegisterPermission = false)
public class HelpCommand extends SubCommand {
    public static final int PAGE_SIZE = 10;

    public HelpCommand(String mainCommandName) {
        super(mainCommandName);
    }

    @Override
    public String permissionAsString() {
        return "";
    }

    @Override
    public List<Argument> arguments() {
        return List.of(Argument.word("page|command").optional());
    }

    @Override
    public boolean execute(CommandSender sender, CommandArgs args) {
        Player player = sender instanceof Player p ? p : null;
        if (args.size() == 0) return showPage(sender, player, 1);
        if (args.size() == 1 && args.string(0).matches("\\d+")) return showPage(sender, player, args.integer(0));

        SubCommand target = resolve(args.raw());
        if (target == null || !visibleTo(sender, target)) {
            sender.sendMessage(CommandMessages.render(lgm, player, "Player.Commands.HelpUnknownCommand",
                    "%prefix% &9> &cNo help found for %command%.", Map.of("%command%", String.join(" ", args.raw()))));
            return true;
        }
        showDetails(sender, player, target);
        return true;
    }

    @Override
    public Map<Integer, String[]> subArgs(CommandSender sender, int isPlayer, String[] args) {
        Map<Integer, String[]> completions = new HashMap<>();
        List<String> first = new ArrayList<>();
        int pages = new PaginatedList<>(visibleCommands(sender)).maxItemsPerPage(PAGE_SIZE).pageCount();
        for (int page = 1; page <= pages; page++) first.add(String.valueOf(page));
        for (SubCommand command : manager().getSubCommands()) {
            if (sender == null || visibleTo(sender, command)) first.add(command.name());
        }
        completions.put(1, first.toArray(new String[0]));
        if (args.length > 1) {
            SubCommand parent = resolve(Arrays.copyOf(args, args.length - 1));
            if (parent != null) {
                completions.put(args.length, parent.getChildren().stream().map(SubCommand::name).toArray(String[]::new));
            }
        }
        return completions;
    }

    private boolean showPage(CommandSender sender, Player player, int page) {
        PaginatedList<SubCommand> commands = new PaginatedList<>(visibleCommands(sender))
                .maxItemsPerPage(PAGE_SIZE)
                .sort("subcommand", true);
        int pages = commands.pageCount();
        lgm.addPlaceholder(PlaceholderType.MESSAGE, "%page%", page, false);
        lgm.addPlaceholder(PlaceholderType.MESSAGE, "%max_page%", pages, false);
        if (page < 1 || page > pages) {
            sender.sendMessage(lgm.getMessage("Player.Commands.HelpPageDoesNotExist", player, true));
            return true;
        }
        sender.sendMessage(lgm.getMessage("Player.Commands.HelpMessages.Header", player, false));
        for (SubCommand command : commands.page(page)) {
            sender.sendMessage(format(lgm.getMessage("Player.Commands.HelpMessages.Format", player, false), command));
        }
        sender.sendMessage(lgm.getMessage("Player.Commands.HelpMessages.Footer", player, true));
        return true;
    }

    private void showDetails(CommandSender sender, Player player, SubCommand command) {
        Map<String, String> values = new HashMap<>();
        values.put("%usage%", command.syntax());
        values.put("%description%", command.info().isEmpty() ? "-" : command.info());
        values.put("%aliases%", String.join(", ", command.aliases()));
        values.put("%permission%", command.permissionAsString().isEmpty() ? "-" : command.permissionAsString());
        values.put("%arguments%", command.arguments().stream()
                .map(argument -> {
                    String description = argument.description(sender);
                    return argument.usage() + (description.isEmpty() ? "" : " (" + description + ")");
                })
                .collect(Collectors.joining(" ")));
        values.put("%subcommands%", command.getChildren().stream()
                .filter(child -> visibleTo(sender, child)).map(SubCommand::name).collect(Collectors.joining(", ")));
        values.put("%notes%", String.join(", ", notes(command)));

        Map<String, String> lines = new LinkedHashMap<>();
        lines.put("Header", "&a------------ &b%usage% &a------------");
        lines.put("Description", "&7Description: &f%description%");
        if (command.aliases().length > 0) lines.put("Aliases", "&7Aliases: &f%aliases%");
        lines.put("Permission", "&7Permission: &f%permission%");
        if (!command.arguments().isEmpty()) lines.put("Arguments", "&7Arguments: &f%arguments%");
        if (!values.get("%subcommands%").isEmpty()) lines.put("SubCommands", "&7Sub commands: &f%subcommands%");
        if (!values.get("%notes%").isEmpty()) lines.put("Notes", "&7Notes: &f%notes%");
        lines.forEach((key, fallback) -> sender.sendMessage(
                CommandMessages.render(lgm, player, "Player.Commands.HelpDetails." + key, fallback, values)));
    }

    private static List<String> notes(SubCommand command) {
        List<String> notes = new ArrayList<>();
        if (command.isPlayerRequired()) notes.add("players only");
        if (command.cooldownMillis() > 0) notes.add((command.cooldownMillis() + 999) / 1000 + "s cooldown");
        if (command.requiresConfirmation()) notes.add("needs confirmation");
        if (command.isAsync()) notes.add("runs asynchronously");
        return notes;
    }

    private SubCommand resolve(String[] path) {
        if (path.length == 0) return null;
        SubCommand current = manager().getSubCommand(path[0]);
        for (int i = 1; i < path.length && current != null; i++) {
            current = current.getChild(path[i]);
        }
        return current;
    }

    private List<SubCommand> visibleCommands(CommandSender sender) {
        CommandManager manager = manager();
        return manager.getAllSubCommands().stream()
                .filter(command -> command != manager.getRootCommand())
                .filter(command -> sender == null || visibleTo(sender, command))
                .toList();
    }

    private boolean visibleTo(CommandSender sender, SubCommand command) {
        if (command.isPlayerRequired() && !(sender instanceof Player)) return false;
        return manager().hasPermission(sender, command);
    }

    private CommandManager manager() {
        return registry.getCommandManager(mainCommandName);
    }
}
