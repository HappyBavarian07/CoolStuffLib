package de.happybavarian07.coolstufflib.menusystem.actions;/*
 * @Author HappyBavarian07
 * @Date 21.07.2024 | 12:33
 */

import de.happybavarian07.coolstufflib.menusystem.Menu;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.Supplier;

/**
 * Functional menu click action. Decorators can be chained:
 * <pre><code>
 * registerButton(13, stopItem, MenuAction.of((p, e) -&gt; Bukkit.shutdown())
 *         .confirm("Stop the server?")
 *         .cooldown(5000)
 *         .requires("adminpanel.server.stop"));
 * </code></pre>
 * Each decorator wraps everything before it, so the last one runs first: here the permission is
 * checked first, then the cooldown, and only then the confirmation menu opens.
 */
@FunctionalInterface
public interface MenuAction {
    /**
     * Executes the action.
     *
     * @param player clicker
     * @param event  click event, may be {@code null} when invoked programmatically
     */
    void execute(Player player, InventoryClickEvent event);

    /** Runs this action only if the player has {@code permission}, otherwise sends the no-permission message. */
    default MenuAction requires(String permission) {
        return new PermissionAction(permission, this);
    }

    /** Asks for confirmation in a {@link de.happybavarian07.coolstufflib.menusystem.misc.ConfirmationMenu} first. */
    default MenuAction confirm(String reason) {
        return new ConfirmationAction(reason, this);
    }

    /** Blocks repeated execution per player for {@code millis}. */
    default MenuAction cooldown(long millis) {
        return new CooldownAction(millis, this);
    }

    /** Blocks repeated execution per player for {@code millis} and tells the player why. */
    default MenuAction cooldown(long millis, String message) {
        return new CooldownAction(millis, this, message);
    }

    /** Plays {@code sound} before running this action. */
    default MenuAction withSound(Sound sound) {
        return new SoundAction(sound, 1f, 1f, this);
    }

    /** Runs {@code next} after this action. */
    default MenuAction then(MenuAction next) {
        return new CompositeAction(this, next);
    }

    /** Closes the inventory after this action. */
    default MenuAction thenClose() {
        return then(close());
    }

    /** Returns to the parent menu (or closes) after this action. */
    default MenuAction thenBack() {
        return then(back());
    }

    /** Lets a lambda use the decorators: {@code MenuAction.of((p, e) -> ...).requires("perm")}. */
    static MenuAction of(MenuAction action) {
        return action;
    }

    /** An action that does nothing, e.g. for info items. */
    static MenuAction none() {
        return (player, event) -> {
        };
    }

    /** Closes the player's inventory. */
    static MenuAction close() {
        return (player, event) -> player.closeInventory();
    }

    /** Reopens the parent of the clicked menu, or closes the inventory if there is none. */
    static MenuAction back() {
        return (player, event) -> {
            if (event != null && event.getInventory().getHolder() instanceof Menu menu) {
                menu.closeAndReturnOrClose();
            } else {
                player.closeInventory();
            }
        };
    }

    /** Builds and opens a menu only when clicked. */
    static MenuAction open(Supplier<? extends Menu> menu) {
        return (player, event) -> menu.get().open();
    }
}
