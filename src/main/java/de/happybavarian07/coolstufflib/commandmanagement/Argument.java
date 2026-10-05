package de.happybavarian07.coolstufflib.commandmanagement;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

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
    /** Choice lists up to this size are spelled out in usage lines. */
    public static final int MAX_INLINE_OPTIONS = 5;

    private final String name;
    private final boolean required;
    private final boolean greedy;
    private final Function<CommandSender, Collection<String>> completer;
    private final List<String> fixedOptions;
    private final ArgumentResolver<?> resolver;
    private final Function<CommandSender, CompletionStage<? extends Collection<String>>> asyncCompleter;
    private final Function<CommandSender, String> description;
    private final Predicate<CommandSender> visibility;
    private final long cacheMillis;
    private final Map<Object, CachedCompletion> cache = new ConcurrentHashMap<>();

    private Argument(String name, boolean required, boolean greedy, Function<CommandSender, Collection<String>> completer) {
        this(name, required, greedy, completer, null, null, null, sender -> "", sender -> true, 0);
    }

    private Argument(String name, boolean required, boolean greedy, Function<CommandSender, Collection<String>> completer,
                     List<String> fixedOptions) {
        this(name, required, greedy, completer, fixedOptions, null, null, sender -> "", sender -> true, 0);
    }

    private Argument(String name, boolean required, boolean greedy, Function<CommandSender, Collection<String>> completer,
                     List<String> fixedOptions, ArgumentResolver<?> resolver,
                     Function<CommandSender, CompletionStage<? extends Collection<String>>> asyncCompleter,
                     Function<CommandSender, String> description, Predicate<CommandSender> visibility, long cacheMillis) {
        this.name = name;
        this.required = required;
        this.greedy = greedy;
        this.completer = completer;
        this.fixedOptions = fixedOptions;
        this.resolver = resolver;
        this.asyncCompleter = asyncCompleter;
        this.description = description;
        this.visibility = visibility;
        this.cacheMillis = cacheMillis;
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
        return new Argument(name, true, false, sender -> values, values);
    }

    public static Argument choice(String name, Function<CommandSender, ? extends Collection<String>> choices) {
        return new Argument(name, true, false, choices::apply);
    }

    public static <T> Argument typedChoice(String name, ArgumentResolver<T> resolver) {
        Objects.requireNonNull(resolver);
        return new Argument(name, true, false, sender -> visibleOptions(sender, resolver.resolve(sender)),
                null, resolver, null, sender -> "", sender -> true, 0);
    }

    public static Argument asyncChoice(String name,
                                       Function<CommandSender, CompletionStage<? extends Collection<String>>> choices) {
        return new Argument(name, true, false, sender -> List.of(), null, null, choices, sender -> "", sender -> true, 0);
    }

    /** Enum constant, completed in lower case. */
    public static <E extends Enum<E>> Argument enumOf(String name, Class<E> type) {
        List<String> values = Arrays.stream(type.getEnumConstants()).map(e -> e.name().toLowerCase(Locale.ROOT)).toList();
        return new Argument(name, true, false, sender -> values, values);
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

    public Argument description(String description) {
        return description(sender -> description);
    }

    public Argument description(Function<CommandSender, String> description) {
        return copy(required, greedy, completer, fixedOptions, resolver, asyncCompleter,
                Objects.requireNonNull(description), cacheMillis);
    }

    public Argument permission(String permission) {
        return visibleWhen(sender -> sender != null && sender.hasPermission(permission));
    }

    public Argument visibleWhen(Predicate<CommandSender> predicate) {
        return copy(required, greedy, completer, fixedOptions, resolver, asyncCompleter, description,
                visibility.and(Objects.requireNonNull(predicate)), cacheMillis);
    }

    public Argument cached(Duration duration) {
        if (duration.isNegative() || duration.isZero()) throw new IllegalArgumentException("Cache duration must be positive");
        return copy(required, greedy, completer, fixedOptions, resolver, asyncCompleter, description, duration.toMillis());
    }

    public Argument clearCache() {
        cache.clear();
        return this;
    }

    /** Returns an optional copy of this argument. */
    public Argument optional() {
        return copy(false, greedy, completer, fixedOptions, resolver, asyncCompleter, description, cacheMillis);
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
        Object key = senderKey(sender);
        CachedCompletion cached = cacheMillis > 0 ? cache.get(key) : null;
        if (!visibility.test(sender)) return List.of();
        if (cached != null && cached.expiresAt() > System.currentTimeMillis()) return cached.values();
        Collection<String> values = completeUncached(sender);
        List<String> result = values == null ? List.of() : List.copyOf(values);
        if (cacheMillis > 0) cache.put(key, new CachedCompletion(result, System.currentTimeMillis() + cacheMillis));
        return result;
    }

    public CompletionStage<? extends Collection<String>> completeAsync(CommandSender sender) {
        if (asyncCompleter == null) {
            return java.util.concurrent.CompletableFuture.completedFuture(complete(sender));
        }
        if (!visibility.test(sender)) return java.util.concurrent.CompletableFuture.completedFuture(List.of());
        CompletionStage<? extends Collection<String>> stage = asyncCompleter.apply(sender);
        if (stage == null) throw new IllegalStateException("Async completer returned null for argument '" + name + "'");
        return stage.thenApply(values -> values == null ? List.of() : List.copyOf(values));
    }

    public boolean hasAsyncCompleter() {
        return asyncCompleter != null;
    }

    public String description(CommandSender sender) {
        String value = description.apply(sender);
        return value == null ? "" : value;
    }

    public boolean hasTypedResolver() {
        return resolver != null;
    }

    public <T> T resolve(CommandSender sender, String value) {
        if (resolver == null) throw new IllegalStateException("Argument has no typed resolver");
        if (!visibility.test(sender)) throw new IllegalArgumentException("Argument '" + name + "' is not available");
        for (ArgumentOption<?> option : visibleTypedOptions(sender)) {
            if (option.value().equalsIgnoreCase(value)) {
                @SuppressWarnings("unchecked")
                T resolved = (T) option.resolved();
                return resolved;
            }
        }
        throw new IllegalArgumentException("Unknown value '" + value + "' for argument '" + name + "'");
    }

    private Collection<String> completeUncached(CommandSender sender) {
        Collection<String> values = completer.apply(sender);
        return values == null ? List.of() : List.copyOf(values);
    }

    private List<ArgumentOption<?>> visibleTypedOptions(CommandSender sender) {
        Collection<? extends ArgumentOption<?>> options = resolver.resolve(sender);
        if (options == null) return List.of();
        List<ArgumentOption<?>> visible = new java.util.ArrayList<>();
        for (ArgumentOption<?> option : options) {
            if (option.permission() == null || sender != null && sender.hasPermission(option.permission())) {
                visible.add(option);
            }
        }
        return List.copyOf(visible);
    }

    private static <T> List<String> visibleOptions(CommandSender sender, Collection<ArgumentOption<T>> options) {
        if (options == null) return List.of();
        return options.stream()
                .filter(option -> option.permission() == null || sender != null && sender.hasPermission(option.permission()))
                .map(ArgumentOption::value)
                .toList();
    }

    private Argument copy(boolean required, boolean greedy, Function<CommandSender, Collection<String>> completer,
                          List<String> fixedOptions, ArgumentResolver<?> resolver,
                          Function<CommandSender, CompletionStage<? extends Collection<String>>> asyncCompleter,
                          Function<CommandSender, String> description, long cacheMillis) {
        return copy(required, greedy, completer, fixedOptions, resolver, asyncCompleter, description, visibility, cacheMillis);
    }

    private Argument copy(boolean required, boolean greedy, Function<CommandSender, Collection<String>> completer,
                          List<String> fixedOptions, ArgumentResolver<?> resolver,
                          Function<CommandSender, CompletionStage<? extends Collection<String>>> asyncCompleter,
                          Function<CommandSender, String> description, Predicate<CommandSender> visibility, long cacheMillis) {
        return new Argument(name, required, greedy, completer, fixedOptions, resolver, asyncCompleter, description, visibility, cacheMillis);
    }

    private static Object senderKey(CommandSender sender) {
        if (sender instanceof Player player) return player.getUniqueId();
        if (sender == null) return new UUID(0, 0);
        return sender.getName();
    }

    private record CachedCompletion(List<String> values, long expiresAt) {
    }

    /**
     * <p>Usage notation: {@code <name>} required, {@code [name]} optional, {@code <name...>} free text, and
     * short choice lists spelled out, e.g. {@code <survival|creative>} or {@code [on|off]}.</p>
     */
    public String usage() {
        return bracket(label(), required);
    }

    private String label() {
        if (greedy) return name + "...";
        if (fixedOptions != null && !fixedOptions.isEmpty() && fixedOptions.size() <= MAX_INLINE_OPTIONS) {
            return String.join("|", fixedOptions);
        }
        return name;
    }

    static String bracket(String label, boolean required) {
        return required ? "<" + label + ">" : "[" + label + "]";
    }
}
