package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigLifecycleEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryBackupModule extends AbstractBaseConfigModule {
    private final Map<Long, BackupEntry> backups = new HashMap<>();
    private long backupCounter = 0;
    private int maxBackups = 10;
    private boolean autoBackupOnSave = false;

    public InMemoryBackupModule() {
        super("InMemoryBackupModule",
                "Creates and manages in-memory backups of configuration data",
                "1.0.0");
    }

    public InMemoryBackupModule(int maxBackups) {
        this();
        this.maxBackups = Math.max(1, maxBackups);
    }

    @Override
    protected void onInitialize() {
        // Nothing to initialize
    }

    @Override
    protected void onEnable() {
        if (autoBackupOnSave) {
            registerEventListener(
                    config.getEventBus(),
                    ConfigLifecycleEvent.class,
                    this::onConfigLifecycleEvent
            );
        }
    }

    @Override
    protected void onDisable() {
        if (autoBackupOnSave) {
            unregisterEventListener(
                    config.getEventBus(),
                    ConfigLifecycleEvent.class,
                    this::onConfigLifecycleEvent
            );
        }
    }

    @Override
    protected void onCleanup() {
        backups.clear();
        backupCounter = 0;
    }

    private void onConfigLifecycleEvent(ConfigLifecycleEvent event) {
        if (event.getType() == ConfigLifecycleEvent.Type.SAVE) {
            createBackup();
        }
    }

    public long createBackup() {
        if (config == null) return -1;

        // Create a snapshot of the current configuration
        Map<String, Object> snapshot = new HashMap<>(config.getRootSection().toSerializableMap());

        // Create backup entry
        backupCounter++;
        BackupEntry entry = new BackupEntry(backupCounter, Instant.now(), snapshot);
        backups.put(backupCounter, entry);

        // Enforce max backups limit
        cleanupOldBackups();

        return backupCounter;
    }

    public boolean restoreBackup(long id) {
        if (config == null || !backups.containsKey(id)) return false;

        BackupEntry entry = backups.get(id);
        Map<String, Object> state = entry.getData();

        // Create backup of current state before restoring
        createBackup();

        // Clear existing config
        config.clear();

        // Restore values from backup
        for (Map.Entry<String, Object> mapEntry : state.entrySet()) {
            config.set(mapEntry.getKey(), mapEntry.getValue());
        }

        return true;
    }

    public boolean deleteBackup(long id) {
        return backups.remove(id) != null;
    }

    public void clearBackups() {
        backups.clear();
    }

    public List<BackupEntry> getBackups() {
        return new ArrayList<>(backups.values());
    }

    public BackupEntry getBackup(long id) {
        return backups.get(id);
    }

    public int getBackupCount() {
        return backups.size();
    }

    public int getMaxBackups() {
        return maxBackups;
    }

    public void setMaxBackups(int maxBackups) {
        this.maxBackups = Math.max(1, maxBackups);
        cleanupOldBackups();
    }

    public void setAutoBackupOnSave(boolean autoBackupOnSave) {
        this.autoBackupOnSave = autoBackupOnSave;

        // Update event listeners if the module is enabled
        if (state == ModuleState.ENABLED) {
            if (autoBackupOnSave) {
                registerEventListener(
                        config.getEventBus(),
                        ConfigLifecycleEvent.class,
                        this::onConfigLifecycleEvent
                );
            } else {
                unregisterEventListener(
                        config.getEventBus(),
                        ConfigLifecycleEvent.class,
                        this::onConfigLifecycleEvent
                );
            }
        }
    }

    private void cleanupOldBackups() {
        if (backups.size() <= maxBackups) {
            return;
        }

        // Sort backups by ID (which is creation time order)
        List<Long> backupIds = new ArrayList<>(backups.keySet());
        backupIds.sort(Long::compareTo);

        // Remove oldest backups until we're at capacity
        int toRemove = backups.size() - maxBackups;
        for (int i = 0; i < toRemove; i++) {
            backups.remove(backupIds.get(i));
        }
    }

    public static class BackupEntry {
        private final long id;
        private final Instant timestamp;
        private final Map<String, Object> data;

        public BackupEntry(long id, Instant timestamp, Map<String, Object> data) {
            this.id = id;
            this.timestamp = timestamp;
            this.data = new HashMap<>(data);
        }

        public long getId() {
            return id;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public Map<String, Object> getData() {
            return new HashMap<>(data);
        }
    }

    @Override
    protected Map<String, Object> getAdditionalModuleState() {
        return Map.of();
    }
}
