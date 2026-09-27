package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>One message with its own placeholders. Nothing is stored in the language manager, so it can be used from
 * any thread.</p>
 * <pre><code>lgm.message("Player.General.NoPermissions").with("%target%", name).send(sender);</code></pre>
 */
public final class MessageBuilder {
    private final LanguageManager lgm;
    private final String path;
    private final Map<String, Object> placeholders = new LinkedHashMap<>();
    private Long count;
    private String language;

    MessageBuilder(LanguageManager lgm, String path) {
        this.lgm = lgm;
        this.path = path;
    }

    public MessageBuilder with(String key, Object value) {
        placeholders.put(key, value);
        return this;
    }

    public MessageBuilder with(Map<String, ?> values) {
        placeholders.putAll(values);
        return this;
    }

    /** Picks the {@code zero}/{@code one}/{@code other} variant and sets {@code %count%}. */
    public MessageBuilder count(long count) {
        this.count = count;
        return this;
    }

    /** Renders in this language instead of the viewer's. */
    public MessageBuilder lang(String language) {
        this.language = language;
        return this;
    }

    public String text(@Nullable Player viewer) {
        return lgm.renderMessage(path, viewer, lgm.startFor(language, viewer, true), placeholders, count, false);
    }

    public List<String> lines(@Nullable Player viewer) {
        return lgm.renderLines(path, viewer, lgm.startFor(language, viewer, true), placeholders, count);
    }

    /** Sends the chat text and, for rich entries, the action bar, title and sound. */
    public void send(CommandSender sender) {
        Player viewer = sender instanceof Player player ? player : null;
        lgm.send(path, sender, lgm.startFor(language, viewer, true), placeholders, count);
    }
}
