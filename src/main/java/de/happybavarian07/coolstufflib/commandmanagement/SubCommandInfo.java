package de.happybavarian07.coolstufflib.commandmanagement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Declares a sub command's metadata so it only has to implement its logic.</p>
 *
 * <pre><code>
 * &#64;SubCommandInfo(name = "heal", aliases = "h")
 * &#64;CommandData(playerRequired = true)
 * public class HealCommand extends SubCommand {
 *     public HealCommand() { super("admin"); }
 *
 *     &#64;Override
 *     public boolean execute(CommandSender sender, CommandArgs args) { ... }
 * }
 * </code></pre>
 *
 * <p>Empty values fall back to conventions: info and syntax from the language file keys
 * {@code Messages.Commands.<main>.<sub>.Info} / {@code .Syntax} (syntax otherwise generated from the
 * name and arguments), permission {@code <main>.<sub>} in lower case.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SubCommandInfo {
    String name();

    String[] aliases() default {};

    String info() default "";

    String syntax() default "";

    String permission() default "";

    boolean autoRegisterPermission() default true;

    /** Per-sender cooldown in milliseconds; 0 disables it. */
    long cooldownMillis() default 0;

    /** Require running the same command again within {@link SubCommand#CONFIRMATION_WINDOW_MILLIS} to execute it. */
    boolean confirm() default false;
}
