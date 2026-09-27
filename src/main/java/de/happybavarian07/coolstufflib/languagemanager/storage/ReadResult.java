package de.happybavarian07.coolstufflib.languagemanager.storage;

import java.util.*;

public record ReadResult(Map<String, LanguageEntry> entries, Map<String, String> sectionComments, List<LanguageProblem> problems) {
    public static ReadResult empty() {
        return new ReadResult(Map.of(), Map.of(), List.of());
    }

    public Set<String> failedFiles() {
        Set<String> files = new HashSet<>();
        for (LanguageProblem problem : problems) files.add(problem.file());
        return files;
    }
}
