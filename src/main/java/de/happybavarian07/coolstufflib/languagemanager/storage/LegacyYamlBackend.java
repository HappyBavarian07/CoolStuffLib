package de.happybavarian07.coolstufflib.languagemanager.storage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** The layout before 3.0.0: one {@code languages/<lang>.yml} file per language. */
public final class LegacyYamlBackend implements LanguageBackend {
    public static final String ID = "yaml-legacy";
    private final File folder;

    public LegacyYamlBackend(File folder) {
        this.folder = folder;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public Set<String> languages() {
        Set<String> names = new TreeSet<>();
        File[] files = folder.listFiles(file -> file.isFile() && file.getName().endsWith(".yml") && !file.getName().startsWith("_"));
        if (files != null) for (File file : files) names.add(file.getName().substring(0, file.getName().length() - 4));
        return names;
    }

    @Override
    public ReadResult read(String language) {
        File file = new File(folder, language + ".yml");
        if (!file.isFile()) return ReadResult.empty();
        String label = language + ".yml";
        try {
            YamlEntries.Parsed parsed = YamlEntries.read(Files.readString(file.toPath(), StandardCharsets.UTF_8), label, key -> key);
            Map<String, LanguageEntry> entries = new LinkedHashMap<>();
            for (LanguageEntry entry : parsed.entries()) entries.put(entry.key(), entry);
            return new ReadResult(entries, parsed.sectionComments(), List.of());
        } catch (LanguageParseException e) {
            return new ReadResult(Map.of(), Map.of(), List.of(e.problem()));
        } catch (IOException e) {
            return new ReadResult(Map.of(), Map.of(), List.of(new LanguageProblem(label, 0, "Could not read: " + e.getMessage())));
        }
    }

    @Override
    public List<LanguageProblem> write(String language, Collection<LanguageEntry> entries, Map<String, String> sectionComments) {
        List<Map.Entry<String, LanguageEntry>> list = new ArrayList<>();
        for (LanguageEntry entry : entries) list.add(Map.entry(entry.key(), entry));
        return YamlEntries.merge(new File(folder, language + ".yml").toPath(), language + ".yml", list, sectionComments::get);
    }

    @Override
    public String fileOf(String language, String key) {
        return language + ".yml";
    }
}
