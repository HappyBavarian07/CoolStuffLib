package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigValueEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PersistentHistoryModule extends AbstractBaseConfigModule {
    private final List<HistoryEntry> history = new ArrayList<>();
    private final File historyFile;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PersistentHistoryModule(File historyFile) {
        super("PersistentHistoryModule",
              "Records configuration changes to a persistent history file",
              "1.0.0");
        this.historyFile = historyFile;
    }

    @Override
    protected void onInitialize() {
        loadHistory();
    }

    @Override
    protected void onEnable() {
        registerEventListener(
            config.getEventBus(),
            ConfigValueEvent.class,
            this::onValueChangeEvent
        );
    }

    @Override
    protected void onDisable() {
        unregisterEventListener(
            config.getEventBus(),
            ConfigValueEvent.class,
            this::onValueChangeEvent
        );
        saveHistory();
    }

    @Override
    protected void onCleanup() {
        history.clear();
    }

    private void onValueChangeEvent(ConfigValueEvent event) {
        if (event.getType() == ConfigValueEvent.Type.SET) {
            addHistoryEntry(
                event.getFullPath(),
                event.getOldValue(),
                event.getNewValue(),
                ChangeType.MODIFY
            );
        } else if (event.getType() == ConfigValueEvent.Type.REMOVE) {
            addHistoryEntry(
                event.getFullPath(),
                event.getOldValue(),
                null,
                ChangeType.DELETE
            );
        }
    }

    public void addHistoryEntry(String key, Object oldValue, Object newValue, ChangeType type) {
        HistoryEntry entry = new HistoryEntry(
            LocalDateTime.now(),
            key,
            oldValue,
            newValue,
            type
        );
        history.add(entry);

        // Save periodically to prevent data loss
        if (history.size() % 10 == 0) {
            saveHistory();
        }
    }

    public List<HistoryEntry> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public List<HistoryEntry> getHistoryForKey(String key) {
        List<HistoryEntry> result = new ArrayList<>();
        for (HistoryEntry entry : history) {
            if (entry.key.equals(key)) {
                result.add(entry);
            }
        }
        return result;
    }

    private void loadHistory() {
        history.clear();
        if (historyFile == null || !historyFile.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(historyFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = line.split("\\|", 5);
                    if (parts.length != 5) continue;

                    LocalDateTime timestamp = LocalDateTime.parse(parts[0], formatter);
                    String key = parts[1];
                    String oldValueStr = parts[2].equals("null") ? null : parts[2];
                    String newValueStr = parts[3].equals("null") ? null : parts[3];
                    ChangeType type = ChangeType.valueOf(parts[4]);

                    HistoryEntry entry = new HistoryEntry(
                        timestamp,
                        key,
                        oldValueStr,
                        newValueStr,
                        type
                    );
                    history.add(entry);
                } catch (Exception e) {
                    // Skip invalid entries
                }
            }
        } catch (IOException e) {
            // Handle error
        }
    }

    private void saveHistory() {
        if (historyFile == null) {
            return;
        }

        try {
            historyFile.getParentFile().mkdirs();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(historyFile))) {
                for (HistoryEntry entry : history) {
                    String line = String.format("%s|%s|%s|%s|%s",
                        entry.timestamp.format(formatter),
                        entry.key,
                        entry.oldValue == null ? "null" : entry.oldValue,
                        entry.newValue == null ? "null" : entry.newValue,
                        entry.type.name()
                    );
                    writer.write(line);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            // Handle error
        }
    }

    public static class HistoryEntry {
        private final LocalDateTime timestamp;
        private final String key;
        private final Object oldValue;
        private final Object newValue;
        private final ChangeType type;

        public HistoryEntry(LocalDateTime timestamp, String key, Object oldValue, Object newValue, ChangeType type) {
            this.timestamp = timestamp;
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.type = type;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getKey() {
            return key;
        }

        public Object getOldValue() {
            return oldValue;
        }

        public Object getNewValue() {
            return newValue;
        }

        public ChangeType getType() {
            return type;
        }
    }

    public enum ChangeType {
        CREATE,
        MODIFY,
        DELETE
    }

    public Map<String, Object> getAdditionalModuleState() {
        Map<String, Object> state = new HashMap<>();
        state.put("historySize", history.size());
        state.put("historyFile", historyFile != null ? historyFile.getAbsolutePath() : null);
        return state;
    }
}
