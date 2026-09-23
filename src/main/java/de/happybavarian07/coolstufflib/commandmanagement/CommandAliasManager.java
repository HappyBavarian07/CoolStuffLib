package de.happybavarian07.coolstufflib.commandmanagement;

import de.happybavarian07.coolstufflib.CoolStuffLib;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Creates, removes and restores command aliases at runtime and keeps them in an {@link AliasStore}.</p>
 *
 * <pre><code>
 * CommandAliasManager aliases = new CommandAliasManager(registry, new YamlAliasStore(dataYml, dataFile, YamlAliasStore.DEFAULT_SECTION));
 * aliases.loadAll();                                        // on enable
 * aliases.create(CommandAlias.of("gmc", "gamemode creative {args}"));
 * aliases.remove("gmc");
 * </code></pre>
 */
public final class CommandAliasManager {
    private final CommandManagerRegistry registry;
    private final AliasStore store;
    private final Map<String, CommandAlias> active = new ConcurrentHashMap<>();

    public CommandAliasManager(CommandManagerRegistry registry, AliasStore store) {
        this.registry = registry;
        this.store = store;
    }

    /** Registers every stored alias; broken ones are logged and skipped. Returns the registered names. */
    public List<String> loadAll() {
        List<String> registered = new ArrayList<>();
        for (CommandAlias alias : store.loadAll()) {
            try {
                register(alias);
                registered.add(alias.name());
            } catch (RuntimeException e) {
                CoolStuffLib.logError("Could not register stored command alias '" + alias.name() + "'", e);
            }
        }
        return registered;
    }

    /**
     * Registers and stores a new alias. Invalid aliases (name taken, runs itself, console alias without
     * permission) throw and are not stored.
     */
    public void create(CommandAlias alias) {
        register(alias);
        store.save(alias);
    }

    /** Unregisters and deletes an alias; {@code false} if no alias has that name. */
    public boolean remove(String name) {
        CommandAlias alias = active.remove(key(name));
        if (alias == null) return false;
        CommandManager manager = registry.getCommandManager(alias.name());
        if (manager != null) registry.unregister(manager);
        store.delete(alias.name());
        return true;
    }

    public Optional<CommandAlias> get(String name) {
        return Optional.ofNullable(active.get(key(name)));
    }

    public List<CommandAlias> list() {
        return active.values().stream().sorted((a, b) -> a.name().compareToIgnoreCase(b.name())).toList();
    }

    private void register(CommandAlias alias) {
        if (active.containsKey(key(alias.name()))) {
            throw new IllegalStateException("Alias '" + alias.name() + "' already exists");
        }
        alias.applyTo(registry.command(alias.name())).register();
        active.put(key(alias.name()), alias);
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
