package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public record MigrationReport(String language, int entries, List<String> differences, List<LanguageProblem> problems,
                              @Nullable File backup) {
    public boolean ok() {
        return differences.isEmpty() && problems.isEmpty();
    }

    public String summary() {
        return language + ": " + entries + " keys, " + differences.size() + " differences"
                + (differences.isEmpty() ? "" : " " + differences.subList(0, Math.min(5, differences.size())))
                + (problems.isEmpty() ? "" : ", problems: " + problems);
    }
}
