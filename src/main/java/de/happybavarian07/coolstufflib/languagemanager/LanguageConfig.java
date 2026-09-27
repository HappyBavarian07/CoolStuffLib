package de.happybavarian07.coolstufflib.languagemanager;

import de.happybavarian07.coolstufflib.languagemanager.storage.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * <p>One language's values. {@link #getConfig()} is built in memory from the owner's files over the jar defaults
 * and replaced as a whole on every reload, so readers never see half a reload.</p>
 */
public class LanguageConfig {
    private final String langName;
    private final LanguageStorage storage;
    private volatile String updateLanguage = "en";
    private volatile LoadResult loaded;
    private volatile FileConfiguration config;
    private volatile PrepareResult pendingPrepare;
    private List<LanguageProblem> reportedLoadProblems = List.of();

    public LanguageConfig(File langFile, File langFolder, String resourceDirectory, String langName) {
        this.langName = langName;
        this.storage = new LanguageStorage(langFolder, resourceDirectory);
        saveDefaultConfig();
        reloadConfig();
    }

    public void reloadConfig() {
        LoadResult result = storage.load(langName, updateLanguage, loaded);
        YamlConfiguration next = new YamlConfiguration();
        for (LanguageEntry entry : result.merged().values()) next.set(entry.key(), entry.value());
        loaded = result;
        config = next;
    }

    public FileConfiguration getConfig() {
        if (config == null) reloadConfig();
        return config;
    }

    /** Writes values that were changed through {@link #getConfig()} to the owner's files. */
    public void saveConfig() {
        FileConfiguration current = config;
        LoadResult base = loaded;
        if (current == null || base == null) return;
        List<LanguageEntry> changed = new ArrayList<>();
        for (String key : current.getKeys(true)) {
            if (current.isConfigurationSection(key)) continue;
            Object value = current.get(key);
            LanguageEntry known = base.merged().get(key);
            if (known == null || !Objects.equals(known.value(), value)) {
                changed.add(new LanguageEntry(key, value, known == null ? null : known.comment(), null));
            }
        }
        if (!changed.isEmpty()) write(changed);
    }

    /** Migrates an old single file and adds keys that are in the jar but not in the owner's files. */
    public void saveDefaultConfig() {
        pendingPrepare = storage.prepare(langName, updateLanguage);
    }

    public void update(String updateLanguage) {
        this.updateLanguage = updateLanguage == null || updateLanguage.isBlank() ? "en" : updateLanguage;
        saveDefaultConfig();
        reloadConfig();
    }

    public List<LanguageProblem> write(Collection<LanguageEntry> entries) {
        List<LanguageProblem> problems = storage.write(langName, entries);
        reloadConfig();
        return problems;
    }

    public String getLangName() {
        return langName;
    }

    /** The language's folder in the split layout, or its single file. */
    public File getFile() {
        return storage.locationOf(langName);
    }

    public LoadResult getLoaded() {
        if (loaded == null) reloadConfig();
        return loaded;
    }

    public LanguageStorage getStorage() {
        return storage;
    }

    public String originOf(String key) {
        LoadResult result = loaded;
        LanguageEntry entry = result == null ? null : result.merged().get(key);
        return entry == null || entry.origin() == null ? "?" : entry.origin().toString();
    }

    /** Report lines since the last call: migration, added keys and problems (each problem once). */
    public synchronized List<String> drainReport() {
        Set<String> lines = new LinkedHashSet<>();
        PrepareResult prepare = pendingPrepare;
        pendingPrepare = null;
        if (prepare != null) {
            MigrationReport migration = prepare.migration();
            if (migration != null && migration.ok()) {
                lines.add("Moved " + langName + ".yml into the folder " + langName + "/ (" + migration.entries()
                        + " keys, backup: " + migration.backup() + ")");
            } else if (migration != null) {
                lines.add("Could not move " + langName + ".yml into the new layout, still using the old file: " + migration.summary());
            }
            if (!prepare.addedKeys().isEmpty()) {
                List<String> added = prepare.addedKeys();
                lines.add("Added " + added.size() + " new keys to " + langName + ": "
                        + String.join(", ", added.subList(0, Math.min(10, added.size()))) + (added.size() > 10 ? ", ..." : ""));
            }
            prepare.problems().forEach(problem -> lines.add("Problem in " + problem));
        }
        LoadResult result = loaded;
        if (result != null && !result.problems().equals(reportedLoadProblems)) {
            result.problems().forEach(problem -> lines.add("Problem in " + problem));
            reportedLoadProblems = result.problems();
        }
        return new ArrayList<>(lines);
    }
}
