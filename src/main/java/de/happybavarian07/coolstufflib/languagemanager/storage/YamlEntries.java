package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;

final class YamlEntries {
    record Parsed(List<LanguageEntry> entries, Map<String, String> sectionComments) {
    }

    private YamlEntries() {
    }

    static Yaml yaml() {
        return new Yaml(new SafeConstructor(new LoaderOptions()));
    }

    static Parsed read(String text, String file, UnaryOperator<String> toFullKey) throws LanguageParseException {
        Object root;
        try {
            root = yaml().load(text);
        } catch (YAMLException e) {
            throw new LanguageParseException(problem(file, e));
        }
        if (root == null) return new Parsed(List.of(), Map.of());
        if (!(root instanceof Map<?, ?> map)) {
            throw new LanguageParseException(new LanguageProblem(file, 1, "The file must contain keys, not a single value"));
        }
        YamlDocument doc = YamlDocument.parse(text);
        List<LanguageEntry> entries = new ArrayList<>();
        Map<String, String> sectionComments = new LinkedHashMap<>();
        flatten(map, "", doc, file, toFullKey, entries, sectionComments);
        return new Parsed(entries, sectionComments);
    }

    private static void flatten(Map<?, ?> map, String prefix, YamlDocument doc, String file, UnaryOperator<String> toFullKey,
                                List<LanguageEntry> entries, Map<String, String> sectionComments) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String path = prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> child) {
                String comment = doc.commentOf(path);
                if (comment != null) sectionComments.put(toFullKey.apply(path), comment);
                flatten(child, path, doc, file, toFullKey, entries, sectionComments);
            } else if (value != null) {
                int line = doc.node(path).map(node -> node.line() + 1).orElse(0);
                entries.add(new LanguageEntry(toFullKey.apply(path), value, doc.commentOf(path), new LanguageEntry.Location(file, line)));
            }
        }
    }

    static LanguageProblem problem(String file, YAMLException e) {
        int line = e instanceof MarkedYAMLException marked && marked.getProblemMark() != null
                ? marked.getProblemMark().getLine() + 1 : 0;
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage().lines().findFirst().orElse("");
        return new LanguageProblem(file, line, message);
    }

    /**
     * A file's header is comment lines at the very top followed by one empty line: the comment of the section
     * that the file itself stands for (e.g. {@code messages/Player.yml} for {@code Messages.Player}). Returns
     * the comment text (without '#' and one following space, lines joined with '\n'), or null when the file has
     * no such header.
     */
    static @Nullable String readHeader(String text) {
        String[] lines = text.split("\r\n|\n", -1);
        List<String> comment = new ArrayList<>();
        int i = 0;
        for (; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (!trimmed.startsWith("#")) break;
            String rest = trimmed.substring(1);
            comment.add(rest.startsWith(" ") ? rest.substring(1) : rest);
        }
        if (comment.isEmpty() || i >= lines.length || !lines[i].trim().isEmpty()) return null;
        return String.join("\n", comment);
    }

    /** The text of a new file's header: the comment lines, then one empty line. */
    static String header(String comment, String newline) {
        StringBuilder out = new StringBuilder();
        for (String line : comment.split("\n", -1)) {
            out.append("#").append(line.isEmpty() ? "" : " ").append(line).append(newline);
        }
        return out.append(newline).toString();
    }

    static List<LanguageProblem> merge(Path path, String file, List<Map.Entry<String, LanguageEntry>> entries,
                                       Function<String, String> sectionComment) {
        List<LanguageProblem> problems = new ArrayList<>();
        String before;
        try {
            before = Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            problems.add(new LanguageProblem(file, 0, "Could not read: " + e.getMessage()));
            return problems;
        }
        YamlDocument doc = YamlDocument.parse(before);
        for (Map.Entry<String, LanguageEntry> entry : entries) {
            try {
                doc.set(entry.getKey(), entry.getValue().value(), entry.getValue().comment(), sectionComment);
            } catch (IllegalStateException e) {
                problems.add(new LanguageProblem(file, 0, e.getMessage()));
            }
        }
        String after = doc.text();
        if (after.equals(before)) return problems;
        try {
            yaml().load(after);
        } catch (YAMLException e) {
            problems.add(new LanguageProblem(file, 0, "Not written, the result would not be valid YAML: " + problem(file, e).message()));
            return problems;
        }
        try {
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(tmp, after, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            problems.add(new LanguageProblem(file, 0, "Could not write: " + e.getMessage()));
        }
        return problems;
    }
}
