package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public record PrepareResult(String language, @Nullable MigrationReport migration, List<String> addedKeys,
                            List<LanguageProblem> problems) {
}
