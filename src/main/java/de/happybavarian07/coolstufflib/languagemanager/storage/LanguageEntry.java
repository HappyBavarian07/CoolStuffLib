package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.jetbrains.annotations.Nullable;

/** One value of a language file: full dotted key, value (string, number, boolean or list), comment and origin. */
public record LanguageEntry(String key, Object value, @Nullable String comment, @Nullable Location origin) {
    public record Location(String file, int line) {
        @Override
        public String toString() {
            return line > 0 ? file + ":" + line : file;
        }
    }
}
