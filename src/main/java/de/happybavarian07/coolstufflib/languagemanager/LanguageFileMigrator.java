package de.happybavarian07.coolstufflib.languagemanager;

import de.happybavarian07.coolstufflib.languagemanager.storage.LanguageEntry;
import de.happybavarian07.coolstufflib.languagemanager.storage.SplitYamlBackend;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * <p>Handles migration of language files to synchronize with resource files.</p>
 */
public class LanguageFileMigrator {
    private final File userConfigFile;
    private final InputStream resourceStream;
    private final Map<String, Object> userValues;
    private final Map<String, Object> resourceValues;
    private final Consumer<Map<String, Object>> writer;
    private final List<MigrationEntry> migrationEntries = new ArrayList<>();

    /**
     * <p>Creates a new migrator instance.</p>
     *
     * @param userConfigFile The user configuration file
     * @param resourceStream The resource input stream
     */
    public LanguageFileMigrator(File userConfigFile, InputStream resourceStream) {
        FileConfiguration jarConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(resourceStream));
        this.resourceValues = flattenConfig(jarConfig);
        if (userConfigFile.isDirectory()) {
            SplitYamlBackend backend = new SplitYamlBackend(userConfigFile.getParentFile());
            String language = userConfigFile.getName();
            this.userConfigFile = null;
            this.resourceStream = null;
            Map<String, Object> owned = new LinkedHashMap<>();
            backend.read(language).entries().forEach((key, entry) -> owned.put(key, entry.value()));
            this.userValues = owned;
            this.writer = changes -> backend.write(language, changes.entrySet().stream()
                    .map(change -> new LanguageEntry(change.getKey(), change.getValue(), commentOf(jarConfig, change.getKey()), null))
                    .toList());
        } else {
            FileConfiguration userConfig = YamlConfiguration.loadConfiguration(userConfigFile);
            this.userConfigFile = userConfigFile;
            this.resourceStream = resourceStream;
            this.userValues = flattenConfig(userConfig);
            this.writer = changes -> {
                changes.forEach(userConfig::set);
                try {
                    userConfig.save(userConfigFile);
                } catch (Exception ignored) {
                }
            };
        }
        scanForMigrations();
    }

    private static String commentOf(FileConfiguration jarConfig, String key) {
        List<String> lines = jarConfig.getComments(key);
        if (lines.isEmpty()) return null;
        List<String> text = new ArrayList<>();
        for (String line : lines) {
            if (line != null) text.add(line.startsWith(" ") ? line.substring(1) : line);
        }
        return text.isEmpty() ? null : String.join("\n", text);
    }

    /** Compares the owner's values with the jar's; {@code writer} receives the selected keys and values. */
    public LanguageFileMigrator(Map<String, Object> userValues, Map<String, Object> resourceValues, Consumer<Map<String, Object>> writer) {
        this.userConfigFile = null;
        this.resourceStream = null;
        this.userValues = new LinkedHashMap<>(userValues);
        this.resourceValues = new LinkedHashMap<>(resourceValues);
        this.writer = writer;
        scanForMigrations();
    }

    /**
     * <p>Checks if the files differ by hash.</p>
     *
     * @return {@code true} if files differ
     */
    public boolean filesDifferByHash() {
        if (userConfigFile == null || resourceStream == null) return !userValues.equals(resourceValues);
        return !getFileHash(userConfigFile).equals(getStreamHash(resourceStream));
    }

    private String getFileHash(File file) {
        try (InputStream fis = new FileInputStream(file)) {
            return getHash(fis);
        } catch (Exception e) {
            return "";
        }
    }

    private String getStreamHash(InputStream stream) {
        try {
            return getHash(stream);
        } catch (Exception e) {
            return "";
        }
    }

    @NotNull
    private String getHash(InputStream stream) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[4096];
        int n;
        while ((n = stream.read(buffer)) > 0) {
            digest.update(buffer, 0, n);
        }
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void scanForMigrations() {
        migrationEntries.clear();
        for (Map.Entry<String, Object> resource : resourceValues.entrySet()) {
            String key = resource.getKey();
            Object userValue = userValues.get(key);
            if (!userValues.containsKey(key)) {
                migrationEntries.add(new MigrationEntry(key, null, resource.getValue(), MigrationStatus.MISSING_IN_USER));
            } else if (userValue != null && !userValue.equals(resource.getValue())) {
                migrationEntries.add(new MigrationEntry(key, userValue, resource.getValue(), MigrationStatus.DIFFERENT_VALUE));
            }
        }
        for (Map.Entry<String, Object> user : userValues.entrySet()) {
            String key = user.getKey();
            if (!resourceValues.containsKey(key)) {
                migrationEntries.add(new MigrationEntry(key, user.getValue(), null, MigrationStatus.MISSING_IN_RESOURCE));
            } else if (resourceValues.get(key).equals(user.getValue())) {
                migrationEntries.add(new MigrationEntry(key, user.getValue(), resourceValues.get(key), MigrationStatus.UNCHANGED));
            }
        }
    }

    private static Map<String, Object> flattenConfig(FileConfiguration config) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : config.getKeys(true)) {
            Object value = config.get(key);
            if (value != null && !(value instanceof ConfigurationSection)) map.put(key, value);
        }
        return map;
    }

    /**
     * <p>Returns the list of migration entries.</p>
     *
     * @return The migration entries
     */
    public List<MigrationEntry> getMigrationEntries() {
        return new ArrayList<>(migrationEntries);
    }

    /**
     * <p>Edits a migration entry.</p>
     *
     * @param entry    The migration entry
     * @param newValue The new value
     */
    public void editEntry(MigrationEntry entry, Object newValue) {
        entry.setUserValue(newValue);
        entry.setSelectedForMigration(true);
    }

    /**
     * <p>Applies the selected migrations to the user configuration.</p>
     */
    public void migrateSelected() {
        Map<String, Object> changes = new LinkedHashMap<>();
        for (MigrationEntry entry : migrationEntries) {
            if (entry.isSelectedForMigration() && entry.getResourceValue() != null) {
                changes.put(entry.getKey(), entry.getUserValue() != null ? entry.getUserValue() : entry.getResourceValue());
            }
        }
        if (!changes.isEmpty()) writer.accept(changes);
    }

    public enum MigrationStatus {
        MISSING_IN_USER,
        MISSING_IN_RESOURCE,
        DIFFERENT_VALUE,
        UNCHANGED
    }

    public static class MigrationEntry {
        private final String key;
        private final Object resourceValue;
        private final MigrationStatus status;
        private Object userValue;
        private boolean selectedForMigration;

        public MigrationEntry(String key, Object userValue, Object resourceValue, MigrationStatus status) {
            this.key = key;
            this.userValue = userValue;
            this.resourceValue = resourceValue;
            this.status = status;
            this.selectedForMigration = status == MigrationStatus.MISSING_IN_USER;
        }

        public String getKey() {
            return key;
        }

        public Object getUserValue() {
            return userValue;
        }

        public void setUserValue(Object value) {
            this.userValue = value;
        }

        public Object getResourceValue() {
            return resourceValue;
        }

        public MigrationStatus getStatus() {
            return status;
        }

        public boolean isSelectedForMigration() {
            return selectedForMigration;
        }

        public void setSelectedForMigration(boolean selected) {
            this.selectedForMigration = selected;
        }

        @Override
        public String toString() {
            return "MigrationEntry{" + "key='" + key + '\'' +
                    ", userValue=" + userValue +
                    ", resourceValue=" + resourceValue +
                    ", status=" + status +
                    ", selectedForMigration=" + selectedForMigration +
                    '}';
        }

        public static MigrationEntry fromString(String str) {
            String[] parts = str.split(",");
            String key = parts[0].split("=")[1].trim();
            Object userValue = parts[1].split("=")[1].trim();
            Object resourceValue = parts[2].split("=")[1].trim();
            MigrationStatus status = MigrationStatus.valueOf(parts[3].split("=")[1].trim());
            boolean selectedForMigration = Boolean.parseBoolean(parts[4].split("=")[1].trim());
            MigrationEntry entry = new MigrationEntry(key, userValue, resourceValue, status);
            entry.setSelectedForMigration(selectedForMigration);
            return entry;
        }
    }
}
