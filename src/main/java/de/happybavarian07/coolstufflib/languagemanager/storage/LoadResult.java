package de.happybavarian07.coolstufflib.languagemanager.storage;

import java.util.List;
import java.util.Map;

/** One language after merging: {@code own} (owner files) over {@code defaults} (jar) gives {@code merged}. */
public record LoadResult(String language, String backend, Map<String, LanguageEntry> merged, Map<String, LanguageEntry> own,
                         Map<String, LanguageEntry> defaults, List<LanguageProblem> problems) {
}
