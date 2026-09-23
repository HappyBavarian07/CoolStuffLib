package de.happybavarian07.coolstufflib.commandmanagement;

import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.languagemanager.PlaceholderType;
import de.happybavarian07.coolstufflib.utils.Utils;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * <p>Renders {@code Messages.<path>} with placeholders, or the English fallback when a consumer's
 * language file does not have the key yet.</p>
 */
final class CommandMessages {
    private CommandMessages() {
    }

    static String render(LanguageManager lgm, Player player, String path, String fallback, Map<String, String> placeholders) {
        if (lgm != null) {
            placeholders.forEach((key, value) -> lgm.addPlaceholder(PlaceholderType.MESSAGE, key, value, false));
            String message = lgm.getMessageOrDefault(path, player, null, true);
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
