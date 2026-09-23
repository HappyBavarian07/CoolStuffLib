package de.happybavarian07.coolstufflib.commandmanagement;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>Typed access to a sub command's arguments (indexes start at 0, after the sub command name).
 * Invalid or missing input throws {@link CommandArgumentException}, which the command manager turns into
 * a localized message, so handlers need no try/catch.</p>
 *
 * <pre><code>
 * &#64;Override
 * public boolean execute(CommandSender sender, CommandArgs args) {
 *     Player target = args.onlinePlayer(0);
 *     int amount = args.integer(1, 1, 1, 64);
 *     ...
 *     return true;
 * }
 * </code></pre>
 */
public final class CommandArgs {
    private final CommandSender sender;
    private final String[] raw;
    private final List<Argument> definitions;

    public CommandArgs(CommandSender sender, String[] raw, List<Argument> definitions) {
        this.sender = sender;
        this.raw = raw == null ? new String[0] : raw.clone();
        this.definitions = definitions == null ? List.of() : definitions;
    }

    public CommandSender sender() {
        return sender;
    }

    /** The sender as a player; fails with the "you have to be a player" message otherwise. */
    public Player player() {
        if (sender instanceof Player player) return player;
        throw new CommandArgumentException("Console.ExecutesPlayerCommand", "%prefix% &9> &cYou have to be a Player!", Map.of());
    }

    public int size() {
        return raw.length;
    }

    public boolean has(int index) {
        return index >= 0 && index < raw.length;
    }

    public String[] raw() {
        return raw.clone();
    }

    public String string(int index) {
        return require(index);
    }

    public String string(int index, String defaultValue) {
        return has(index) ? raw[index] : defaultValue;
    }

    /** All arguments from {@code from} on, joined with spaces. */
    public String joined(int from) {
        require(from);
        return String.join(" ", Arrays.copyOfRange(raw, from, raw.length));
    }

    public int integer(int index) {
        String value = require(index);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw notANumber(index, value);
        }
    }

    public int integer(int index, int defaultValue) {
        return has(index) ? integer(index) : defaultValue;
    }

    /** An integer that must lie within {@code [min, max]}. */
    public int integer(int index, int min, int max) {
        return checkRange(index, integer(index), min, max);
    }

    /** An optional integer within {@code [min, max]}; {@code defaultValue} when absent. */
    public int integer(int index, int defaultValue, int min, int max) {
        return has(index) ? integer(index, min, max) : defaultValue;
    }

    public double decimal(int index) {
        String value = require(index);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw notANumber(index, value);
        }
    }

    public double decimal(int index, double defaultValue) {
        return has(index) ? decimal(index) : defaultValue;
    }

    /** Accepts true/false, yes/no, on/off, 1/0 (case-insensitive). */
    public boolean bool(int index) {
        String value = require(index);
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "yes", "on", "1" -> true;
            case "false", "no", "off", "0" -> false;
            default -> throw invalidChoice(index, value, "true, false");
        };
    }

    public Player onlinePlayer(int index) {
        String name = require(index);
        Player player = Bukkit.getPlayerExact(name);
        if (player == null) {
            throw new CommandArgumentException("Player.Commands.PlayerNotFound",
                    "%prefix% &9> &cPlayer %value% is not online!", placeholders(index, name));
        }
        return player;
    }

    public World world(int index) {
        String name = require(index);
        World world = Bukkit.getWorld(name);
        if (world == null) {
            throw new CommandArgumentException("Player.Commands.WorldNotFound",
                    "%prefix% &9> &cWorld %value% does not exist!", placeholders(index, name));
        }
        return world;
    }

    /** Case-insensitive enum constant lookup. */
    public <E extends Enum<E>> E enumOf(int index, Class<E> type) {
        String value = require(index);
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value)) return constant;
        }
        String options = Arrays.stream(type.getEnumConstants())
                .map(constant -> constant.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(", "));
        throw invalidChoice(index, value, options);
    }

    private String require(int index) {
        if (!has(index)) {
            throw new CommandArgumentException("Player.Commands.MissingArgument",
                    "%prefix% &9> &cMissing argument: %argument%", placeholders(index, ""));
        }
        return raw[index];
    }

    private int checkRange(int index, int value, int min, int max) {
        if (value < min || value > max) {
            Map<String, String> placeholders = new java.util.HashMap<>(placeholders(index, String.valueOf(value)));
            placeholders.put("%min%", String.valueOf(min));
            placeholders.put("%max%", String.valueOf(max));
            throw new CommandArgumentException("Player.Commands.NumberOutOfRange",
                    "%prefix% &9> &c%value% must be between %min% and %max%!", placeholders);
        }
        return value;
    }

    private CommandArgumentException notANumber(int index, String value) {
        return new CommandArgumentException("Player.Commands.NotANumber",
                "%prefix% &9> &cThat's not a Number!", placeholders(index, value));
    }

    private CommandArgumentException invalidChoice(int index, String value, String options) {
        Map<String, String> placeholders = new java.util.HashMap<>(placeholders(index, value));
        placeholders.put("%options%", options);
        return new CommandArgumentException("Player.Commands.InvalidChoice",
                "%prefix% &9> &c%value% is not valid. Options: %options%", placeholders);
    }

    private Map<String, String> placeholders(int index, String value) {
        String name = index < definitions.size() ? definitions.get(index).name() : "#" + (index + 1);
        return Map.of("%argument%", name, "%value%", value, "%index%", String.valueOf(index + 1));
    }
}
