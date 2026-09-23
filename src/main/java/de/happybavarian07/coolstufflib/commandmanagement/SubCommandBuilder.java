package de.happybavarian07.coolstufflib.commandmanagement;

import org.bukkit.command.CommandSender;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>Builds a {@link SubCommand} from lambdas, for commands too small to deserve a class.</p>
 *
 * <pre><code>
 * // inside CommandManager.setup()
 * registerSubCommand(subCommand("heal")
 *         .aliases("h")
 *         .arguments(Argument.player("target").optional())
 *         .player((player, args) -&gt; heal(args.has(0) ? args.onlinePlayer(0) : player))
 *         .build());
 * </code></pre>
 */
public final class SubCommandBuilder {
    final String mainCommandName;
    final String name;
    String[] aliases = new String[0];
    String info;
    String syntax;
    String permission;
    boolean autoRegisterPermission = true;
    boolean playerOnly;
    boolean opRequired;
    boolean strictArguments;
    boolean senderTypeSpecificSubArgs;
    Integer minArgs;
    Integer maxArgs;
    long cooldownMillis;
    boolean confirm;
    boolean async;
    final List<SubCommand> children = new ArrayList<>();
    List<Argument> arguments = List.of();
    CommandHandler handler;

    public SubCommandBuilder(String mainCommandName, String name) {
        this.mainCommandName = Objects.requireNonNull(mainCommandName);
        this.name = Objects.requireNonNull(name);
    }

    public SubCommandBuilder aliases(String... aliases) {
        this.aliases = aliases.clone();
        return this;
    }

    public SubCommandBuilder info(String info) {
        this.info = info;
        return this;
    }

    public SubCommandBuilder syntax(String syntax) {
        this.syntax = syntax;
        return this;
    }

    public SubCommandBuilder permission(String permission) {
        this.permission = permission;
        return this;
    }

    public SubCommandBuilder autoRegisterPermission(boolean autoRegisterPermission) {
        this.autoRegisterPermission = autoRegisterPermission;
        return this;
    }

    public SubCommandBuilder playerOnly() {
        this.playerOnly = true;
        return this;
    }

    /** Ops may run the command even without its permission (same meaning as {@link CommandData#opRequired()}). */
    public SubCommandBuilder opRequired() {
        this.opRequired = true;
        return this;
    }

    /** Reject arguments that are not among the completions of their position. */
    public SubCommandBuilder strictArguments() {
        this.strictArguments = true;
        return this;
    }

    public SubCommandBuilder senderTypeSpecificSubArgs() {
        this.senderTypeSpecificSubArgs = true;
        return this;
    }

    /** Overrides the minimum derived from the arguments. */
    public SubCommandBuilder minArgs(int minArgs) {
        this.minArgs = minArgs;
        return this;
    }

    /** Overrides the maximum derived from the arguments. */
    public SubCommandBuilder maxArgs(int maxArgs) {
        this.maxArgs = maxArgs;
        return this;
    }

    /** Per-sender cooldown. */
    public SubCommandBuilder cooldown(Duration cooldown) {
        this.cooldownMillis = cooldown.toMillis();
        return this;
    }

    /** Require running the same command twice within {@link SubCommand#CONFIRMATION_WINDOW_MILLIS}. */
    public SubCommandBuilder confirm() {
        this.confirm = true;
        return this;
    }

    /** Run off the main thread. */
    public SubCommandBuilder async() {
        this.async = true;
        return this;
    }

    /** Nested sub commands: {@code /main this child ...}. */
    public SubCommandBuilder sub(SubCommand... children) {
        this.children.addAll(List.of(children));
        return this;
    }

    public SubCommandBuilder arguments(Argument... arguments) {
        this.arguments = List.of(arguments);
        return this;
    }

    public SubCommandBuilder executes(CommandHandler handler) {
        this.handler = handler;
        return this;
    }

    /** Player-only logic; always counts as handled. */
    public SubCommandBuilder player(CommandHandler.PlayerHandler handler) {
        this.playerOnly = true;
        this.handler = (sender, args) -> {
            handler.handle(args.player(), args);
            return true;
        };
        return this;
    }

    public SubCommand build() {
        if (handler == null && children.isEmpty()) {
            throw new IllegalStateException("Sub command '" + name + "' needs executes(...), player(...) or sub(...)");
        }
        BuiltSubCommand built = new BuiltSubCommand(this);
        for (SubCommand child : children) built.addChild(child);
        return built;
    }

    private static final class BuiltSubCommand extends SubCommand {
        private final SubCommandBuilder spec;

        private BuiltSubCommand(SubCommandBuilder spec) {
            super(spec.mainCommandName);
            this.spec = spec;
        }

        @Override
        public String name() {
            return spec.name;
        }

        @Override
        public String[] aliases() {
            return spec.aliases.clone();
        }

        @Override
        public String info() {
            return spec.info != null ? spec.info : super.info();
        }

        @Override
        public String syntax() {
            return spec.syntax != null ? spec.syntax : super.syntax();
        }

        @Override
        public String permissionAsString() {
            return spec.permission != null ? spec.permission : super.permissionAsString();
        }

        @Override
        public boolean autoRegisterPermission() {
            return spec.autoRegisterPermission;
        }

        @Override
        public boolean isPlayerRequired() {
            return spec.playerOnly;
        }

        @Override
        public boolean isOpRequired() {
            return spec.opRequired;
        }

        @Override
        public boolean allowOnlySubCommandArgsThatFitToSubArgs() {
            return spec.strictArguments;
        }

        @Override
        public boolean senderTypeSpecificSubArgs() {
            return spec.senderTypeSpecificSubArgs;
        }

        @Override
        public int minArgs() {
            return spec.minArgs != null ? spec.minArgs : super.minArgs();
        }

        @Override
        public int maxArgs() {
            return spec.maxArgs != null ? spec.maxArgs : super.maxArgs();
        }

        @Override
        public long cooldownMillis() {
            return spec.cooldownMillis;
        }

        @Override
        public boolean requiresConfirmation() {
            return spec.confirm;
        }

        @Override
        public boolean isAsync() {
            return spec.async;
        }

        @Override
        public List<Argument> arguments() {
            return spec.arguments;
        }

        @Override
        public boolean execute(CommandSender sender, CommandArgs args) {
            return spec.handler != null && spec.handler.handle(sender, args);
        }
    }
}
