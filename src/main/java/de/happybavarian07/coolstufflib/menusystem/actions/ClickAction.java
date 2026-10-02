package de.happybavarian07.coolstufflib.menusystem.actions;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Routes a click to a different action depending on the click type.
 * <pre><code>
 * registerButton(10, item, ClickAction.of()
 *         .left(openDetails)
 *         .right(removeEntry.confirm("Remove?"))
 *         .shift(copyEntry));
 * </code></pre>
 * A shift click uses {@link #shiftLeft(MenuAction)} or {@link #shiftRight(MenuAction)} if set for its side, then the
 * shift action if set, otherwise falls through to left/right.
 * Clicks without a matching action use {@link #otherwise(MenuAction)} or do nothing.
 */
public final class ClickAction implements MenuAction {
    private MenuAction left;
    private MenuAction right;
    private MenuAction shift;
    private MenuAction shiftLeft;
    private MenuAction shiftRight;
    private MenuAction middle;
    private MenuAction fallback;

    private ClickAction() {
    }

    public static ClickAction of() {
        return new ClickAction();
    }

    public ClickAction left(MenuAction action) {
        this.left = action;
        return this;
    }

    public ClickAction right(MenuAction action) {
        this.right = action;
        return this;
    }

    public ClickAction shift(MenuAction action) {
        this.shift = action;
        return this;
    }

    public ClickAction shiftLeft(MenuAction action) {
        this.shiftLeft = action;
        return this;
    }

    public ClickAction shiftRight(MenuAction action) {
        this.shiftRight = action;
        return this;
    }

    public ClickAction middle(MenuAction action) {
        this.middle = action;
        return this;
    }

    public ClickAction otherwise(MenuAction action) {
        this.fallback = action;
        return this;
    }

    @Override
    public void execute(Player player, InventoryClickEvent event) {
        MenuAction target = select(event == null ? null : event.getClick());
        if (target != null) target.execute(player, event);
    }

    private MenuAction select(ClickType click) {
        if (click == null) return left != null ? left : fallback;
        if (click == ClickType.SHIFT_LEFT && shiftLeft != null) return shiftLeft;
        if (click == ClickType.SHIFT_RIGHT && shiftRight != null) return shiftRight;
        if (click.isShiftClick() && shift != null) return shift;
        if (click == ClickType.MIDDLE && middle != null) return middle;
        if (click.isLeftClick() && left != null) return left;
        if (click.isRightClick() && right != null) return right;
        return fallback;
    }
}
