package de.happybavarian07.coolstufflib.commandmanagement;

import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.languagemanager.PlaceholderType;
import de.happybavarian07.coolstufflib.utils.Utils;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * <p>Thrown by {@link CommandArgs} for missing or invalid input. The command manager catches it and sends
 * the localized message ({@code Messages.<messagePath>}), or the English fallback if the language file
 * has no such key.</p>
 */
public class CommandArgumentException extends RuntimeException {
    private final String messagePath;
    private final String fallback;
    private final Map<String, String> placeholders;

    public CommandArgumentException(String messagePath, String fallback, Map<String, String> placeholders) {
        super(messagePath + " " + placeholders);
        this.messagePath = messagePath;
        this.fallback = fallback;
        this.placeholders = Map.copyOf(placeholders);
    }

    public String getMessagePath() {
        return messagePath;
    }

    public Map<String, String> getPlaceholders() {
        return placeholders;
    }

    public String render(LanguageManager lgm, Player player) {
        if (lgm != null) {
            placeholders.forEach((key, value) -> lgm.addPlaceholder(PlaceholderType.MESSAGE, key, value, false));
            String message = lgm.getMessageOrDefault(messagePath, player, null, true);
            placeholders.keySet().forEach(key -> lgm.removePlaceholder(PlaceholderType.MESSAGE, key));
            if (message != null) return message;
        }
        String text = fallback;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        String prefix = lgm == null || lgm.getPrefix() == null ? "" : lgm.getPrefix();
        return Utils.format(player, text, prefix);
    }
}
