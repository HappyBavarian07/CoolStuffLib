package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>Which file of the split layout a key lives in: header keys in {@code language.yml}, {@code Messages.<Section>.*}
 * in {@code messages/<Section>.yml}, {@code Items.<Section>.*} in {@code items/<Section>.yml}, two-part keys in
 * {@code messages/_root.yml} / {@code items/_root.yml}, {@code MenuTitles.*} in {@code titles.yml}, anything else in
 * {@code other.yml}.</p>
 */
public final class SplitRule {
    public static final Set<String> HEADER_KEYS = Set.of("LanguageFullName", "LanguageVersion", "LanguageUUID",
            "LanguageDescription", "LanguageParent", "LanguageLocales");
    private static final Pattern FILE_SAFE = Pattern.compile("[A-Za-z0-9_\\-]+");

    public record Target(String file, String relativeKey) {
    }

    private SplitRule() {
    }

    public static Target of(String key) {
        String[] parts = key.split("\\.", 3);
        String root = parts[0];
        if (HEADER_KEYS.contains(root)) return new Target("language.yml", key);
        String folder = root.equals("Messages") ? "messages" : root.equals("Items") ? "items" : null;
        if (folder != null && parts.length == 2) return new Target(folder + "/_root.yml", parts[1]);
        if (folder != null && parts.length == 3 && FILE_SAFE.matcher(parts[1]).matches() && !parts[1].startsWith("_")) {
            return new Target(folder + "/" + parts[1] + ".yml", parts[2]);
        }
        if (root.equals("MenuTitles") && parts.length >= 2) {
            return new Target("titles.yml", key.substring("MenuTitles.".length()));
        }
        return new Target("other.yml", key);
    }

    public static String fullKey(String file, String relativeKey) {
        if (file.equals("titles.yml")) return "MenuTitles." + relativeKey;
        String[] path = file.split("/");
        if (path.length == 2 && path[1].endsWith(".yml")) {
            String root = path[0].equals("messages") ? "Messages" : path[0].equals("items") ? "Items" : null;
            if (root != null) {
                String section = path[1].substring(0, path[1].length() - 4);
                return section.equals("_root") ? root + "." + relativeKey : root + "." + section + "." + relativeKey;
            }
        }
        return relativeKey;
    }

    /** The full section key a split file itself stands for ({@code messages/Player.yml} -> {@code Messages.Player}), or null when the file has no such section (root and other files). */
    public static @Nullable String sectionOf(String file) {
        String[] path = file.split("/");
        if (path.length != 2 || !path[1].endsWith(".yml")) return null;
        String root = path[0].equals("messages") ? "Messages" : path[0].equals("items") ? "Items" : null;
        if (root == null) return null;
        String section = path[1].substring(0, path[1].length() - 4);
        return section.equals("_root") ? null : root + "." + section;
    }
}
