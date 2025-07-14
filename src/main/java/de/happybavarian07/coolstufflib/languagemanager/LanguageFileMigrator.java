package de.happybavarian07.coolstufflib.languagemanager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class LanguageFileMigrator {
    private final File userConfigFile;
    private final InputStream resourceStream;
    private final FileConfiguration userConfig;
    private final FileConfiguration resourceConfig;
    private final List<MigrationEntry> migrationEntries;

    public LanguageFileMigrator(File userConfigFile, InputStream resourceStream) {
        this.userConfigFile = userConfigFile;
        this.resourceStream = resourceStream;
        this.userConfig = YamlConfiguration.loadConfiguration(userConfigFile);
        this.resourceConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(resourceStream));
        this.migrationEntries = new ArrayList<>();
        scanForMigrations();
    }

    public boolean filesDifferByHash() {
        String userHash = getFileHash(userConfigFile);
        String resourceHash = getStreamHash(resourceStream);
        return !userHash.equals(resourceHash);
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
        Map<String, Object> resourceMap = flattenConfig(resourceConfig, "");
        Map<String, Object> userMap = flattenConfig(userConfig, "");
        for (String key : resourceMap.keySet()) {
            Object resourceValue = resourceMap.get(key);
            Object userValue = userMap.get(key);
            if (!userMap.containsKey(key)) {
                migrationEntries.add(new MigrationEntry(key, null, resourceValue, MigrationStatus.MISSING_IN_USER));
            } else if (userValue != null && !userValue.equals(resourceValue)) {
                migrationEntries.add(new MigrationEntry(key, userValue, resourceValue, MigrationStatus.DIFFERENT_VALUE));
            }
        }
        for (String key : userMap.keySet()) {
            if (!resourceMap.containsKey(key)) {
                migrationEntries.add(new MigrationEntry(key, userMap.get(key), null, MigrationStatus.UNCHANGED));
            }
        }
    }

    private Map<String, Object> flattenConfig(FileConfiguration config, String prefix) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : config.getKeys(true)) {
            Object value = config.get(key);
            if (value != null && !(value instanceof ConfigurationSection)) {
                map.put(prefix.isEmpty() ? key : prefix + "." + key, value);
            }
        }
        return map;
    }

    public List<MigrationEntry> getMigrationEntries() {
        return new ArrayList<>(migrationEntries);
    }

    public void editEntry(MigrationEntry entry, Object newValue) {
        entry.setUserValue(newValue);
        entry.setSelectedForMigration(true);
    }

    public void migrateSelected() {
        for (MigrationEntry entry : migrationEntries) {
            if (entry.isSelectedForMigration() && entry.getResourceValue() != null) {
                userConfig.set(entry.getKey(), entry.getUserValue() != null ? entry.getUserValue() : entry.getResourceValue());
            }
        }
        try {
            userConfig.save(userConfigFile);
        } catch (Exception ignored) {}
    }

    public static class MigrationEntry {
        private final String key;
        private Object userValue;
        private final Object resourceValue;
        private final MigrationStatus status;
        private boolean selectedForMigration;

        public MigrationEntry(String key, Object userValue, Object resourceValue, MigrationStatus status) {
            this.key = key;
            this.userValue = userValue;
            this.resourceValue = resourceValue;
            this.status = status;
            this.selectedForMigration = status == MigrationStatus.MISSING_IN_USER || status == MigrationStatus.DIFFERENT_VALUE;
        }

        public String getKey() { return key; }
        public Object getUserValue() { return userValue; }
        public Object getResourceValue() { return resourceValue; }
        public MigrationStatus getStatus() { return status; }
        public boolean isSelectedForMigration() { return selectedForMigration; }
        public void setSelectedForMigration(boolean selected) { this.selectedForMigration = selected; }
        public void setUserValue(Object value) { this.userValue = value; }
    }

    public enum MigrationStatus {
        MISSING_IN_USER,
        DIFFERENT_VALUE,
        NEW_IN_RESOURCE,
        UNCHANGED
    }
}
