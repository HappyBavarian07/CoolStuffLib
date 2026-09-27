package de.happybavarian07.coolstufflib.languagemanager.storage;

import de.happybavarian07.coolstufflib.utils.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Everything one plugin's language folder needs: the owner's backend, the jar defaults, migration and merging. */
public final class LanguageStorage {
    private static final String BACKEND_FILE = "_backend.txt";
    private static final Map<String, Function<File, LanguageBackend>> BACKENDS = new ConcurrentHashMap<>();
    private static volatile String defaultBackend = SplitYamlBackend.ID;

    static {
        BACKENDS.put(SplitYamlBackend.ID, SplitYamlBackend::new);
        BACKENDS.put(LegacyYamlBackend.ID, LegacyYamlBackend::new);
    }

    private final File folder;
    private final ClasspathLanguageSource jar;

    public LanguageStorage(File folder, String resourceDirectory) {
        this.folder = folder;
        this.jar = new ClasspathLanguageSource(FileUtils.class.getClassLoader(), resourceDirectory == null ? "languages" : resourceDirectory);
    }

    public static void registerBackend(String id, Function<File, LanguageBackend> factory) {
        BACKENDS.put(id, factory);
    }

    public static Set<String> backendIds() {
        return new TreeSet<>(BACKENDS.keySet());
    }

    public static void setDefaultBackend(String id) {
        if (!BACKENDS.containsKey(id)) throw new IllegalArgumentException("Unknown language backend: " + id);
        defaultBackend = id;
    }

    public static LanguageBackend createBackend(String id, File folder) {
        Function<File, LanguageBackend> factory = BACKENDS.get(id);
        if (factory == null) throw new IllegalArgumentException("Unknown language backend: " + id);
        return factory.apply(folder);
    }

    /** The backend this folder uses: the one saved by {@link #useBackend}, otherwise the default. */
    public static String backendIdFor(File folder) {
        File file = new File(folder, BACKEND_FILE);
        if (file.isFile()) {
            try {
                String id = Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
                if (BACKENDS.containsKey(id)) return id;
            } catch (IOException ignored) {
            }
        }
        return defaultBackend;
    }

    public static void useBackend(File folder, String id) {
        if (!BACKENDS.containsKey(id)) throw new IllegalArgumentException("Unknown language backend: " + id);
        try {
            Files.createDirectories(folder.toPath());
            Files.writeString(new File(folder, BACKEND_FILE).toPath(), id + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save the language backend choice", e);
        }
    }

    /** The owner's backend for this language; an unmigrated single file is still read in place. */
    public LanguageBackend diskFor(String language) {
        String id = backendIdFor(folder);
        if (SplitYamlBackend.ID.equals(id)) {
            boolean split = new File(folder, language).isDirectory();
            boolean legacy = new File(folder, language + ".yml").isFile();
            return !split && legacy ? new LegacyYamlBackend(folder) : new SplitYamlBackend(folder);
        }
        return createBackend(id, folder);
    }

    /** Jar defaults for the language, or for {@code updateLanguage} (then {@code en}) when the jar has no such language. */
    public ReadResult defaults(String language, @Nullable String updateLanguage) {
        ReadResult own = jar.read(language);
        if (!own.entries().isEmpty() || updateLanguage == null || updateLanguage.equals(language)) return own;
        ReadResult update = jar.read(updateLanguage);
        return update.entries().isEmpty() && !"en".equals(updateLanguage) ? jar.read("en") : update;
    }

    /** Moves an old single file into the split layout and adds keys that exist in the jar but not in the owner's files. */
    public PrepareResult prepare(String language, @Nullable String updateLanguage) {
        MigrationReport migration = SplitYamlBackend.ID.equals(backendIdFor(folder))
                ? LanguageMigration.migrateLegacyFile(folder, language) : null;
        LanguageBackend disk = diskFor(language);
        ReadResult current = disk.read(language);
        ReadResult defaults = defaults(language, updateLanguage);
        Set<String> failed = current.failedFiles();
        Set<String> ownKeys = current.entries().keySet();
        Set<String> ownPrefixes = prefixes(ownKeys);
        List<LanguageEntry> missing = new ArrayList<>();
        for (LanguageEntry entry : defaults.entries().values()) {
            if (ownKeys.contains(entry.key()) || conflicts(entry.key(), ownKeys, ownPrefixes)) continue;
            if (failed.contains(disk.fileOf(language, entry.key()))) continue;
            missing.add(entry);
        }
        List<LanguageProblem> problems = new ArrayList<>(current.problems());
        if (!missing.isEmpty() && disk.isWritable()) problems.addAll(disk.write(language, missing, defaults.sectionComments()));
        return new PrepareResult(language, migration, missing.stream().map(LanguageEntry::key).toList(), problems);
    }

    /** Merges the owner's files over the jar defaults. Keys of files that fail to load keep their values from {@code previous}. */
    public LoadResult load(String language, @Nullable String updateLanguage, @Nullable LoadResult previous) {
        LanguageBackend disk = diskFor(language);
        ReadResult current = disk.read(language);
        ReadResult defaults = defaults(language, updateLanguage);
        Map<String, LanguageEntry> own = new LinkedHashMap<>(current.entries());
        if (previous != null && !current.problems().isEmpty()) {
            Set<String> failed = current.failedFiles();
            for (LanguageEntry old : previous.own().values()) {
                if (!own.containsKey(old.key()) && failed.contains(disk.fileOf(language, old.key()))) own.put(old.key(), old);
            }
        }
        Set<String> ownPrefixes = prefixes(own.keySet());
        Map<String, LanguageEntry> merged = new LinkedHashMap<>();
        for (LanguageEntry entry : defaults.entries().values()) {
            if (!conflicts(entry.key(), own.keySet(), ownPrefixes)) merged.put(entry.key(), entry);
        }
        merged.putAll(own);
        List<LanguageProblem> problems = new ArrayList<>(current.problems());
        problems.addAll(defaults.problems());
        return new LoadResult(language, disk.id(), Collections.unmodifiableMap(merged), Collections.unmodifiableMap(own),
                defaults.entries(), List.copyOf(problems));
    }

    public List<LanguageProblem> write(String language, Collection<LanguageEntry> entries) {
        return diskFor(language).write(language, entries);
    }

    public File locationOf(String language) {
        File dir = new File(folder, language);
        return dir.isDirectory() ? dir : new File(folder, language + ".yml");
    }

    public File getFolder() {
        return folder;
    }

    /** A jar key conflicts when the owner has a value above it or a section at it. */
    private static boolean conflicts(String key, Set<String> ownKeys, Set<String> ownPrefixes) {
        if (ownPrefixes.contains(key)) return true;
        int dot = key.indexOf('.');
        while (dot > 0) {
            if (ownKeys.contains(key.substring(0, dot))) return true;
            dot = key.indexOf('.', dot + 1);
        }
        return false;
    }

    private static Set<String> prefixes(Set<String> keys) {
        Set<String> prefixes = new HashSet<>();
        for (String key : keys) {
            int dot = key.indexOf('.');
            while (dot > 0) {
                prefixes.add(key.substring(0, dot));
                dot = key.indexOf('.', dot + 1);
            }
        }
        return prefixes;
    }
}
