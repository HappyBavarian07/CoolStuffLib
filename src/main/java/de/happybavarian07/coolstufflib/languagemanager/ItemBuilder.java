package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/** <pre><code>lgm.item("StartMenu.HintItem").with("%x%", x).build(player);</code></pre> */
public final class ItemBuilder {
    private final LanguageManager lgm;
    private final String path;
    private final Map<String, Object> placeholders = new LinkedHashMap<>();
    private String language;

    ItemBuilder(LanguageManager lgm, String path) {
        this.lgm = lgm;
        this.path = path;
    }

    public ItemBuilder with(String key, Object value) {
        placeholders.put(key, value);
        return this;
    }

    public ItemBuilder with(Map<String, ?> values) {
        placeholders.putAll(values);
        return this;
    }

    public ItemBuilder lang(String language) {
        this.language = language;
        return this;
    }

    public ItemStack build(@Nullable Player viewer) {
        return lgm.renderItem(path, viewer, lgm.startFor(language, viewer, false), placeholders, false, null);
    }
}
