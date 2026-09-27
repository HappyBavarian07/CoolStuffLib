package de.happybavarian07.coolstufflib.languagemanager.storage;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Where the texts of a plugin's languages are stored. Implementations never delete keys. */
public interface LanguageBackend {
    String id();

    Set<String> languages();

    /** Reads everything it can; broken files are reported in the result instead of throwing. */
    ReadResult read(String language);

    /** Adds or updates the entries. {@code sectionComments} maps full section keys to comments for new sections. */
    List<LanguageProblem> write(String language, Collection<LanguageEntry> entries, Map<String, String> sectionComments);

    default List<LanguageProblem> write(String language, Collection<LanguageEntry> entries) {
        return write(language, entries, Map.of());
    }

    boolean isWritable();

    /** The file label a key of this language belongs to, as used in {@link LanguageProblem#file()}. */
    default String fileOf(String language, String key) {
        return language;
    }
}
