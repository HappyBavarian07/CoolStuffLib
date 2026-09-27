package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/** <pre><code>lgm.title("StartMenu").text(player);</code></pre> */
public final class TitleBuilder {
    private final LanguageManager lgm;
    private final String path;
    private final Map<String, Object> placeholders = new LinkedHashMap<>();
    private String language;

    TitleBuilder(LanguageManager lgm, String path) {
        this.lgm = lgm;
        this.path = path;
    }

    public TitleBuilder with(String key, Object value) {
        placeholders.put(key, value);
        return this;
    }

    public TitleBuilder lang(String language) {
        this.language = language;
        return this;
    }

    public String text(@Nullable Player viewer) {
        return lgm.renderTitle(path, viewer, lgm.startFor(language, viewer, false), placeholders);
    }
}
