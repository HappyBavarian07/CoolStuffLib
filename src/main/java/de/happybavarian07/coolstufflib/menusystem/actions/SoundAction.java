package de.happybavarian07.coolstufflib.menusystem.actions;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Plays a sound and optionally executes a wrapped action.
 */
public class SoundAction implements MenuAction {
    private final Sound sound;
    private final float volume;
    private final float pitch;
    private final MenuAction action;

    public SoundAction(Sound sound, float volume, float pitch) {
        this(sound, volume, pitch, null);
    }

    public SoundAction(Sound sound, float volume, float pitch, MenuAction action) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.action = action;
    }

    @Override
    public void execute(Player player, InventoryClickEvent event) {
        if (sound != null) player.playSound(player.getLocation(), sound, volume, pitch);
        if (action != null) action.execute(player, event);
    }
}
