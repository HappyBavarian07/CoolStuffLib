package de.happybavarian07.coolstufflib.commandmanagement;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>A command line with placeholders, used by command aliases and menu command actions.</p>
 * <ul>
 * <li>{@code {player}} sender name, {@code {uuid}} sender UUID (empty for the console)</li>
 * <li>{@code {0}}, {@code {1}}, ... positional arguments (required)</li>
 * <li>{@code {args}} all arguments, {@code {args:1}} all arguments from index 1 (both optional)</li>
 * </ul>
 * <pre><code>new CommandTemplate("gamemode creative {0}").resolve(sender, new String[]{"Steve"}) // "gamemode creative Steve"</code></pre>
 */
public final class CommandTemplate {
    /** Alias chains deeper than this are stopped to prevent loops. */
    public static final int MAX_DISPATCH_DEPTH = 5;
    private static final Pattern TOKEN = Pattern.compile("\\{(player|uuid|args(?::(\\d+))?|(\\d+))}");
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private final String template;
    private final int requiredArgs;
    private final boolean acceptsExtraArgs;

    public CommandTemplate(String template) {
        String trimmed = template == null ? "" : template.trim();
        this.template = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        if (this.template.isEmpty()) throw new IllegalArgumentException("Command template is empty");
        int highest = -1;
        boolean extra = false;
        Matcher matcher = TOKEN.matcher(this.template);
        while (matcher.find()) {
            if (matcher.group(3) != null) highest = Math.max(highest, Integer.parseInt(matcher.group(3)));
            if (matcher.group(1).startsWith("args")) extra = true;
        }
        this.requiredArgs = highest + 1;
        this.acceptsExtraArgs = extra;
    }

    public String template() {
        return template;
    }

    /** Number of positional arguments the template needs ({@code {2}} means 3). */
    public int requiredArgs() {
        return requiredArgs;
    }

    /** Whether {@code {args}} / {@code {args:n}} take any further arguments. */
    public boolean acceptsExtraArgs() {
        return acceptsExtraArgs;
    }

    /** The command the template runs, e.g. {@code gamemode}. */
    public String commandName() {
        return template.split(" ", 2)[0];
    }

    /** {@code <arg1> <arg2> [args...]} for usage lines. */
    public String usage() {
        StringBuilder usage = new StringBuilder();
        for (int i = 1; i <= requiredArgs; i++) usage.append("<arg").append(i).append("> ");
        if (acceptsExtraArgs) usage.append("[args...]");
        return usage.toString().trim();
    }

    /**
     * Fills in the placeholders. A missing positional argument throws {@link CommandArgumentException}.
     */
    public String resolve(CommandSender sender, String[] args) {
        String[] values = args == null ? new String[0] : args;
        Matcher matcher = TOKEN.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String token = matcher.group(1);
            String replacement;
            if (token.equals("player")) {
                replacement = sender == null ? "" : sender.getName();
            } else if (token.equals("uuid")) {
                replacement = sender instanceof Entity entity ? entity.getUniqueId().toString() : "";
            } else if (token.startsWith("args")) {
                int from = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
                replacement = from >= values.length ? "" : String.join(" ", Arrays.copyOfRange(values, from, values.length));
            } else {
                int index = Integer.parseInt(matcher.group(3));
                if (index >= values.length) {
                    throw new CommandArgumentException("Player.Commands.MissingArgument", "%prefix% &9> &cMissing argument: %argument%",
                            Map.of("%argument%", "#" + (index + 1), "%value%", "", "%index%", String.valueOf(index + 1)));
                }
                replacement = values[index];
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString().trim().replaceAll(" {2,}", " ");
    }

    /**
     * <p>Runs {@code commandLine} as {@code sender}. Returns {@code false} if the command does not exist,
     * failed, or the alias chain got deeper than {@link #MAX_DISPATCH_DEPTH}.</p>
     */
    public static boolean dispatch(CommandSender sender, String commandLine) {
        int depth = DEPTH.get();
        if (depth >= MAX_DISPATCH_DEPTH) {
            CoolStuffLib.logError("Stopped command '" + commandLine + "'", new IllegalStateException(
                    "Command alias chain is deeper than " + MAX_DISPATCH_DEPTH + "; aliases probably call each other"));
            return false;
        }
        DEPTH.set(depth + 1);
        try {
            return Bukkit.dispatchCommand(sender, commandLine);
        } finally {
            if (depth == 0) DEPTH.remove();
            else DEPTH.set(depth);
        }
    }
}
