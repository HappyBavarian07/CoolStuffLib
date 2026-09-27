package de.happybavarian07.coolstufflib.languagemanager.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/** Read-only defaults shipped in the plugin jar, in the split layout or as single files. */
public final class ClasspathLanguageSource implements LanguageBackend {
    public static final String ID = "jar";
    private final ClassLoader loader;
    private final String directory;

    public ClasspathLanguageSource(ClassLoader loader, String directory) {
        this.loader = loader;
        this.directory = directory.endsWith("/") ? directory.substring(0, directory.length() - 1) : directory;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public Set<String> languages() {
        Set<String> names = new TreeSet<>();
        for (String path : list(directory)) {
            int slash = path.indexOf('/');
            if (slash > 0) names.add(path.substring(0, slash));
            else if (path.endsWith(".yml")) names.add(path.substring(0, path.length() - 4));
        }
        return names;
    }

    @Override
    public ReadResult read(String language) {
        List<String> splitFiles = list(directory + "/" + language).stream().filter(path -> path.endsWith(".yml")).toList();
        Map<String, LanguageEntry> entries = new LinkedHashMap<>();
        Map<String, String> sectionComments = new LinkedHashMap<>();
        List<LanguageProblem> problems = new ArrayList<>();
        if (!splitFiles.isEmpty()) {
            for (String relative : splitFiles) {
                String resource = directory + "/" + language + "/" + relative;
                readInto(resource, key -> SplitRule.fullKey(relative, key), entries, sectionComments, problems);
                String section = SplitRule.sectionOf(relative);
                if (section != null) {
                    String text = text(resource);
                    if (text != null) {
                        String header = YamlEntries.readHeader(text);
                        if (header != null) sectionComments.put(section, header);
                    }
                }
            }
        } else {
            readInto(directory + "/" + language + ".yml", key -> key, entries, sectionComments, problems);
        }
        return new ReadResult(entries, sectionComments, problems);
    }

    private void readInto(String resource, java.util.function.UnaryOperator<String> toFullKey, Map<String, LanguageEntry> entries,
                          Map<String, String> sectionComments, List<LanguageProblem> problems) {
        String text = text(resource);
        if (text == null) return;
        try {
            YamlEntries.Parsed parsed = YamlEntries.read(text, "jar:" + resource, toFullKey);
            for (LanguageEntry entry : parsed.entries()) entries.put(entry.key(), entry);
            sectionComments.putAll(parsed.sectionComments());
        } catch (LanguageParseException e) {
            problems.add(e.problem());
        }
    }

    @Override
    public List<LanguageProblem> write(String language, Collection<LanguageEntry> entries, Map<String, String> sectionComments) {
        return List.of(new LanguageProblem("jar:" + directory, 0, "The jar is read-only"));
    }

    private String text(String resource) {
        URL url = loader.getResource(resource);
        if (url == null) return null;
        try {
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            try (InputStream in = connection.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            return null;
        }
    }

    /** Files below {@code dir} (recursive), as paths relative to it; works for folders and jars. */
    private List<String> list(String dir) {
        URL url = loader.getResource(dir);
        if (url == null) return List.of();
        try {
            if ("file".equals(url.getProtocol())) {
                Path root = Path.of(url.toURI());
                if (!Files.isDirectory(root)) return List.of();
                try (Stream<Path> walk = Files.walk(root)) {
                    return walk.filter(Files::isRegularFile).map(path -> root.relativize(path).toString().replace('\\', '/')).sorted().toList();
                }
            }
            if ("jar".equals(url.getProtocol())) {
                JarURLConnection connection = (JarURLConnection) url.openConnection();
                connection.setUseCaches(false);
                String prefix = dir + "/";
                try (JarFile jar = connection.getJarFile()) {
                    return jar.stream().map(JarEntry::getName)
                            .filter(name -> name.startsWith(prefix) && !name.endsWith("/"))
                            .map(name -> name.substring(prefix.length())).sorted().toList();
                }
            }
        } catch (IOException | URISyntaxException e) {
            return List.of();
        }
        return List.of();
    }
}
