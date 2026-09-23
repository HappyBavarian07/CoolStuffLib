package de.happybavarian07.coolstufflib.commandmanagement;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * <p>Describes one positional argument of a sub command. Drives tab completion, the generated syntax,
 * the argument count limits and the names used in {@link CommandArgs} error messages.</p>
 *
 * <pre><code>
 * &#64;Override
 * public List&lt;Argument&gt; arguments() {
 *     return List.of(Argument.player("target"), Argument.integer("amount").optional());
 * }
 * </code></pre>
 */
public final class Argument {
    private final String name;
    private final boolean required;
    private final boolean greedy;
    private final Function<CommandSender, Collection<String>> completer;

    private Argument(String name, boolean required, boolean greedy, Function<CommandSender, Collection<String>> completer) {
        this.name = name;
        this.required = required;
        this.greedy = greedy;
        this.completer = completer;
    }

    /** Online player names. */
    public static Argument player(String name) {
        return new Argument(name, true, false, sender -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
    }

    /** Loaded world names. */
    public static Argument world(String name) {
        return new Argument(name, true, false, sender -> Bukkit.getWorlds().stream().map(World::getName).toList());
    }

    /** One of a fixed set of values. */
    public static Argument choice(String name, String... options) {
        List<String> values = List.of(options);
        return new Argument(name, true, false, sender -> values);
    }

    /** Enum constant, completed in lower case. */
    public static <E extends Enum<E>> Argument enumOf(String name, Class<E> type) {
        List<String> values = Arrays.stream(type.getEnumConstants()).map(e -> e.name().toLowerCase(Locale.ROOT)).toList();
        return new Argument(name, true, false, sender -> values);
    }

    /** A whole number; no completions. */
    public static Argument integer(String name) {
        return new Argument(name, true, false, sender -> List.of());
    }

    /** A decimal number; no completions. */
    public static Argument decimal(String name) {
        return new Argument(name, true, false, sender -> List.of());
    }

    /** A single word; no completions. */
    public static Argument word(String name) {
        return new Argument(name, true, false, sender -> List.of());
    }

    /** Free text that consumes all remaining arguments. Must be last. */
    public static Argument text(String name) {
        return new Argument(name, true, true, sender -> List.of());
    }

    /** Custom completions; the sender may be {@code null}. */
    public static Argument custom(String name, Function<CommandSender, Collection<String>> completer) {
        return new Argument(name, true, false, completer);
    }

    /** Returns an optional copy of this argument. */
    public Argument optional() {
        return new Argument(name, false, greedy, completer);
    }

    public String name() {
        return name;
    }

    public boolean required() {
        return required;
    }

    public boolean greedy() {
        return greedy;
    }

    public List<String> complete(CommandSender sender) {
        Collection<String> values = completer.apply(sender);
        return values == null ? List.of() : List.copyOf(values);
    }

    /** {@code <name>}, {@code [name]} or {@code <name...>} for syntax lines. */
    public String usage() {
        String label = greedy ? name + "..." : name;
        return required ? "<" + label + ">" : "[" + label + "]";
    }
}
