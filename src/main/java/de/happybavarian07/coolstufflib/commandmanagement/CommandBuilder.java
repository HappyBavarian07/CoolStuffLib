package de.happybavarian07.coolstufflib.commandmanagement;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private CommandTemplate template;
    private boolean templateAsConsole;

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

    public CommandBuilder cooldown(Duration cooldown) {
        root.cooldown(cooldown);
        return this;
    }

    public CommandBuilder confirm() {
        root.confirm();
        return this;
    }

    public CommandBuilder async() {
        root.async();
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

    /**
     * <p>Makes this command an alias for another command line, run as the sender (their permissions
     * apply). Placeholders: {@code {0}}, {@code {1}}, ... {@code {args}}, {@code {args:1}}, {@code {player}}.
     * Argument limits and the usage line follow from the template.</p>
     * <pre><code>registry.command("gmc").runs("gamemode creative {args}").register();</code></pre>
     */
    public CommandBuilder runs(String commandLine) {
        return runsTemplate(commandLine, false);
    }

    /**
     * <p>Like {@link #runs(String)} but runs the command line as the console. Requires an explicit
     * {@link #permission(String)}, because the console bypasses the target command's own permission.</p>
     */
    public CommandBuilder runsAsConsole(String commandLine) {
        return runsTemplate(commandLine, true);
    }

    private CommandBuilder runsTemplate(String commandLine, boolean asConsole) {
        CommandTemplate parsed = new CommandTemplate(commandLine);
        this.template = parsed;
        this.templateAsConsole = asConsole;
        root.minArgs(parsed.requiredArgs());
        root.maxArgs(parsed.acceptsExtraArgs() ? Integer.MAX_VALUE : parsed.requiredArgs());
        root.executes((sender, args) -> {
            String line = parsed.resolve(sender, args.raw());
            CommandSender runner = asConsole ? Bukkit.getConsoleSender() : sender;
            if (!CommandTemplate.dispatch(runner, line)) {
                sender.sendMessage(CommandMessages.render(registry.getLanguageManager(), sender instanceof Player player ? player : null,
                        "Player.Commands.AliasFailed", "%prefix% &9> &cCould not run /%command%.", Map.of("%command%", line)));
            }
            return true;
        });
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
        if (template != null) {
            String target = template.commandName();
            if (target.equalsIgnoreCase(name) || aliases.stream().anyMatch(target::equalsIgnoreCase)) {
                throw new IllegalArgumentException("Alias '" + name + "' would run itself");
            }
            if (templateAsConsole && permission == null) {
                throw new IllegalStateException("Console alias '" + name + "' needs an explicit permission(...)");
            }
            if (info.isEmpty()) info = "Runs /" + template.template();
        }
        String resolvedPermission = permission != null ? permission : name.toLowerCase(Locale.ROOT);
        String resolvedUsage = usage != null ? usage
                : template != null ? ("/" + name + " " + template.usage()).trim() : generatedUsage();
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
        if (!subCommands.isEmpty()) {
            String label = subCommands.size() <= Argument.MAX_INLINE_OPTIONS
                    ? subCommands.stream().map(SubCommand::name).collect(java.util.stream.Collectors.joining("|"))
                    : "sub command";
            generated.append(' ').append(Argument.bracket(label, root.handler == null));
        }
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
