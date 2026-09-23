package de.happybavarian07.coolstufflib.commandmanagement;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * <p>Builds and registers a whole command without writing a {@link CommandManager} class.
 * Obtain one with {@link CommandManagerRegistry#command(String)}.</p>
 *
 * <pre><code>
 * registry.command("heal")
 *         .permission("myplugin.heal")
 *         .arguments(Argument.player("target").optional())
 *         .player((player, args) -&gt; heal(args.has(0) ? args.onlinePlayer(0) : player))
 *         .register();
 * </code></pre>
 *
 * <p>The handler runs for {@code /heal} and for any first argument that is not a sub command added
 * with {@link #sub(SubCommand...)}.</p>
 */
public final class CommandBuilder {
    private final CommandManagerRegistry registry;
    private final String name;
    private final SubCommandBuilder root;
    private final List<SubCommand> subCommands = new ArrayList<>();
    private List<String> aliases = List.of();
    private String usage;
    private String info = "";
    private String permission;
    private boolean autoRegisterPermission = true;

    CommandBuilder(CommandManagerRegistry registry, String name) {
        this.registry = registry;
        this.name = name;
        this.root = new SubCommandBuilder(name, name);
    }

    public CommandBuilder aliases(String... aliases) {
        this.aliases = List.of(aliases);
        return this;
    }

    public CommandBuilder usage(String usage) {
        this.usage = usage;
        return this;
    }

    public CommandBuilder info(String info) {
        this.info = info;
        return this;
    }

    public CommandBuilder permission(String permission) {
        this.permission = permission;
        return this;
    }

    public CommandBuilder autoRegisterPermission(boolean autoRegisterPermission) {
        this.autoRegisterPermission = autoRegisterPermission;
        return this;
    }

    public CommandBuilder playerOnly() {
        root.playerOnly();
        return this;
    }

    public CommandBuilder opRequired() {
        root.opRequired();
        return this;
    }

    public CommandBuilder strictArguments() {
        root.strictArguments();
        return this;
    }

    public CommandBuilder minArgs(int minArgs) {
        root.minArgs(minArgs);
        return this;
    }

    public CommandBuilder maxArgs(int maxArgs) {
        root.maxArgs(maxArgs);
        return this;
    }

    public CommandBuilder arguments(Argument... arguments) {
        root.arguments(arguments);
        return this;
    }

    public CommandBuilder executes(CommandHandler handler) {
        root.executes(handler);
        return this;
    }

    public CommandBuilder player(CommandHandler.PlayerHandler handler) {
        root.player(handler);
        return this;
    }

    public CommandBuilder sub(SubCommand... subCommands) {
        this.subCommands.addAll(List.of(subCommands));
        return this;
    }

    /** Builds the command manager and registers it with the registry. */
    public CommandManager register() {
        if (registry.getCommandManager(name) != null) {
            throw new IllegalStateException("Command '" + name + "' is already registered");
        }
        String resolvedPermission = permission != null ? permission : name.toLowerCase(Locale.ROOT);
        String resolvedUsage = usage != null ? usage : generatedUsage();
        SubCommand rootCommand = null;
        if (root.handler != null) {
            rootCommand = root.permission(resolvedPermission).syntax(resolvedUsage).info(info)
                    .autoRegisterPermission(false).build();
        }
        BuiltCommandManager manager = new BuiltCommandManager(this, resolvedPermission, resolvedUsage, rootCommand);
        if (!registry.register(manager)) {
            throw new IllegalStateException("Command '" + name + "' is already registered");
        }
        return manager;
    }

    private String generatedUsage() {
        StringBuilder generated = new StringBuilder("/").append(name);
        for (Argument argument : root.arguments) generated.append(' ').append(argument.usage());
        if (!subCommands.isEmpty()) generated.append(root.handler == null ? " <sub command>" : " [sub command]");
        return generated.toString();
    }

    private static final class BuiltCommandManager extends CommandManager {
        private final CommandBuilder spec;
        private final String permission;
        private final String usage;

        private BuiltCommandManager(CommandBuilder spec, String permission, String usage, SubCommand rootCommand) {
            this.spec = spec;
            this.permission = permission;
            this.usage = usage;
            if (rootCommand != null) setRootCommand(rootCommand);
        }

        @Override
        public String getCommandName() {
            return spec.name;
        }

        @Override
        public String getCommandUsage() {
            return usage;
        }

        @Override
        public String getCommandInfo() {
            return spec.info;
        }

        @Override
        public JavaPlugin getJavaPlugin() {
            return spec.registry.getPlugin();
        }

        @Override
        public List<String> getCommandAliases() {
            return spec.aliases;
        }

        @Override
        public String getCommandPermissionAsString() {
            return permission;
        }

        @Override
        public boolean autoRegisterPermission() {
            return spec.autoRegisterPermission;
        }

        @Override
        public void setup() {
            for (SubCommand subCommand : spec.subCommands) registerSubCommand(subCommand);
        }
    }
}
