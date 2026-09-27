package de.happybavarian07.coolstufflib.languagemanager.storage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/** The default layout: one folder per language, split into small files by {@link SplitRule}. */
public final class SplitYamlBackend implements LanguageBackend {
    public static final String ID = "yaml-split";
    private final File folder;

    public SplitYamlBackend(File folder) {
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
        File[] dirs = folder.listFiles(file -> file.isDirectory() && !file.getName().startsWith("_") && !file.getName().startsWith("."));
        if (dirs != null) for (File dir : dirs) names.add(dir.getName());
        return names;
    }

    @Override
    public ReadResult read(String language) {
        Path root = folder.toPath().resolve(language);
        if (!Files.isDirectory(root)) return ReadResult.empty();
        Map<String, LanguageEntry> entries = new LinkedHashMap<>();
        Map<String, String> sectionComments = new LinkedHashMap<>();
        List<LanguageProblem> problems = new ArrayList<>();
        List<Path> files;
        try (Stream<Path> walk = Files.walk(root)) {
            files = walk.filter(path -> path.toString().endsWith(".yml")).sorted().toList();
        } catch (IOException e) {
            return new ReadResult(Map.of(), Map.of(), List.of(new LanguageProblem(language, 0, "Could not list files: " + e.getMessage())));
        }
        for (Path file : files) {
            String relative = root.relativize(file).toString().replace('\\', '/');
            String label = language + "/" + relative;
            try {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                YamlEntries.Parsed parsed = YamlEntries.read(text, label, key -> SplitRule.fullKey(relative, key));
                for (LanguageEntry entry : parsed.entries()) entries.put(entry.key(), entry);
                sectionComments.putAll(parsed.sectionComments());
                String section = SplitRule.sectionOf(relative);
                if (section != null) {
                    String header = YamlEntries.readHeader(text);
                    if (header != null) sectionComments.put(section, header);
                }
            } catch (LanguageParseException e) {
                problems.add(e.problem());
            } catch (IOException e) {
                problems.add(new LanguageProblem(label, 0, "Could not read: " + e.getMessage()));
            }
        }
        return new ReadResult(entries, sectionComments, problems);
    }

    @Override
    public List<LanguageProblem> write(String language, Collection<LanguageEntry> entries, Map<String, String> sectionComments) {
        Map<String, List<Map.Entry<String, LanguageEntry>>> byFile = new LinkedHashMap<>();
        for (LanguageEntry entry : entries) {
            SplitRule.Target target = SplitRule.of(entry.key());
            byFile.computeIfAbsent(target.file(), file -> new ArrayList<>()).add(Map.entry(target.relativeKey(), entry));
        }
        List<LanguageProblem> problems = new ArrayList<>();
        for (Map.Entry<String, List<Map.Entry<String, LanguageEntry>>> file : byFile.entrySet()) {
            String name = file.getKey();
            Path path = folder.toPath().resolve(language).resolve(name);
            String label = language + "/" + name;
            String section = SplitRule.sectionOf(name);
            if (section != null && !Files.exists(path) && sectionComments.containsKey(section)) {
                try {
                    Files.createDirectories(path.getParent());
                    Files.writeString(path, YamlEntries.header(sectionComments.get(section), "\n"), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    problems.add(new LanguageProblem(label, 0, "Could not write: " + e.getMessage()));
                    continue;
                }
            }
            problems.addAll(YamlEntries.merge(path, label, file.getValue(),
                    relative -> sectionComments.get(SplitRule.fullKey(name, relative))));
        }
        return problems;
    }

    @Override
    public String fileOf(String language, String key) {
        return language + "/" + SplitRule.of(key).file();
    }
}
