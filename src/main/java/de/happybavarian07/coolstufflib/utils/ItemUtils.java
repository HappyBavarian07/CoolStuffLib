package de.happybavarian07.coolstufflib.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.UUID;

/**
 * <p>Utilities for creating and manipulating ItemStacks, including custom player heads.</p>
 */
public final class ItemUtils {

    private ItemUtils() {}

    /**
     * <p>Creates a player skull item with a custom head texture or player name.</p>
     * <pre><code>ItemStack skull = ItemUtils.createSkull("textureOrName", "Display Name", true);</code></pre>
     *
     * @param headValue the head texture string or player name
     * @param name      the display name for the skull item
     * @param isTexture true if headValue is a texture string, false if a player name
     * @return the created ItemStack
     */
    public static ItemStack createSkull(String headValue, String name, boolean isTexture) {
        Material material;
        try {
            material = Material.matchMaterial("PLAYER_HEAD");
        } catch (Exception e) {
            material = Material.matchMaterial("SKULL_ITEM");
        }
        
        ItemStack head = new ItemStack(Objects.requireNonNullElse(material, Material.PLAYER_HEAD), 1);
        if (headValue == null || headValue.isEmpty()) return head;

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        try {
            Head headEnum = Head.valueOf(headValue);
            return headEnum.getAsItem();
        } catch (IllegalArgumentException ignored) {}

        meta.setDisplayName(ChatUtils.chat(name));
        
        if (!isTexture) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(headValue));
            head.setItemMeta(meta);
            return head;
        }

        return applyItemStackToProfile(headValue, head, meta);
    }

    @NotNull
    public static ItemStack applyItemStackToProfile(String headValue, ItemStack head, SkullMeta meta) {
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "CustomHead");
        try {
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL("https://textures.minecraft.net/texture/" + headValue));
            profile.setTextures(textures);
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
        meta.setOwnerProfile(profile);
        head.setItemMeta(meta);
        return head;
    }

    /**
     * <p>Creates a skull with a head texture and a name from a Head enum.</p>
     *
     * @param headTexture the Head enum value
     * @param name        the display name
     * @return the created skull ItemStack
     */
    public static ItemStack createSkull(Head headTexture, String name) {
        if (headTexture == null) return new ItemStack(Material.PLAYER_HEAD);
        return createSkull(headTexture.getTexture(), name, true);
    }
}
