package de.happybavarian07.coolstufflib.commandmanagement;

import org.bukkit.configuration.ConfigurationSection;

import java.time.Duration;
import java.util.List;

/**
 * <p>Definition of a command alias ("shortcut"): a new command that runs another command line.
 * See {@link CommandTemplate} for the placeholders in {@code command}.</p>
 *
 * @param name            the new command, e.g. {@code gmc}
 * @param command         the command line it runs, e.g. {@code gamemode creative {args}}
 * @param permission      permission for the alias, or {@code null} for the default ({@code <name>})
 * @param console         run the command line as the console (requires {@code permission})
 * @param description     help text, or {@code null}
 * @param aliases         further names for the alias command
 * @param playerOnly      only players may use it
 * @param cooldownSeconds per-sender cooldown, 0 for none
 * @param confirm         the sender must repeat the command to run it
 */
public record CommandAlias(String name, String command, String permission, boolean console, String description,
                           List<String> aliases, boolean playerOnly, long cooldownSeconds, boolean confirm) {

    public CommandAlias {
        if (name == null || name.isBlank() || name.contains(" ")) throw new IllegalArgumentException("Invalid alias name: " + name);
        if (command == null || command.isBlank()) throw new IllegalArgumentException("Alias '" + name + "' has no command");
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }

    /** A plain alias run as the sender. */
    public static CommandAlias of(String name, String command) {
        return new CommandAlias(name, command, null, false, null, List.of(), false, 0, false);
    }

    /**
     * <p>Reads {@code parent.<name>}, either the short form {@code name: "command"} or a section with
     * {@code command}, {@code permission}, {@code console}, {@code description}, {@code aliases},
     * {@code player-only}, {@code cooldown-seconds} and {@code confirm}.</p>
     */
    public static CommandAlias fromSection(ConfigurationSection parent, String name) {
        ConfigurationSection entry = parent.getConfigurationSection(name);
        if (entry == null) return of(name, parent.getString(name));
        return new CommandAlias(name, entry.getString("command"), entry.getString("permission"),
                entry.getBoolean("console"), entry.getString("description"), entry.getStringList("aliases"),
                entry.getBoolean("player-only"), entry.getLong("cooldown-seconds"), entry.getBoolean("confirm"));
    }

    /** Writes the long form into {@code section}; defaults are omitted. */
    public void writeTo(ConfigurationSection section) {
        section.set("command", command);
        if (permission != null) section.set("permission", permission);
        if (console) section.set("console", true);
        if (description != null) section.set("description", description);
        if (!aliases.isEmpty()) section.set("aliases", aliases);
        if (playerOnly) section.set("player-only", true);
        if (cooldownSeconds > 0) section.set("cooldown-seconds", cooldownSeconds);
        if (confirm) section.set("confirm", true);
    }

    /** Applies this definition to a builder from {@link CommandManagerRegistry#command(String)}. */
    public CommandBuilder applyTo(CommandBuilder builder) {
        if (permission != null) builder.permission(permission);
        if (description != null) builder.info(description);
        if (!aliases.isEmpty()) builder.aliases(aliases.toArray(new String[0]));
        if (playerOnly) builder.playerOnly();
        if (cooldownSeconds > 0) builder.cooldown(Duration.ofSeconds(cooldownSeconds));
        if (confirm) builder.confirm();
        return console ? builder.runsAsConsole(command) : builder.runs(command);
    }
}
