package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import java.util.HashMap;
import java.util.Map;

public class InMemoryBackupModule extends ConfigModule {
    private final Map<Long, Map<String, Object>> backups = new HashMap<>();
    private long backupCounter = 0;

    @Override
    public String getName() { return "InMemoryBackupModule"; }
    @Override
    public void enable() { /* Do nothing */ }
    @Override
    public void disable() { /* Do nothing */ }
    @Override
    public void onAttach(AdvancedConfig config) { super.onAttach(config); }
    @Override
    public void onDetach() { super.onDetach(); }
    @Override
    public void reload() {}
    @Override
    public void save() {}
    @Override
    public void onConfigChange(String key, Object oldValue, Object newValue) {}
    @Override
    public boolean supportsConfig(AdvancedConfig config) { return true; }
    @Override
    public Object onGetValue(String key, Object value) { return value; }

    public long createBackup() {
        if (getConfig() == null) return -1;
        Map<String, Object> snapshot = new HashMap<>();
        for (Map.Entry<String, ?> entry : getConfig().getValueMap().entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue());
        }
        backupCounter++;
        backups.put(backupCounter, new HashMap<>(snapshot));
        return backupCounter;
    }

    public boolean restoreBackup(long id) {
        if (getConfig() == null || !backups.containsKey(id)) return false;
        Map<String, Object> state = backups.get(id);
        for (Map.Entry<String, Object> entry : state.entrySet()) {
            getConfig().setValue(entry.getKey(), entry.getValue());
        }
        return true;
    }

    @Override
    public Map<String, Object> getModuleState() {
        return Map.of("backupCount", backups.size());
    }
}
