package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

public class PersistentHistoryModule extends ConfigModule {
    private final List<HistoryEntry> history = new ArrayList<>();
    private final File historyFile;

    public PersistentHistoryModule(File historyFile) {
        this.historyFile = historyFile;
        loadHistory();
    }

    @Override
    public String getName() {
        return "PersistentHistoryModule";
    }

    @Override
    public void enable() {

    }

    @Override
    public void disable() {

    }

    @Override
    public void reload() {
        loadHistory();
    }

    @Override
    public void save() {
        saveHistory();
    }

    @Override
    public Object onGetValue(String key, Object value) {
        return value;
    }

    @Override
    public Map<String, Object> getModuleState() {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (HistoryEntry entry : history) {
            entries.add(entry.toMap());
        }
        return Map.of("history", entries);
    }

    @Override
    public void onConfigChange(String key, Object oldValue, Object newValue) {
        if (!isEnabled()) return;
        history.add(new HistoryEntry(LocalDateTime.now(), key, oldValue, newValue));
        saveHistory();
    }

    @Override
    public boolean supportsConfig(AdvancedConfig config) {
        return true;
    }

    public List<HistoryEntry> getHistory() {
        return Collections.unmodifiableList(history);
    }

    private void loadHistory() {
        history.clear();
        if (historyFile == null || !historyFile.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(historyFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                HistoryEntry entry = HistoryEntry.fromString(line);
                if (entry != null) history.add(entry);
            }
        } catch (IOException ignored) {}
    }

    private void saveHistory() {
        if (historyFile == null) return;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(historyFile))) {
            for (HistoryEntry entry : history) {
                writer.write(entry.toString());
                writer.newLine();
            }
        } catch (IOException ignored) {}
    }

    public record HistoryEntry(LocalDateTime timestamp, String key, Object oldValue, Object newValue) {

        public Map<String, Object> toMap() {
                return Map.of(
                        "timestamp", timestamp.toString(),
                        "key", key,
                        "oldValue", oldValue == null ? "null" : oldValue.toString(),
                        "newValue", newValue == null ? "null" : newValue.toString()
                );
            }

            @Override
            public @NotNull String toString() {
                return timestamp + "," + key + "," + (oldValue == null ? "null" : oldValue) + "," + (newValue == null ? "null" : newValue);
            }

            public static HistoryEntry fromString(String str) {
                try {
                    String[] parts = str.split(",", 4);
                    return new HistoryEntry(LocalDateTime.parse(parts[0]), parts[1],
                            "null".equals(parts[2]) ? null : parts[2],
                            "null".equals(parts[3]) ? null : parts[3]);
                } catch (Exception e) {
                    return null;
                }
            }
        }
}
